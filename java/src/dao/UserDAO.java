package dao;

import config.DatabaseConfig;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO - Data Access Object for the Users table
 * 
 * This class handles all database operations for users (CRUD + authentication).
 * It follows the DAO pattern to separate database logic from business logic and UI.
 * 
 * Each method:
 * 1. Gets a database connection from DatabaseConfig
 * 2. Creates a PreparedStatement (prevents SQL injection)
 * 3. Executes the SQL query/update
 * 4. Processes results (if any)
 * 5. Closes resources automatically via try-with-resources
 * 
 * The try-with-resources pattern (try (...)) automatically closes Connection,
 * PreparedStatement, and ResultSet even if exceptions occur.
 */
public class UserDAO {
    
    /**
     * authenticate() - Validates user login credentials
     * 
     * This method checks if a username/password combination exists in the database.
     * Used by the LoginFrame to verify user credentials before allowing access.
     * 
     * @param username The username to check
     * @param password The password to verify
     * @return User object if credentials are valid, null if invalid
     * 
     * SQL: SELECT * FROM Users WHERE Username = ? AND Password = ?
     * 
     * How it works:
     * 1. Connects to database
     * 2. Searches for a user with matching username AND password
     * 3. If found, creates and returns a User object with their data
     * 4. If not found, returns null (invalid credentials)
     */
    public User authenticate(String username, String password) {
        // SQL query with ? placeholders (prevents SQL injection)
        String sql = "SELECT * FROM Users WHERE Username = ? AND Password = ?";
        
        // try-with-resources: automatically closes connection and statement when done
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Replace the ? placeholders with actual values
            // Using PreparedStatement prevents SQL injection attacks
            pstmt.setString(1, username);  // Replace first ? with username
            pstmt.setString(2, password);  // Replace second ? with password
            
            // Execute the query and get results
            ResultSet rs = pstmt.executeQuery();
            
            // Check if a matching row was found
            if (rs.next()) {
                // User found - extract data from the result and create User object
                return new User(
                    rs.getInt("Id"),           // Get Id column as int
                    rs.getString("Username"),   // Get Username column as String
                    rs.getString("Password")    // Get Password column as String
                );
            }
        } catch (SQLException e) {
            // Database error occurred - print stack trace for debugging
            e.printStackTrace();
        }
        
        // No matching user found or an error occurred
        return null;
    }
    
    /**
     * createUser() - Inserts a new user into the database
     * 
     * Creates a new user record in the Users table.
     * The database auto-generates the Id using IDENTITY.
     * 
     * @param user User object containing username and password
     * @return true if user was created successfully, false if failed
     * 
     * SQL: INSERT INTO Users (Username, Password) VALUES (?, ?)
     * 
     * Note: This may fail if the username already exists (UNIQUE constraint)
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO Users (Username, Password) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set the values for the INSERT statement
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            
            // Execute the INSERT and get number of rows affected
            int rowsAffected = pstmt.executeUpdate();
            
            // If 1 row was inserted, return true (success)
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            // Failed - likely due to duplicate username
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * getAllUsers() - Retrieves all users from the database
     * 
     * Fetches every row from the Users table and converts each to a User object.
     * Used by the UserPanel to display the complete list of users.
     * 
     * @return List of all User objects (empty list if no users exist)
     * 
     * SQL: SELECT * FROM Users
     */
    public List<User> getAllUsers() {
        // ArrayList to store all users we find
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // Loop through all rows in the result set
            while (rs.next()) {
                // For each row, create a User object and add it to the list
                User user = new User(
                    rs.getInt("Id"),
                    rs.getString("Username"),
                    rs.getString("Password")
                );
                users.add(user);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Return the list (may be empty if no users exist or an error occurred)
        return users;
    }
    
    /**
     * getUserById() - Retrieves a specific user by their ID
     * 
     * Looks up a single user by their unique ID.
     * Used when editing a user - need to fetch their current data.
     * 
     * @param id The unique ID of the user to find
     * @return User object if found, null if not found
     * 
     * SQL: SELECT * FROM Users WHERE Id = ?
     */
    public User getUserById(int id) {
        String sql = "SELECT * FROM Users WHERE Id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set the ID we're searching for
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            // If a matching row exists, convert it to a User object
            if (rs.next()) {
                return new User(
                    rs.getInt("Id"),
                    rs.getString("Username"),
                    rs.getString("Password")
                );
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // User not found
        return null;
    }
    
    /**
     * updateUser() - Updates an existing user's information
     * 
     * Modifies the username and/or password for an existing user.
     * The user is identified by their ID (which cannot be changed).
     * 
     * @param user User object with updated data (must include valid Id)
     * @return true if user was updated successfully, false if failed
     * 
     * SQL: UPDATE Users SET Username = ?, Password = ? WHERE Id = ?
     * 
     * Note: This may fail if you try to change username to one that already exists
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE Users SET Username = ?, Password = ? WHERE Id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set the new values and specify which user to update
            pstmt.setString(1, user.getUsername());  // New username
            pstmt.setString(2, user.getPassword());  // New password
            pstmt.setInt(3, user.getId());           // Which user (by ID)
            
            // Execute the UPDATE and check if any rows were modified
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * deleteUser() - Removes a user from the database
     * 
     * Permanently deletes a user record by their ID.
     * WARNING: This cannot be undone.
     * 
     * @param id The ID of the user to delete
     * @return true if user was deleted successfully, false if failed or not found
     * 
     * SQL: DELETE FROM Users WHERE Id = ?
     */
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM Users WHERE Id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Specify which user to delete
            pstmt.setInt(1, id);
            
            // Execute the DELETE and check if any rows were removed
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}