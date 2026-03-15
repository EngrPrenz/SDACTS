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
	ID    int     `json:"id"`
	Name  string  `json:"name"`
	Price float64 `json:"price"`
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
	connString := "Driver={SQL Server};Server=ACER-NITROV15-F\\SQLEXPRESS;Database=SampleDB;Trusted_Connection=Yes;"

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

// ─── Login Handlers ──────────────────────────────────────────────────────────

func handleLoginPage(w http.ResponseWriter, r *http.Request) {
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

	fmt.Printf("[DEBUG] Login attempt — username: %q, password: %q\n", creds.Username, creds.Password)

	var hash string
	query := fmt.Sprintf("SELECT Password FROM Users WHERE Username = '%s'", creds.Username)
	err := db.QueryRow(query).Scan(&hash)
	if err != nil {
		fmt.Printf("[DEBUG] DB query error: %v\n", err)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Invalid username or password"})
		return
	}

	fmt.Printf("[DEBUG] Password from DB: %q\n", hash)
	fmt.Printf("[DEBUG] Password from login form: %q\n", creds.Password)
	fmt.Printf("[DEBUG] Plain-text match: %v\n", hash == creds.Password)

	bcryptErr := bcrypt.CompareHashAndPassword([]byte(hash), []byte(creds.Password))
	fmt.Printf("[DEBUG] Bcrypt match error: %v\n", bcryptErr)

	if bcryptErr != nil && hash != creds.Password {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Invalid username or password"})
		return
	}

	token := createSession(creds.Username)
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookieName,
		Value:    token,
		Path:     "/",
		HttpOnly: true,
		MaxAge:   int(sessionDuration.Seconds()),
	})

	fmt.Printf("[DEBUG] Login success for %q\n", creds.Username)
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

	if len(creds.Username) < 3 {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Username must be at least 3 characters"})
		return
	}
	if len(creds.Password) < 4 {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Password must be at least 4 characters"})
		return
	}

	// Check for duplicate username
	var exists int
	err := db.QueryRow(fmt.Sprintf("SELECT COUNT(*) FROM Users WHERE Username = '%s'", creds.Username)).Scan(&exists)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Database error"})
		return
	}
	if exists > 0 {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Username already taken"})
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(creds.Password), bcrypt.DefaultCost)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "error": "Error securing password"})
		return
	}

	// Insert into SampleDB Users table
	_, err = db.Exec(fmt.Sprintf("INSERT INTO Users (Username, Password) VALUES ('%s', '%s')", creds.Username, string(hash)))
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
	// SampleDB Products has: Id, Name, Price (no Quantity)
	rows, err := db.Query("SELECT Id, Name, Price FROM Products ORDER BY Id")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Price); err != nil {
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

	_, err := db.Exec(fmt.Sprintf("INSERT INTO Products (Name, Price) VALUES ('%s', %f)", p.Name, p.Price))
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

	_, err := db.Exec(fmt.Sprintf("UPDATE Products SET Name = '%s', Price = %f WHERE Id = %d", p.Name, p.Price, p.ID))
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

	_, err := db.Exec(fmt.Sprintf("DELETE FROM Products WHERE Id = %d", data.ID))
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
		rows, err = db.Query(fmt.Sprintf("SELECT Id, Name, Price FROM Products WHERE Id = %d", id))
	} else {
		rows, err = db.Query(fmt.Sprintf("SELECT Id, Name, Price FROM Products WHERE Name LIKE '%%%s%%'", searchTerm))
	}
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Price); err != nil {
			continue
		}
		products = append(products, p)
	}
	json.NewEncoder(w).Encode(products)
}
