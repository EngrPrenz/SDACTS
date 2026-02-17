-- Product Management System - Table Setup
-- Since ProductDB already exists, we only need to create the products table
-- Run this script in SQL Server Management Studio 19

USE ProductDB;
GO

-- Create the products table (matching your existing structure)
CREATE TABLE products (
    id INT PRIMARY KEY IDENTITY(1,1),
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2),
    quantity INT
);
GO

-- Insert some sample data for testing (optional)
INSERT INTO products (name, price, quantity) VALUES
('Laptop', 999.99, 10),
('Mouse', 29.99, 50),
('Keyboard', 89.99, 25),
('Monitor', 399.99, 15),
('Headphones', 199.99, 30);
GO

-- Verify the data
SELECT * FROM products;
GO
