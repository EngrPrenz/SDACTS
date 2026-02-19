package dao;

import config.DatabaseConfig;
import model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductDAO - Data Access Object for the Products table
 * 
 * This class handles all database operations for products (CRUD + search).
 * Similar to UserDAO but with an additional search feature.
 * 
 * Each method follows the same pattern:
 * 1. Get connection from DatabaseConfig
 * 2. Prepare SQL statement with placeholders (?)
 * 3. Set parameter values (prevents SQL injection)
 * 4. Execute query/update
 * 5. Process results
 * 6. Resources auto-close via try-with-resources
 */
public class ProductDAO {
    
    /**
     * createProduct() - Inserts a new product into the database
     * 
     * Adds a new product record to the Products table.
     * The database auto-generates the Id using IDENTITY.
     * 
     * @param product Product object containing name and price
     * @return true if product was created successfully, false if failed
     * 
     * SQL: INSERT INTO Products (Name, Price) VALUES (?, ?)
     * 
     * How it works:
     * 1. Connects to database
     * 2. Inserts the product name and price
     * 3. Database automatically generates a new unique Id
     * 4. Returns true if insert succeeded, false if it failed
     */
    public boolean createProduct(Product product) {
        String sql = "INSERT INTO Products (Name, Price) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set the values for the new product
            pstmt.setString(1, product.getName());        // Product name
            pstmt.setBigDecimal(2, product.getPrice());   // Product price (BigDecimal for precision)
            
            // Execute the INSERT and check how many rows were affected
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;  // Success if at least 1 row inserted
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * getAllProducts() - Retrieves all products from the database
     * 
     * Fetches every product record and converts each to a Product object.
     * Used by ProductPanel to display the full product list in the table.
     * 
     * @return List of all Product objects (empty list if none exist)
     * 
     * SQL: SELECT * FROM Products
     * 
     * How it works:
     * 1. Query database for all products
     * 2. For each row returned, create a Product object
     * 3. Add each Product to an ArrayList
     * 4. Return the complete list
     */
    public List<Product> getAllProducts() {
        // ArrayList to collect all products
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Products";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // Loop through each row in the result set
            while (rs.next()) {
                // Extract data from current row and create Product object
                Product product = new Product(
                    rs.getInt("Id"),                // Product ID
                    rs.getString("Name"),           // Product name
                    rs.getBigDecimal("Price")       // Product price (as BigDecimal)
                );
                products.add(product);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return products;  // Return list (may be empty if no products or error occurred)
    }
    
    /**
     * getProductById() - Retrieves a specific product by its ID
     * 
     * Looks up a single product using its unique ID.
     * Useful when you need the full details of one product.
     * 
     * @param id The unique ID of the product to find
     * @return Product object if found, null if not found
     * 
     * SQL: SELECT * FROM Products WHERE Id = ?
     */
    public Product getProductById(int id) {
        String sql = "SELECT * FROM Products WHERE Id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set the ID we're searching for
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            // If a matching row exists, convert it to a Product object
            if (rs.next()) {
                return new Product(
                    rs.getInt("Id"),
                    rs.getString("Name"),
                    rs.getBigDecimal("Price")
                );
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Product not found
        return null;
    }
    
    /**
     * updateProduct() - Updates an existing product's information
     * 
     * Modifies the name and/or price of an existing product.
     * The product is identified by its ID (which cannot be changed).
     * 
     * @param product Product object with updated data (must include valid Id)
     * @return true if product was updated successfully, false if failed
     * 
     * SQL: UPDATE Products SET Name = ?, Price = ? WHERE Id = ?
     * 
     * How it works:
     * 1. Finds the product with matching Id
     * 2. Updates its Name and Price to the new values
     * 3. Returns true if the update affected at least one row
     */
    public boolean updateProduct(Product product) {
        String sql = "UPDATE Products SET Name = ?, Price = ? WHERE Id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set the new values and specify which product to update
            pstmt.setString(1, product.getName());        // New name
            pstmt.setBigDecimal(2, product.getPrice());   // New price
            pstmt.setInt(3, product.getId());             // Which product (by ID)
            
            // Execute the UPDATE and check if any rows were modified
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * deleteProduct() - Removes a product from the database
     * 
     * Permanently deletes a product record by its ID.
     * WARNING: This cannot be undone.
     * 
     * @param id The ID of the product to delete
     * @return true if product was deleted successfully, false if failed or not found
     * 
     * SQL: DELETE FROM Products WHERE Id = ?
     */
    public boolean deleteProduct(int id) {
        String sql = "DELETE FROM Products WHERE Id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Specify which product to delete
            pstmt.setInt(1, id);
            
            // Execute the DELETE and check if any rows were removed
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * searchProductsByName() - Finds products matching a search term
     * 
     * Searches for products whose name contains the search term (case-insensitive).
     * Uses SQL LIKE with wildcards to find partial matches.
     * 
     * @param name The search term (e.g., "laptop")
     * @return List of matching Product objects (empty if no matches)
     * 
     * SQL: SELECT * FROM Products WHERE Name LIKE ?
     * 
     * Examples:
     * - searchProductsByName("laptop") finds: "Gaming Laptop", "Dell Laptop", "laptop case"
     * - searchProductsByName("key") finds: "Keyboard", "USB Key", "Keycap set"
     * 
     * How it works:
     * 1. Wraps the search term in % wildcards: %searchTerm%
     * 2. SQL LIKE finds any product name containing that pattern
     * 3. Returns all matching products as a List
     */
    public List<Product> searchProductsByName(String name) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Products WHERE Name LIKE ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Add wildcards around the search term for partial matching
            // "laptop" becomes "%laptop%" which matches "Gaming Laptop" or "laptop case"
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            
            // Loop through all matching results
            while (rs.next()) {
                Product product = new Product(
                    rs.getInt("Id"),
                    rs.getString("Name"),
                    rs.getBigDecimal("Price")
                );
                products.add(product);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return products;  // Return list of matches (may be empty)
    }
}