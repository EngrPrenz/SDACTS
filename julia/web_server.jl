# Web Server for Product Management System
# Configured for SampleDB with Users (Id, Username, Password) and Products (Id, Name, Price)
# NOTE: Passwords stored as plain text to match your existing Users table schema

using HTTP
using JSON3
using ODBC

# =============================================================================
# DATABASE CONNECTION
# =============================================================================
const SERVER   = "ACER-NITROV15-F\\SQLEXPRESS"
const DATABASE = "SampleDB"

function get_connection()
    conn_str = "Driver={ODBC Driver 17 for SQL Server};Server=$SERVER;Database=$DATABASE;Trusted_Connection=yes;"
    return ODBC.Connection(conn_str)
end

# =============================================================================
# SESSION STORE  (in-memory: token → username)
# =============================================================================
const SESSIONS = Dict{String, String}()

function create_session(username::String)::String
    token = bytes2hex(rand(UInt8, 32))
    SESSIONS[token] = username
    return token
end

function get_session_user(token::String)::Union{String, Nothing}
    return get(SESSIONS, token, nothing)
end

function delete_session(token::String)
    delete!(SESSIONS, token)
end

# =============================================================================
# COOKIE HELPERS
# =============================================================================
function get_cookie(req::HTTP.Request, name::String)::Union{String, Nothing}
    for (k, v) in req.headers
        if lowercase(k) == "cookie"
            for part in split(v, ";")
                kv = strip(part)
                if startswith(kv, name * "=")
                    return String(kv[length(name)+2:end])
                end
            end
        end
    end
    return nothing
end

function auth_user(req::HTTP.Request)::Union{String, Nothing}
    token = get_cookie(req, "session")
    token === nothing && return nothing
    return get_session_user(token)
end

# =============================================================================
# USER AUTH API
# NOTE: Passwords are stored and compared as plain text to match your existing
#       Users table. Do NOT add hashing unless you migrate all existing passwords.
# =============================================================================
function api_register(req::HTTP.Request)
    body     = JSON3.read(String(req.body))
    username = String(strip(String(get(body, :username, ""))))
    password = String(get(body, :password, ""))

    length(username) < 3 && return json_resp(400, Dict("success"=>false,"error"=>"Username must be at least 3 characters."))
    length(password) < 4 && return json_resp(400, Dict("success"=>false,"error"=>"Password must be at least 4 characters."))
    length(username) > 50 && return json_resp(400, Dict("success"=>false,"error"=>"Username must be 50 characters or fewer."))

    conn   = get_connection()
    cursor = ODBC.DBInterface.execute(conn,
        "SELECT Id FROM Users WHERE Username = ?", [username])
    !isempty(cursor) && return json_resp(409, Dict("success"=>false,"error"=>"Username already taken."))

    ODBC.DBInterface.execute(conn,
        "INSERT INTO Users (Username, Password) VALUES (?, ?)",
        [username, password])

    return json_resp(200, Dict("success"=>true))
end

function api_login(req::HTTP.Request)
    body     = JSON3.read(String(req.body))
    username = String(strip(String(get(body, :username, ""))))
    password = String(get(body, :password, ""))

    conn   = get_connection()
    cursor = ODBC.DBInterface.execute(conn,
        "SELECT Password FROM Users WHERE Username = ?", [username])
    rows   = collect(cursor)

    if isempty(rows) || String(rows[1][1]) != password
        return json_resp(401, Dict("success"=>false,"error"=>"Invalid username or password."))
    end

    token   = create_session(username)
    headers = make_headers(["Content-Type" => "application/json",
                            "Set-Cookie"   => "session=$token; Path=/; HttpOnly; SameSite=Strict"])
    return HTTP.Response(200, headers, JSON3.write(Dict("success"=>true)))
end

function api_logout(req::HTTP.Request)
    token = get_cookie(req, "session")
    token !== nothing && delete_session(token)
    headers = make_headers(["Content-Type" => "application/json",
                            "Set-Cookie"   => "session=; Path=/; HttpOnly; Max-Age=0"])
    return HTTP.Response(200, headers, JSON3.write(Dict("success"=>true)))
end

# =============================================================================
# PRODUCT DB FUNCTIONS
# Table: Products (Id INT, Name NVARCHAR(100), Price DECIMAL(10,2))
# =============================================================================

safe_int(v)    = ismissing(v) ? 0   : Int(v)
safe_float(v)  = ismissing(v) ? 0.0 : Float64(v)
safe_string(v) = ismissing(v) ? ""  : string(v)

function cursor_to_array(cursor)
    result = Dict{String,Any}[]
    for row in cursor
        push!(result, Dict(
            "id"    => safe_int(row[1]),
            "name"  => safe_string(row[2]),
            "price" => safe_float(row[3])
        ))
    end
    return result
end

function get_all_products()
    conn   = get_connection()
    cursor = ODBC.DBInterface.execute(conn,
        "SELECT Id, Name, Price FROM Products ORDER BY Id")
    return cursor_to_array(cursor)
end

function add_product_db(name, price)
    conn = get_connection()
    ODBC.DBInterface.execute(conn,
        "INSERT INTO Products (Name, Price) VALUES (?, ?)",
        [name, Float64(price)])
end

