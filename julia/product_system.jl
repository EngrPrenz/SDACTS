# Product Management System - Configured for SampleDB
# Tables: Users (Id, Username, Password) | Products (Id, Name, Price)
# NOTE: Passwords are stored and compared as plain text to match your schema.

using ODBC
using DataFrames

# =============================================================================
# DATABASE CONNECTION CONFIGURATION
# =============================================================================
const SERVER   = "ACER-NITROV15-F\\SQLEXPRESS"
const DATABASE = "SampleDB"

# =============================================================================
# CONNECTION FUNCTION
# =============================================================================
function get_connection()
    conn_string = "Driver={ODBC Driver 17 for SQL Server};Server=$SERVER;Database=$DATABASE;Trusted_Connection=yes;"
    try
        conn = ODBC.Connection(conn_string)
        println("✓ Connected to $DATABASE successfully!")
        return conn
    catch e
        println("✗ Error connecting to database:")
        println(e)
        return nothing
    end
end

# =============================================================================
# USER FUNCTIONS
# Table: Users (Id INT IDENTITY PK, Username NVARCHAR(50) UNIQUE, Password NVARCHAR(255))
# Passwords are plain text — no hashing — to match your existing data.
# =============================================================================

function register_user(username::String, password::String)
    length(username) < 3  && (println("✗ Username must be at least 3 characters."); return false)
    length(password) < 4  && (println("✗ Password must be at least 4 characters."); return false)
    length(username) > 50 && (println("✗ Username must be 50 characters or fewer."); return false)

    conn = get_connection()
    conn === nothing && return false

    try
        # Check for duplicate username
        result = ODBC.DBInterface.execute(conn,
            "SELECT Id FROM Users WHERE Username = ?", [username])
        df = DataFrame(result)
        if nrow(df) > 0
            println("✗ Username '$username' is already taken.")
            return false
        end

        ODBC.DBInterface.execute(conn,
            "INSERT INTO Users (Username, Password) VALUES (?, ?)",
            [username, password])
        println("✓ User '$username' registered successfully!")
        return true
    catch e
        println("✗ Error registering user:")
        println(e)
        return false
    end
end

function login_user(username::String, password::String)
    conn = get_connection()
    conn === nothing && return false

    try
        result = ODBC.DBInterface.execute(conn,
            "SELECT Password FROM Users WHERE Username = ?", [username])
        rows = collect(result)

        if isempty(rows) || String(rows[1][1]) != password
            println("✗ Invalid username or password.")
            return false
        end

        println("✓ Login successful! Welcome, $username.")
        return true
    catch e
        println("✗ Error during login:")
        println(e)
        return false
    end
end

function list_users()
    conn = get_connection()
    conn === nothing && return DataFrame()

    try
        result = ODBC.DBInterface.execute(conn,
            "SELECT Id, Username FROM Users ORDER BY Id")
        df = DataFrame(result)
        nrow(df) == 0 ? println("No users found.") : println(df)
        return df
    catch e
        println("✗ Error listing users:")
        println(e)
        return DataFrame()
    end
end

# =============================================================================
# PRODUCT FUNCTIONS
# Table: Products (Id INT IDENTITY PK, Name NVARCHAR(100), Price DECIMAL(10,2))
# No quantity column — matches your SampleDB schema exactly.
# =============================================================================

function add_product(name::String, price::Float64)
    conn = get_connection()
    conn === nothing && return false

    try
        ODBC.DBInterface.execute(conn,
            "INSERT INTO Products (Name, Price) VALUES (?, ?)",
            [name, price])
        println("✓ Product '$name' added successfully!")
        return true
    catch e
        println("✗ Error adding product:")
        println(e)
        return false
    end
end

function search_products(search_term::String="")
    conn = get_connection()
    conn === nothing && return DataFrame()

    try
        if isempty(search_term)
            result = ODBC.DBInterface.execute(conn,
                "SELECT Id, Name, Price FROM Products ORDER BY Id")
        else
            result = ODBC.DBInterface.execute(conn,
                "SELECT Id, Name, Price FROM Products WHERE Name LIKE ? ORDER BY Id",
                ["%$search_term%"])
        end

        df = DataFrame(result)
        nrow(df) == 0 ? println("No products found.") :
                        println("\nFound $(nrow(df)) product(s):\n$df")
        return df
    catch e
        println("✗ Error searching products:")
        println(e)
        return DataFrame()
    end
end

function get_product_by_id(product_id::Int)
    conn = get_connection()
    conn === nothing && return DataFrame()

    try
        result = ODBC.DBInterface.execute(conn,
            "SELECT Id, Name, Price FROM Products WHERE Id = ?", [product_id])
        df = DataFrame(result)
        nrow(df) == 0 ? println("Product with ID $product_id not found.") :
                        println("\nProduct Details:\n$df")
        return df
    catch e
        println("✗ Error retrieving product:")
        println(e)
        return DataFrame()
    end
end

