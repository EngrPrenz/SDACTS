-- ============================================
-- MSSQL CRUD System - Database Setup Script
-- ============================================
-- This script creates the SampleDB database with Users and Products tables
-- Run this script on your SQL Server instance before using the application

-- ============================================
-- 1. CREATE DATABASE
-- ============================================
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'SampleDB')
BEGIN
    CREATE DATABASE SampleDB;
    PRINT 'Database SampleDB created successfully.';
END
ELSE
BEGIN
    PRINT 'Database SampleDB already exists.';
END
GO

-- ============================================
-- 2. USE DATABASE
-- ============================================
USE SampleDB;
GO

-- ============================================
-- 3. CREATE USERS TABLE
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        Username NVARCHAR(50) UNIQUE NOT NULL,
        Password NVARCHAR(255) NOT NULL,
        CreatedDate DATETIME DEFAULT GETDATE(),
        LastModified DATETIME DEFAULT GETDATE()
    );
    PRINT 'Table Users created successfully.';
END
ELSE
BEGIN
    PRINT 'Table Users already exists.';
END
GO

-- ============================================
-- 4. CREATE PRODUCTS TABLE
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Products')
BEGIN
    CREATE TABLE Products (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        Name NVARCHAR(100) NOT NULL,
        Price DECIMAL(10,2) NOT NULL CHECK (Price >= 0),
        CreatedDate DATETIME DEFAULT GETDATE(),
        LastModified DATETIME DEFAULT GETDATE()
    );
    PRINT 'Table Products created successfully.';
END
ELSE
BEGIN
    PRINT 'Table Products already exists.';
END
GO

-- ============================================
-- 5. INSERT DEFAULT ADMIN USER
-- ============================================
IF NOT EXISTS (SELECT * FROM Users WHERE Username = 'admin')
BEGIN
    INSERT INTO Users (Username, Password) 
    VALUES ('admin', '1234');
    PRINT 'Default admin user created (Username: admin, Password: 1234)';
END
ELSE
BEGIN
    PRINT 'Admin user already exists.';
END
GO

-- ============================================
-- 6. INSERT SAMPLE PRODUCTS (OPTIONAL)
-- ============================================
IF NOT EXISTS (SELECT * FROM Products)
BEGIN
    INSERT INTO Products (Name, Price) VALUES 
        ('Laptop', 999.99),
        ('Wireless Mouse', 29.99),
        ('Mechanical Keyboard', 79.99),
        ('USB-C Cable', 12.99),
        ('Monitor Stand', 45.00),
        ('Webcam HD', 89.99),
        ('Desk Lamp', 34.50),
        ('Phone Charger', 19.99),
        ('Headphones', 149.99),
        ('External SSD 1TB', 129.99);
    PRINT 'Sample products inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Products table already contains data.';
END
GO

-- ============================================
-- 7. VERIFY SETUP
-- ============================================
PRINT '';
PRINT '============================================';
PRINT 'SETUP VERIFICATION';
PRINT '============================================';
PRINT '';

-- Check Users table
DECLARE @UserCount INT;
SELECT @UserCount = COUNT(*) FROM Users;
PRINT 'Users table row count: ' + CAST(@UserCount AS NVARCHAR(10));

-- Check Products table
DECLARE @ProductCount INT;
SELECT @ProductCount = COUNT(*) FROM Products;
PRINT 'Products table row count: ' + CAST(@ProductCount AS NVARCHAR(10));

PRINT '';
PRINT 'Database setup complete!';
PRINT '';
PRINT 'You can now run the application with these credentials:';
PRINT 'Username: admin';
PRINT 'Password: 1234';
PRINT '';
PRINT '============================================';
GO

-- ============================================
-- 8. DISPLAY DATA (OPTIONAL)
-- ============================================
-- Uncomment the following lines to view the data

-- SELECT * FROM Users;
-- SELECT * FROM Products;
-- GO