function update_product_db(id, name, price)
    conn = get_connection()
    ODBC.DBInterface.execute(conn,
        "UPDATE Products SET Name=?, Price=? WHERE Id=?",
        [name, Float64(price), Int(id)])
end

function delete_product_db(id)
    conn = get_connection()
    ODBC.DBInterface.execute(conn,
        "DELETE FROM Products WHERE Id=?", [Int(id)])
end

function search_products_db(term)
    conn   = get_connection()
    cursor = ODBC.DBInterface.execute(conn,
        "SELECT Id, Name, Price FROM Products WHERE Name LIKE ? ORDER BY Id",
        ["%$(term)%"])
    return cursor_to_array(cursor)
end

# =============================================================================
# RESPONSE HELPERS
# =============================================================================
function make_headers(extra = Pair{String,String}[])
    h = Pair{String,String}[
        "Access-Control-Allow-Origin"  => "*",
        "Access-Control-Allow-Methods" => "GET, POST, OPTIONS",
        "Access-Control-Allow-Headers" => "Content-Type",
    ]
    append!(h, extra)
    return h
end

function json_resp(status::Int, data)
    return HTTP.Response(status, make_headers(["Content-Type"=>"application/json"]),
                         JSON3.write(data))
end

function serve_file(path::String, mime::String)
    try
        return HTTP.Response(200, make_headers(["Content-Type"=>mime]), read(path, String))
    catch
        return HTTP.Response(404, "File not found: $path")
    end
end

function redirect_to(loc::String)
    return HTTP.Response(302, ["Location"=>loc], "")
end

# =============================================================================
# MAIN REQUEST HANDLER
# =============================================================================
function handle_request(req::HTTP.Request)
    req.method == "OPTIONS" && return HTTP.Response(200, make_headers(), "")

    target = req.target
    method = req.method

    # ── Public pages ─────────────────────────────────────────────────────────
    target == "/login"    && return serve_file("login.html",    "text/html")
    target == "/register" && return serve_file("register.html", "text/html")

    # ── Public API ────────────────────────────────────────────────────────────
    if target == "/api/register" && method == "POST"
        return try api_register(req) catch e; json_resp(500, Dict("success"=>false,"error"=>string(e))) end
    end
    if target == "/api/login" && method == "POST"
        return try api_login(req) catch e; json_resp(500, Dict("success"=>false,"error"=>string(e))) end
    end
    target == "/api/logout" && return api_logout(req)

    # ── Public static ─────────────────────────────────────────────────────────
    target == "/style.css" && return serve_file("style.css", "text/css")

    # ── Auth guard ────────────────────────────────────────────────────────────
    user = auth_user(req)
    if user === nothing
        startswith(target, "/api/") && return json_resp(401, Dict("error"=>"Unauthorized"))
        return redirect_to("/login")
    end

    # ── Protected pages ───────────────────────────────────────────────────────
    (target == "/" || target == "/index.html") && return serve_file("index.html", "text/html")
    target == "/script.js" && return serve_file("script.js", "application/javascript")

    # ── Protected API ─────────────────────────────────────────────────────────
    try
        target == "/api/products" && method == "GET" &&
            return json_resp(200, get_all_products())

        if startswith(target, "/api/products/search") && method == "GET"
            params = HTTP.URIs.queryparams(HTTP.URIs.URI(target))
            return json_resp(200, search_products_db(get(params, "q", "")))
        end

        if target == "/api/products/add" && method == "POST"
            b = JSON3.read(String(req.body))
            add_product_db(b.name, b.price)
            return json_resp(200, Dict("success"=>true))
        end

        if target == "/api/products/update" && method == "POST"
            b = JSON3.read(String(req.body))
            update_product_db(b.id, b.name, b.price)
            return json_resp(200, Dict("success"=>true))
        end

        if target == "/api/products/delete" && method == "POST"
            b = JSON3.read(String(req.body))
            delete_product_db(b.id)
            return json_resp(200, Dict("success"=>true))
        end

        return json_resp(404, Dict("error"=>"Not found"))

    catch e
        println("❌ Error: $e")
        return json_resp(500, Dict("error"=>string(e)))
    end
end

# =============================================================================
# START SERVER
# =============================================================================
function start_server(port=8080)
    println("=" ^ 60)
    println("   PRODUCT MANAGEMENT SYSTEM  –  SampleDB")
    println("=" ^ 60)
    println("\n🚀 Server → http://localhost:$port")
    println("\n📋 Routes:")
    println("   GET  /login                  Login page")
    println("   GET  /register               Register page")
    println("   POST /api/login              Authenticate")
    println("   POST /api/register           Create account")
    println("   GET  /api/logout             Sign out")
    println("   GET  /                       Main app  (auth required)")
    println("   GET  /api/products           All products")
    println("   GET  /api/products/search?q= Search")
    println("   POST /api/products/add       Add product  { name, price }")
    println("   POST /api/products/update    Update product  { id, name, price }")
    println("   POST /api/products/delete    Delete product  { id }")
    println("\n⏹  Ctrl+C to stop")
    println("=" ^ 60 * "\n")

    try
        HTTP.serve(handle_request, "0.0.0.0", port)
    catch e
        isa(e, InterruptException) ? println("\n👋 Stopped.") : println("\n❌ $e")
    end
end

println("\n✓ Loaded!  Run: start_server()")