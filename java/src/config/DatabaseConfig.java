package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String URL = "jdbc:sqlserver://ACER-NITROV15-F\\SQLEXPRESS;databaseName=SampleDB;encrypt=true;trustServerCertificate=true";
    private static final String USERNAME = "sa"; // Change this to your SQL Server username
    private static final String PASSWORD = "admin123"; // Change this to your SQL Server password
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC Driver not found.", e);
        } catch (SQLException e) {
            System.err.println("========================================");
            System.err.println("DATABASE CONNECTION FAILED!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("========================================");
            throw e;
        }
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Database connection successful!");
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    testConnection();
    }
    
}