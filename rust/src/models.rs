use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: i32,
    pub username: String,
    pub password: String,
}

impl User {
    pub fn new(id: i32, username: String, password: String) -> Self {
        Self {
            id,
            username,
            password,
        }
    }

    pub fn empty() -> Self {
        Self {
            id: 0,
            username: String::new(),
            password: String::new(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Product {
    pub id: i32,
    pub name: String,
    pub price: f64,
}

impl Product {
    pub fn new(id: i32, name: String, price: f64) -> Self {
        Self {
            id,
            name,
            price,
        }
    }

    pub fn empty() -> Self {
        Self {
            id: 0,
            name: String::new(),
            price: 0.0,
        }
    }
}