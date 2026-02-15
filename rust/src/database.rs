use crate::db_config::DbConfig;
use crate::models::{User, Product};
use tiberius::{Client, Query, Row};
use tokio::net::TcpStream;
use tokio_util::compat::Compat;

pub struct Database {
    config: DbConfig,
}

impl Database {
    pub fn new(config: DbConfig) -> Self {
        Self { config }
    }

    async fn get_client(&self) -> Result<Client<Compat<TcpStream>>, Box<dyn std::error::Error + Send + Sync>> {
        self.config.create_client().await
    }

    // User operations
    pub async fn authenticate_user(&self, username: &str, password: &str) -> Result<Option<User>, Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("SELECT Id, Username, Password FROM Users WHERE Username = @P1 AND Password = @P2");
        query.bind(username);
        query.bind(password);
        
        let stream = query.query(&mut client).await?;
        let rows: Vec<Row> = stream.into_first_result().await?;
        
        if let Some(row) = rows.first() {
            let id: i32 = row.get(0).unwrap_or(0);
            let username: &str = row.get(1).unwrap_or("");
            let password: &str = row.get(2).unwrap_or("");
            
            Ok(Some(User::new(id, username.to_string(), password.to_string())))
        } else {
            Ok(None)
        }
    }

    pub async fn get_all_users(&self) -> Result<Vec<User>, Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let query = Query::new("SELECT Id, Username, Password FROM Users ORDER BY Id");
        let stream = query.query(&mut client).await?;
        let rows: Vec<Row> = stream.into_first_result().await?;
        
        let mut users = Vec::new();
        for row in rows {
            let id: i32 = row.get(0).unwrap_or(0);
            let username: &str = row.get(1).unwrap_or("");
            let password: &str = row.get(2).unwrap_or("");
            
            users.push(User::new(id, username.to_string(), password.to_string()));
        }
        
        Ok(users)
    }

    pub async fn create_user(&self, username: &str, password: &str) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("INSERT INTO Users (Username, Password) VALUES (@P1, @P2)");
        query.bind(username);
        query.bind(password);
        query.execute(&mut client).await?;
        
        Ok(())
    }

    pub async fn update_user(&self, id: i32, username: &str, password: &str) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("UPDATE Users SET Username = @P1, Password = @P2 WHERE Id = @P3");
        query.bind(username);
        query.bind(password);
        query.bind(id);
        query.execute(&mut client).await?;
        
        Ok(())
    }

    pub async fn delete_user(&self, id: i32) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("DELETE FROM Users WHERE Id = @P1");
        query.bind(id);
        query.execute(&mut client).await?;
        
        Ok(())
    }

    // Product operations
    pub async fn get_all_products(&self) -> Result<Vec<Product>, Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let query = Query::new("SELECT Id, Name, Price FROM Products ORDER BY Id");
        let stream = query.query(&mut client).await?;
        let rows: Vec<Row> = stream.into_first_result().await?;
        
        let mut products = Vec::new();
        for row in rows {
            let id: i32 = row.get(0).unwrap_or(0);
            let name: &str = row.get(1).unwrap_or("");
            
            // Handle DECIMAL conversion properly
            let price: f64 = match row.try_get::<f64, _>(2) {
                Ok(Some(p)) => p,
                Ok(None) => 0.0,
                Err(_) => {
                    // Try getting as Numeric and convert
                    match row.try_get::<tiberius::numeric::Numeric, _>(2) {
                        Ok(Some(numeric)) => {
                            // Convert Numeric i128 to f64
                            let value = numeric.value();
                            let scale = numeric.scale();
                            let divisor = 10_f64.powi(scale as i32);
                            (value as f64) / divisor
                        },
                        Ok(None) => 0.0,
                        Err(_) => 0.0,
                    }
                }
            };
            
            products.push(Product::new(id, name.to_string(), price));
        }
        
        Ok(products)
    }

    pub async fn create_product(&self, name: &str, price: f64) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("INSERT INTO Products (Name, Price) VALUES (@P1, @P2)");
        query.bind(name);
        query.bind(price);
        query.execute(&mut client).await?;
        
        Ok(())
    }

    pub async fn update_product(&self, id: i32, name: &str, price: f64) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("UPDATE Products SET Name = @P1, Price = @P2 WHERE Id = @P3");
        query.bind(name);
        query.bind(price);
        query.bind(id);
        query.execute(&mut client).await?;
        
        Ok(())
    }

    pub async fn delete_product(&self, id: i32) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut client = self.get_client().await?;
        
        let mut query = Query::new("DELETE FROM Products WHERE Id = @P1");
        query.bind(id);
        query.execute(&mut client).await?;
        
        Ok(())
    }
}