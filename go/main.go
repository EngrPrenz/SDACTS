package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"sync"
	"time"

	_ "github.com/alexbrainman/odbc"
	"golang.org/x/crypto/bcrypt"
)

// ─── Data Models ────────────────────────────────────────────────────────────

type Product struct {
	ID       int     `json:"id"`
	Name     string  `json:"name"`
	Price    float64 `json:"price"`
	Quantity int     `json:"quantity"`
}

// ─── Simple In-Memory Session Store ─────────────────────────────────────────

type session struct {
	username  string
	expiresAt time.Time
}

var (
	sessions   = map[string]session{}
	sessionsMu sync.RWMutex
)

const sessionCookieName = "pms_session"
const sessionDuration = 8 * time.Hour

func newSessionToken() string {
	return fmt.Sprintf("%d", time.Now().UnixNano())
}

func createSession(username string) string {
	token := newSessionToken()
	sessionsMu.Lock()
	sessions[token] = session{username: username, expiresAt: time.Now().Add(sessionDuration)}
	sessionsMu.Unlock()
	return token
}

func getSession(r *http.Request) (string, bool) {
	cookie, err := r.Cookie(sessionCookieName)
	if err != nil {
		return "", false
	}
	sessionsMu.RLock()
	s, ok := sessions[cookie.Value]
	sessionsMu.RUnlock()
	if !ok || time.Now().After(s.expiresAt) {
		return "", false
	}
	return s.username, true
}

func deleteSession(token string) {
	sessionsMu.Lock()
	delete(sessions, token)
	sessionsMu.Unlock()
}

// ─── Auth Middleware ─────────────────────────────────────────────────────────

func requireLogin(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if _, ok := getSession(r); !ok {
			http.Redirect(w, r, "/login", http.StatusSeeOther)
			return
		}
		next(w, r)
	}
}

func requireLoginAPI(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Content-Type", "application/json")
		if _, ok := getSession(r); !ok {
			w.WriteHeader(http.StatusUnauthorized)
			json.NewEncoder(w).Encode(map[string]interface{}{
				"success": false,
				"error":   "Not authenticated",
			})
			return
		}
		next(w, r)
	}
}

// ─── Database ────────────────────────────────────────────────────────────────

var db *sql.DB

func main() {
	var err error
	connString := "Driver={SQL Server};Server=XEVO\\SQLEXPRESS01;Database=ProductDB;Trusted_Connection=Yes;"

	fmt.Println("Connecting to SQL Server via ODBC...")
	db, err = sql.Open("odbc", connString)
	if err != nil {
		log.Fatal("Error creating connection: ", err)
	}
	defer db.Close()

	if err = db.Ping(); err != nil {
		log.Fatal("Error connecting to database: ", err)
	}
	fmt.Println("✔ Successfully connected to SQL Server!")

	createTables()

	// ── Static files ──
	http.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("static"))))

	// ── Public routes ──
	http.HandleFunc("/login", handleLoginPage)
	http.HandleFunc("/register", handleRegisterPage)
	http.HandleFunc("/api/login", handleLoginAPI)
	http.HandleFunc("/api/register", handleRegisterAPI)
	http.HandleFunc("/api/logout", handleLogout)

	// ── Protected routes ──
	http.HandleFunc("/", requireLogin(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "static/index.html")
	}))
	http.HandleFunc("/api/products", requireLoginAPI(handleProducts))
	http.HandleFunc("/api/products/add", requireLoginAPI(addProduct))
	http.HandleFunc("/api/products/update", requireLoginAPI(updateProduct))
	http.HandleFunc("/api/products/delete", requireLoginAPI(deleteProduct))
	http.HandleFunc("/api/products/search", requireLoginAPI(searchProducts))

	fmt.Println("🌐 Web server starting at http://localhost:8080")
	fmt.Println("Open your browser and go to: http://localhost:8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func createTables() {
	// Products table
	_, err := db.Exec(`
		IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='products' AND xtype='U')
		CREATE TABLE products (
			id       INT PRIMARY KEY IDENTITY(1,1),
			name     VARCHAR(100) NOT NULL,
			price    DECIMAL(10,2),
			quantity INT
		)`)
	if err != nil {
		log.Fatal("Error creating products table: ", err)
	}

	// Users table
	_, err = db.Exec(`
		IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
		CREATE TABLE users (
			id         INT PRIMARY KEY IDENTITY(1,1),
			username   VARCHAR(50)  NOT NULL UNIQUE,
			password   VARCHAR(255) NOT NULL,
			created_at DATETIME DEFAULT GETDATE()
		)`)
	if err != nil {
		log.Fatal("Error creating users table: ", err)
	}
}

