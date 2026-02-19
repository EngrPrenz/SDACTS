package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConfig - Centralized database connection configuration
 * 
 * This class manages all database connection settings and provides
 * a static method to obtain database connections throughout the application.
 */
public class DatabaseConfig {

    // ================================================================
    // DATABASE CONNECTION SETTINGS
    // Update these three values to match your SQL Server setup
    // ================================================================
    
    /**
     * JDBC connection URL - specifies the database server location and name
     * Format: jdbc:sqlserver://SERVER_NAME;databaseName=DATABASE_NAME;options
     * 
     * Example values:
     * - "jdbc:sqlserver://localhost:1433;databaseName=SampleDB;encrypt=true;trustServerCertificate=true"
     *   → For default SQL Server instance on local machine
     * 
     * - "jdbc:sqlserver://COMPUTERNAME\\SQLEXPRESS;databaseName=SampleDB;encrypt=true;trustServerCertificate=true"
     *   → For SQL Server Express named instance (note the double backslash \\)
     * 
     * - "jdbc:sqlserver://192.168.1.100:1433;databaseName=SampleDB;encrypt=true;trustServerCertificate=true"
     *   → For remote SQL Server
     */
    private static final String URL = "jdbc:sqlserver://ACER-NITROV15-F\\SQLEXPRESS;databaseName=SampleDB;encrypt=true;trustServerCertificate=true";
    
    /**
     * SQL Server login username
     * Common values: "sa" (system administrator) or your custom SQL login
     */
    private static final String USERNAME = "sa";
    
    /**
     * SQL Server login password
     * SECURITY NOTE: In production, use environment variables or secure config files instead of hardcoding
     */
    private static final String PASSWORD = "YourPassword"; // TODO: Replace with your actual password

    /**
     * getConnection() - Creates and returns a new database connection
     * 
     * This method is called by DAO classes whenever they need to interact with the database.
     * Each DAO operation (create, read, update, delete) opens a new connection and closes it
     * when done using try-with-resources.
     * 
     * @return Connection object connected to the SampleDB database
     * @throws SQLException if connection fails (wrong credentials, server not running, etc.)
     * 
     * How it works:
     * 1. Loads the SQL Server JDBC driver class into memory
     * 2. Uses DriverManager to establish a connection using URL, USERNAME, and PASSWORD
     * 3. Returns the active connection for the caller to use
     * 4. If anything goes wrong, throws SQLException with details
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Step 1: Load the Microsoft SQL Server JDBC driver
            // This line ensures the driver is available before attempting connection
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            // Step 2: Create and return a connection using our configured settings
            // DriverManager uses the URL to locate the server and credentials to authenticate
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
            
        } catch (ClassNotFoundException e) {
            // This happens if the mssql-jdbc JAR is not in the classpath
            // Maven should handle this automatically - if you see this error, run: mvn clean install
            throw new SQLException("SQL Server JDBC Driver not found. Make sure mssql-jdbc jar is in your classpath.", e);
            
        } catch (SQLException e) {
            // Connection failed - print helpful diagnostic information to console
            System.err.println("========================================");
            System.err.println("DATABASE CONNECTION FAILED!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("----------------------------------------");
            System.err.println("Checklist:");
            System.err.println("1. Is SQL Server running?");
            System.err.println("   -> Open Services and check 'SQL Server (SQLEXPRESS)'");
            System.err.println("2. Is the instance name correct?");
            System.err.println("   -> Run in PowerShell: Get-Service -Name MSSQL*");
            System.err.println("3. Is SQL Server Browser running?");
            System.err.println("   -> Open Services and start 'SQL Server Browser'");
            System.err.println("4. Are your credentials correct?");
            System.err.println("   -> Check USERNAME and PASSWORD in DatabaseConfig.java");
            System.err.println("5. Is TCP/IP enabled?");
            System.err.println("   -> Open SQL Server Configuration Manager");
            System.err.println("   -> SQL Server Network Configuration -> Protocols");
            System.err.println("   -> Enable TCP/IP");
            System.err.println("========================================");
            
            // Re-throw the exception so the caller knows connection failed
            throw e;
        }
    }
}