from flask import (
    Flask, render_template, request, redirect,
    url_for, session, flash, jsonify
)
from config import get_connection, SECRET_KEY
import hashlib

app = Flask(__name__)
app.secret_key = SECRET_KEY

# ──────────────────────────────────────────────────────────────
# HELPERS
# ──────────────────────────────────────────────────────────────

def hash_pw(password: str) -> str:
    """SHA-256 hash – matches plain '1234' stored in the DB only when
       the DB value is ALREADY a hash. If your DB stores plain text,
       we fall back to plain-text comparison in login()."""
    return hashlib.sha256(password.encode()).hexdigest()


def login_required(f):
    from functools import wraps
    @wraps(f)
    def decorated(*args, **kwargs):
        if "user" not in session:
            return redirect(url_for("login"))
        return f(*args, **kwargs)
    return decorated


# ──────────────────────────────────────────────────────────────
# AUTH
# ──────────────────────────────────────────────────────────────

@app.route("/", methods=["GET", "POST"])
@app.route("/login", methods=["GET", "POST"])
def login():
    if "user" in session:
        return redirect(url_for("dashboard"))

    error = None
    if request.method == "POST":
        username = request.form.get("username", "").strip()
        password = request.form.get("password", "").strip()

        try:
            conn = get_connection()
            cur  = conn.cursor()
            cur.execute("SELECT Id, Password FROM Users WHERE Username = ?", username)
            row = cur.fetchone()
            conn.close()

            if row:
                stored_pw = row[1]
                # Support both plain-text AND hashed passwords
                if stored_pw == password or stored_pw == hash_pw(password):
                    session["user"] = username
                    session["user_id"] = row[0]
                    return redirect(url_for("dashboard"))

            error = "Invalid username or password."
        except Exception as e:
            error = f"Database error: {e}"

    return render_template("login.html", error=error)


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))


# ──────────────────────────────────────────────────────────────
# DASHBOARD
# ──────────────────────────────────────────────────────────────

@app.route("/dashboard")
@login_required
def dashboard():
    try:
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM Users")
        user_count = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM Products")
        prod_count = cur.fetchone()[0]
        cur.execute("SELECT ISNULL(SUM(Price),0) FROM Products")
        total_val  = cur.fetchone()[0]
        conn.close()
    except Exception as e:
        flash(str(e), "danger")
        user_count = prod_count = total_val = 0

    return render_template(
        "dashboard.html",
        user_count=user_count,
        prod_count=prod_count,
        total_val=total_val
    )


# ──────────────────────────────────────────────────────────────
# USERS  (full CRUD)
# ──────────────────────────────────────────────────────────────

@app.route("/users")
@login_required
def users():
    search = request.args.get("q", "").strip()
    try:
        conn = get_connection()
        cur  = conn.cursor()
        if search:
            cur.execute("SELECT Id, Username FROM Users WHERE Username LIKE ?", f"%{search}%")
        else:
            cur.execute("SELECT Id, Username FROM Users ORDER BY Id")
        rows = cur.fetchall()
        conn.close()
    except Exception as e:
        flash(str(e), "danger")
        rows = []
    return render_template("users.html", users=rows, search=search)


@app.route("/users/add", methods=["POST"])
@login_required
def add_user():
    username = request.form.get("username", "").strip()
    password = request.form.get("password", "").strip()
    if not username or not password:
        flash("Username and password are required.", "warning")
        return redirect(url_for("users"))
    try:
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute(
            "INSERT INTO Users (Username, Password) VALUES (?, ?)",
            username, password
        )
        conn.commit()
        conn.close()
        flash(f"User '{username}' created successfully.", "success")
    except Exception as e:
        flash(str(e), "danger")
    return redirect(url_for("users"))


@app.route("/users/edit/<int:uid>", methods=["POST"])
@login_required
def edit_user(uid):
    username = request.form.get("username", "").strip()
    password = request.form.get("password", "").strip()
    if not username:
        flash("Username is required.", "warning")
        return redirect(url_for("users"))
    try:
        conn = get_connection()
        cur  = conn.cursor()
        if password:
            cur.execute(
                "UPDATE Users SET Username=?, Password=? WHERE Id=?",
                username, password, uid
            )
        else:
            cur.execute("UPDATE Users SET Username=? WHERE Id=?", username, uid)
        conn.commit()
        conn.close()
        flash("User updated.", "success")
    except Exception as e:
        flash(str(e), "danger")
    return redirect(url_for("users"))