// ─── Login Handlers ──────────────────────────────────────────────────────────

func handleLoginPage(w http.ResponseWriter, r *http.Request) {
	// If already logged in, redirect to dashboard
	if _, ok := getSession(r); ok {
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}
	http.ServeFile(w, r, "static/login.html")
}

func handleLoginAPI(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Method not allowed"})
		return
	}

	var creds struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&creds); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Invalid request"})
		return
	}

	// Fetch hashed password from DB
	var hash string
	err := db.QueryRow("SELECT password FROM users WHERE username = ?", creds.Username).Scan(&hash)
	if err != nil {
		// Generic message — don't reveal whether user exists
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Invalid username or password"})
		return
	}

	// Compare hash
	if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(creds.Password)); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Invalid username or password"})
		return
	}

	// Create session cookie
	token := createSession(creds.Username)
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookieName,
		Value:    token,
		Path:     "/",
		HttpOnly: true,
		MaxAge:   int(sessionDuration.Seconds()),
	})

	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

func handleRegisterPage(w http.ResponseWriter, r *http.Request) {
	if _, ok := getSession(r); ok {
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}
	http.ServeFile(w, r, "static/register.html")
}

func handleRegisterAPI(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Method not allowed"})
		return
	}

	var creds struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&creds); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Invalid request"})
		return
	}

	// Basic validation
	if len(creds.Username) < 3 {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Username must be at least 3 characters"})
		return
	}
	if len(creds.Password) < 4 {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Password must be at least 4 characters"})
		return
	}

	// Check if username already taken
	var exists int
	err := db.QueryRow("SELECT COUNT(*) FROM users WHERE username = ?", creds.Username).Scan(&exists)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Database error"})
		return
	}
	if exists > 0 {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Username already taken"})
		return
	}

	// Hash the password
	hash, err := bcrypt.GenerateFromPassword([]byte(creds.Password), bcrypt.DefaultCost)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Error securing password"})
		return
	}

	// Insert new user
	_, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", creds.Username, string(hash))
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Error creating account"})
		return
	}

	fmt.Printf("New user registered: %s\n", creds.Username)
	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

func handleLogout(w http.ResponseWriter, r *http.Request) {
	cookie, err := r.Cookie(sessionCookieName)
	if err == nil {
		deleteSession(cookie.Value)
	}
	http.SetCookie(w, &http.Cookie{
		Name:   sessionCookieName,
		Value:  "",
		Path:   "/",
		MaxAge: -1,
	})
	http.Redirect(w, r, "/login", http.StatusSeeOther)
}

// ─── Product Handlers ────────────────────────────────────────────────────────

func handleProducts(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query("SELECT id, name, price, quantity FROM products ORDER BY id")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Price, &p.Quantity); err != nil {
			continue
		}
		products = append(products, p)
	}
	json.NewEncoder(w).Encode(products)
}

func addProduct(w http.ResponseWriter, r *http.Request) {
	var p Product
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": err.Error()})
		return
	}

	_, err := db.Exec("INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)",
		p.Name, p.Price, p.Quantity)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": err.Error()})
		return
	}
	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

func updateProduct(w http.ResponseWriter, r *http.Request) {
	var p Product
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": err.Error()})
		return
	}

	_, err := db.Exec("UPDATE products SET name = ?, price = ?, quantity = ? WHERE id = ?",
		p.Name, p.Price, p.Quantity, p.ID)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": err.Error()})
		return
	}
	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

func deleteProduct(w http.ResponseWriter, r *http.Request) {
	var data struct {
		ID int `json:"id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": err.Error()})
		return
	}

	_, err := db.Exec("DELETE FROM products WHERE id = ?", data.ID)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": err.Error()})
		return
	}
	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

func searchProducts(w http.ResponseWriter, r *http.Request) {
	searchTerm := r.URL.Query().Get("q")
	id, err := strconv.Atoi(searchTerm)

	var rows *sql.Rows
	if err == nil {
		rows, err = db.Query("SELECT id, name, price, quantity FROM products WHERE id = ?", id)
	} else {
		rows, err = db.Query("SELECT id, name, price, quantity FROM products WHERE name LIKE ?", "%"+searchTerm+"%")
	}
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Price, &p.Quantity); err != nil {
			continue
		}
		products = append(products, p)
	}
	json.NewEncoder(w).Encode(products)
}