function update_product(product_id::Int;
                        name::Union{String,Nothing}=nothing,
                        price::Union{Float64,Nothing}=nothing)
    conn = get_connection()
    conn === nothing && return false

    try
        update_fields = String[]
        params        = Any[]

        if name !== nothing
            push!(update_fields, "Name = ?")
            push!(params, name)
        end
        if price !== nothing
            push!(update_fields, "Price = ?")
            push!(params, price)
        end

        if isempty(update_fields)
            println("No fields to update.")
            return false
        end

        push!(params, product_id)
        sql = "UPDATE Products SET $(join(update_fields, ", ")) WHERE Id = ?"
        ODBC.DBInterface.execute(conn, sql, params)
        println("✓ Product ID $product_id updated successfully!")
        return true
    catch e
        println("✗ Error updating product:")
        println(e)
        return false
    end
end

function delete_product(product_id::Int)
    conn = get_connection()
    conn === nothing && return false

    try
        result = ODBC.DBInterface.execute(conn,
            "SELECT Name FROM Products WHERE Id = ?", [product_id])
        df = DataFrame(result)
        if nrow(df) == 0
            println("Product with ID $product_id not found.")
            return false
        end

        product_name = df[1, :Name]
        ODBC.DBInterface.execute(conn,
            "DELETE FROM Products WHERE Id = ?", [product_id])
        println("✓ Product '$product_name' (ID: $product_id) deleted successfully!")
        return true
    catch e
        println("✗ Error deleting product:")
        println(e)
        return false
    end
end

# =============================================================================
# INTERACTIVE MENU SYSTEM
# =============================================================================

function display_menu()
    println("\n" * "="^55)
    println("        PRODUCT MANAGEMENT SYSTEM  –  SampleDB")
    println("="^55)
    println("  Products")
    println("    1. Add Product")
    println("    2. Search Products")
    println("    3. View All Products")
    println("    4. Update Product")
    println("    5. Delete Product")
    println("  Users")
    println("    6. Register User")
    println("    7. Login Test")
    println("    8. List Users")
    println("    0. Exit")
    println("="^55)
    print("Enter your choice: ")
end

function run_menu()
    println("\n🔧 Product Management System (SampleDB) Starting...")

    while true
        display_menu()
        choice = readline()

        if choice == "1"
            println("\n--- Add New Product ---")
            print("Product Name : "); name  = readline()
            print("Price        : "); price = parse(Float64, readline())
            add_product(name, price)

        elseif choice == "2"
            println("\n--- Search Products ---")
            print("Search term  : "); term = readline()
            search_products(term)

        elseif choice == "3"
            println("\n--- All Products ---")
            search_products("")

        elseif choice == "4"
            println("\n--- Update Product ---")
            print("Product ID   : "); id = parse(Int, readline())
            get_product_by_id(id)

            println("\nEnter new values (press Enter to skip):")
            print("New Name  : "); ni = readline()
            name = isempty(ni) ? nothing : ni

            print("New Price : "); pi = readline()
            price = isempty(pi) ? nothing : parse(Float64, pi)

            update_product(id, name=name, price=price)

        elseif choice == "5"
            println("\n--- Delete Product ---")
            print("Product ID : "); id = parse(Int, readline())
            print("Are you sure? (yes/no): ")
            lowercase(readline()) in ("yes","y") ? delete_product(id) :
                                                    println("Deletion cancelled.")

        elseif choice == "6"
            println("\n--- Register User ---")
            print("Username : "); uname = readline()
            print("Password : "); pwd   = readline()
            register_user(uname, pwd)

        elseif choice == "7"
            println("\n--- Login Test ---")
            print("Username : "); uname = readline()
            print("Password : "); pwd   = readline()
            login_user(uname, pwd)

        elseif choice == "8"
            println("\n--- All Users ---")
            list_users()

        elseif choice == "0"
            println("\n👋 Thank you for using Product Management System!")
            break

        else
            println("\n⚠ Invalid choice. Please try again.")
        end

        println("\nPress Enter to continue...")
        readline()
    end
end

# =============================================================================
# QUICK HELPERS
# =============================================================================

function test_connection()
    println("Testing connection to $SERVER / $DATABASE ...")
    conn = get_connection()
    conn !== nothing ? println("✅ Connection successful!") :
                       println("❌ Connection failed!")
    return conn !== nothing
end

view_all() = search_products("")

# =============================================================================
# MAIN ENTRY POINT
# =============================================================================

println("\n✓ Product Management System (SampleDB) loaded!")
println("\nAvailable functions:")
println("  run_menu()                                – interactive menu")
println("  test_connection()                         – verify DB connection")
println("  view_all()                                – list all products")
println("  add_product(name, price)")
println("  search_products(term)")
println("  get_product_by_id(id)")
println("  update_product(id, name=..., price=...)")
println("  delete_product(id)")
println("  register_user(username, password)")
println("  login_user(username, password)")
println("  list_users()")
println("\nExample: run_menu()")