# Product Management System - Customized for Your ProductDB
# Matches your existing table structure: products (id, name, price, quantity)

using ODBC
using DataFrames

# =============================================================================
# DATABASE CONNECTION CONFIGURATION
# =============================================================================
const SERVER = "SQLEXPRESS01"
const DATABASE = "ProductDB"
const USE_WINDOWS_AUTH = true

# =============================================================================
# CONNECTION FUNCTION
# =============================================================================
function get_connection()
    """
    Establishes connection to SQL Server database
    """
    conn_string = "Driver={ODBC Driver 17 for SQL Server};Server=$SERVER;Database=$DATABASE;Trusted_Connection=yes;"
    
    try
        conn = ODBC.Connection(conn_string)
        println("✓ Connected to database successfully!")
        return conn
    catch e
        println("✗ Error connecting to database:")
        println(e)
        return nothing
    end
end

# =============================================================================
# CRUD OPERATIONS
# =============================================================================

function add_product(name::String, price::Float64, quantity::Int)
    """
    Adds a new product to the database
    
    Parameters:
        name: Product name
        price: Product price
        quantity: Stock quantity
    
    Returns: true if successful, false otherwise
    """
    conn = get_connection()
    if conn === nothing
        return false
    end
    
    try
        insert_sql = "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)"
        
        ODBC.DBInterface.execute(conn, insert_sql, [name, price, quantity])
        println("✓ Product '$name' added successfully!")
        
        
        return true
    catch e
        println("✗ Error adding product:")
        println(e)
        
        return false
    end
end

function search_products(search_term::String="")
    """
    Searches for products by name
    If search_term is empty, returns all products
    
    Parameters:
        search_term: Search keyword for product name
    
    Returns: DataFrame with search results
    """
    conn = get_connection()
    if conn === nothing
        return DataFrame()
    end
    
    try
        if isempty(search_term)
            # Return all products
            query = "SELECT * FROM products ORDER BY id"
            result = ODBC.DBInterface.execute(conn, query)
        else
            # Search by name
            query = "SELECT * FROM products WHERE name LIKE ? ORDER BY id"
            search_pattern = "%$search_term%"
            result = ODBC.DBInterface.execute(conn, query, [search_pattern])
        end
        
        df = DataFrame(result)
        
        
        if nrow(df) == 0
            println("No products found.")
        else
            println("\nFound $(nrow(df)) product(s):")
            println(df)
        end
        
        return df
    catch e
        println("✗ Error searching products:")
        println(e)
        
        return DataFrame()
    end
end

function get_product_by_id(product_id::Int)
    """
    Retrieves a specific product by ID
    
    Parameters:
        product_id: Product ID
    
    Returns: DataFrame with product details
    """
    conn = get_connection()
    if conn === nothing
        return DataFrame()
    end
    
    try
        query = "SELECT * FROM products WHERE id = ?"
        result = ODBC.DBInterface.execute(conn, query, [product_id])
        df = DataFrame(result)
        
        
        
        if nrow(df) == 0
            println("Product with ID $product_id not found.")
        else
            println("\nProduct Details:")
            println(df)
        end
        
        return df
    catch e
        println("✗ Error retrieving product:")
        println(e)
        
        return DataFrame()
    end
end

function update_product(product_id::Int; name::Union{String, Nothing}=nothing,
                       price::Union{Float64, Nothing}=nothing,
                       quantity::Union{Int, Nothing}=nothing)
    """
    Updates an existing product's information
    
    Parameters:
        product_id: ID of the product to update
        name: New product name (optional)
        price: New price (optional)
        quantity: New stock quantity (optional)
    
    Returns: true if successful, false otherwise
    """
    conn = get_connection()
    if conn === nothing
        return false
    end
    
    try
        # Build dynamic UPDATE query
        update_fields = String[]
        params = []
        
        if name !== nothing
            push!(update_fields, "name = ?")
            push!(params, name)
        end
        if price !== nothing
            push!(update_fields, "price = ?")
            push!(params, price)
        end
        if quantity !== nothing
            push!(update_fields, "quantity = ?")
            push!(params, quantity)
        end
        
        if isempty(update_fields)
            println("No fields to update.")
            
            return false
        end
        
        push!(params, product_id)
        
        update_sql = "UPDATE products SET $(join(update_fields, ", ")) WHERE id = ?"
        
        ODBC.DBInterface.execute(conn, update_sql, params)
        println("✓ Product ID $product_id updated successfully!")
        
        
        return true
    catch e
        println("✗ Error updating product:")
        println(e)
        
        return false
    end