@app.route("/users/delete/<int:uid>", methods=["POST"])
@login_required
def delete_user(uid):
    if uid == session.get("user_id"):
        flash("You cannot delete your own account.", "warning")
        return redirect(url_for("users"))
    try:
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("DELETE FROM Users WHERE Id=?", uid)
        conn.commit()
        conn.close()
        flash("User deleted.", "success")
    except Exception as e:
        flash(str(e), "danger")
    return redirect(url_for("users"))


@app.route("/users/<int:uid>/json")
@login_required
def get_user_json(uid):
    try:
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("SELECT Id, Username FROM Users WHERE Id=?", uid)
        row = cur.fetchone()
        conn.close()
        if row:
            return jsonify({"id": row[0], "username": row[1]})
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    return jsonify({}), 404


# ──────────────────────────────────────────────────────────────
# PRODUCTS  (full CRUD)
# ──────────────────────────────────────────────────────────────

@app.route("/products")
@login_required
def products():
    search = request.args.get("q", "").strip()
    try:
        conn = get_connection()
        cur  = conn.cursor()
        if search:
            cur.execute(
                "SELECT Id, Name, Price FROM Products WHERE Name LIKE ? ORDER BY Id",
                f"%{search}%"
            )
        else:
            cur.execute("SELECT Id, Name, Price FROM Products ORDER BY Id")
        rows = cur.fetchall()
        conn.close()
        # Convert Decimal price to float so Jinja2/JS renders it cleanly
        rows = [(r[0], r[1], float(r[2])) for r in rows]
    except Exception as e:
        flash(str(e), "danger")
        rows = []
    return render_template("products.html", products=rows, search=search)


@app.route("/products/add", methods=["POST"])
@login_required
def add_product():
    name  = request.form.get("name", "").strip()
    price = request.form.get("price", "").strip()
    if not name or not price:
        flash("Name and price are required.", "warning")
        return redirect(url_for("products"))
    try:
        price_f = float(price)
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("INSERT INTO Products (Name, Price) VALUES (?, ?)", name, price_f)
        conn.commit()
        conn.close()
        flash(f"Product '{name}' added.", "success")
    except ValueError:
        flash("Price must be a number.", "warning")
    except Exception as e:
        flash(str(e), "danger")
    return redirect(url_for("products"))


@app.route("/products/edit/<int:pid>", methods=["POST"])
@login_required
def edit_product(pid):
    name  = request.form.get("name", "").strip()
    price = request.form.get("price", "").strip()
    if not name or not price:
        flash("Name and price are required.", "warning")
        return redirect(url_for("products"))
    try:
        price_f = float(price)
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("UPDATE Products SET Name=?, Price=? WHERE Id=?", name, price_f, pid)
        conn.commit()
        conn.close()
        flash("Product updated.", "success")
    except ValueError:
        flash("Price must be a number.", "warning")
    except Exception as e:
        flash(str(e), "danger")
    return redirect(url_for("products"))


@app.route("/products/delete/<int:pid>", methods=["POST"])
@login_required
def delete_product(pid):
    try:
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("DELETE FROM Products WHERE Id=?", pid)
        conn.commit()
        conn.close()
        flash("Product deleted.", "success")
    except Exception as e:
        flash(str(e), "danger")
    return redirect(url_for("products"))


@app.route("/products/<int:pid>/json")
@login_required
def get_product_json(pid):
    try:
        conn = get_connection()
        cur  = conn.cursor()
        cur.execute("SELECT Id, Name, Price FROM Products WHERE Id=?", pid)
        row = cur.fetchone()
        conn.close()
        if row:
            return jsonify({"id": row[0], "name": row[1], "price": float(row[2])})
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    return jsonify({}), 404


# ──────────────────────────────────────────────────────────────
if __name__ == "__main__":
    app.run(debug=True, port=5000)