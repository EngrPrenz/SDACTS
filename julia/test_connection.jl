# test_connection.jl - Quick connection test for VS Code
# 
# HOW TO USE IN VS CODE:
# 1. Make sure Julia extension is installed
# 2. Press Ctrl+Shift+P → "Julia: Start REPL"
# 3. Press Ctrl+Enter to run this entire file
# OR
# 4. Select code and press Alt+Enter to run selection

println("="^60)
println("    SQL Server Connection Test")
println("="^60)

# Load the main program
println("\n📦 Loading product_manager.jl...")
include("product_manager.jl")

# Test 1: Check if packages are installed
println("\n🔍 Checking required packages...")
try
    using ODBC
    using DataFrames
    println("✅ ODBC package: OK")
    println("✅ DataFrames package: OK")
catch e
    println("❌ Missing packages! Run this in Julia REPL:")
    println("   using Pkg")
    println("   Pkg.add(\"ODBC\")")
    println("   Pkg.add(\"DataFrames\")")
    exit(1)
end

# Test 2: Test connection
println("\n🔌 Testing database connection...")
println("   Server: $SERVER")
println("   Database: $DATABASE")
println("   Auth: $(USE_WINDOWS_AUTH ? "Windows Authentication" : "SQL Server Authentication")")
println()

conn = get_connection()

if conn !== nothing
    println("\n" * "="^60)
    println("    ✅ SUCCESS! Connection established!")
    println("="^60)
    
    # Test 3: Check if Products table exists
    println("\n🔍 Checking for Products table...")
    try
        result = ODBC.query(conn, """
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_NAME = 'Products'
        """)
        
        if result[1, :count] > 0
            println("✅ Products table exists!")
            
            # Get row count
            count_result = ODBC.query(conn, "SELECT COUNT(*) as total FROM Products")
            total = count_result[1, :total]
            println("   Contains $total product(s)")
            
            if total > 0
                println("\n📋 Sample products:")
                sample = ODBC.query(conn, "SELECT TOP 5 * FROM Products")
                println(sample)
            end
        else
            println("⚠ Products table does not exist yet.")
            println("   Run: setup_database() to create it")
        end
    catch e
        println("⚠ Could not check for Products table")
        println("   Run: setup_database() to create it")
    end
    
    ODBC.close!(conn)
    
    println("\n" * "="^60)
    println("    🎉 All tests passed!")
    println("="^60)
    println("\n📝 Next steps:")
    println("   1. Run: main() for interactive menu")
    println("   2. Run: setup_database() to create Products table (if needed)")
    println("   3. Run: add_product(\"name\", \"category\", price, quantity)")
    println("   4. Run: search_products(\"\") to view all products")
    println("\n💡 Tip: Keep Julia REPL open for faster execution!")
    
else
    println("\n" * "="^60)
    println("    ❌ CONNECTION FAILED")
    println("="^60)
    println("\n🔧 Troubleshooting:")
    println("   1. Check if SQL Server is running")
    println("   2. Verify SERVER and DATABASE settings in product_manager.jl")
    println("   3. Check authentication settings (Windows vs SQL Auth)")
    println("   4. Make sure ODBC Driver is installed")
    println("\n📖 See VSCODE_SETUP.md for detailed help")
end

println("\n" * "="^60)