end

function delete_product(product_id::Int)
    """
    Deletes a product from the database
    
    Parameters:
        product_id: ID of the product to delete
    
    Returns: true if successful, false otherwise
    """
    conn = get_connection()
    if conn === nothing
        return false
    end
    
    try
        # Check if product exists
        check_query = "SELECT name FROM products WHERE id = ?"
        result = ODBC.DBInterface.execute(conn, check_query, [product_id])
        df = DataFrame(result)
        
        if nrow(df) == 0
            println("Product with ID $product_id not found.")
            
            return false
        end
        
        product_name = df[1, :name]
        
        # Delete the product
        delete_sql = "DELETE FROM products WHERE id = ?"
        ODBC.DBInterface.execute(conn, delete_sql, [product_id])
        
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
    println("\n" * "="^50)
    println("      PRODUCT MANAGEMENT SYSTEM")
    println("="^50)
    println("1. Add Product")
    println("2. Search Products")
    println("3. View All Products")
    println("4. Update Product")
    println("5. Delete Product")
    println("0. Exit")
    println("="^50)
    print("Enter your choice: ")
end

function run_menu()
    """
    Runs the interactive menu system
    """
    println("\n🔧 Product Management System Starting...")
    
    while true
        display_menu()
        choice = readline()
        
        if choice == "1"
            # Add Product
            println("\n--- Add New Product ---")
            print("Product Name: ")
            name = readline()
            print("Price: ")
            price = parse(Float64, readline())
            print("Quantity: ")
            quantity = parse(Int, readline())
            
            add_product(name, price, quantity)
            
        elseif choice == "2"
            # Search Products
            println("\n--- Search Products ---")
            print("Enter search term (product name): ")
            search_term = readline()
            search_products(search_term)
            
        elseif choice == "3"
            # View All Products
            println("\n--- All Products ---")
            search_products("")
            
        elseif choice == "4"
            # Update Product
            println("\n--- Update Product ---")
            print("Enter Product ID to update: ")
            product_id = parse(Int, readline())
            
            # Show current details
            get_product_by_id(product_id)
            
            println("\nEnter new values (press Enter to skip):")
            
            print("New Product Name: ")
            name_input = readline()
            name = isempty(name_input) ? nothing : name_input
            
            print("New Price: ")
            price_input = readline()
            price = isempty(price_input) ? nothing : parse(Float64, price_input)
            
            print("New Quantity: ")
            quantity_input = readline()
            quantity = isempty(quantity_input) ? nothing : parse(Int, quantity_input)
            
            update_product(product_id, name=name, price=price, quantity=quantity)
            
        elseif choice == "5"
            # Delete Product
            println("\n--- Delete Product ---")
            print("Enter Product ID to delete: ")
            product_id = parse(Int, readline())
            
            print("Are you sure? (yes/no): ")
            confirmation = lowercase(readline())
            
            if confirmation == "yes" || confirmation == "y"
                delete_product(product_id)
            else
                println("Deletion cancelled.")
            end
            
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
# QUICK TEST FUNCTIONS
# =============================================================================

function test_connection()
    """
    Quick test to verify connection works
    """
    println("Testing connection to $SERVER...")
    conn = get_connection()
    
    if conn !== nothing
        println("✅ Connection successful!")
        
        return true
    else
        println("❌ Connection failed!")
        return false
    end
end

function view_all()
    """
    Quick function to view all products
    """
    search_products("")
end

# =============================================================================
# MAIN ENTRY POINT
# =============================================================================

println("\n✓ Product Management System loaded!")
println("\nAvailable functions:")
println("  - run_menu()           : Start interactive menu")
println("  - test_connection()    : Test database connection")
println("  - view_all()           : View all products")
println("  - add_product(name, price, quantity)")
println("  - search_products(term)")
println("  - get_product_by_id(id)")
println("  - update_product(id, name=..., price=..., quantity=...)")
println("  - delete_product(id)")
println("\nExample: run_menu() to start")