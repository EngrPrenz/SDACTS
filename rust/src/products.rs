use crate::db;
use tiberius::Row;

#[derive(Clone)]
pub struct Product {
    pub id: i32,
    pub name: String,
    pub price: f64,
}

pub fn get_products() -> Vec<Product> {
    let mut client = db::get_connection_blocking();
    let mut products = Vec::new();

    let rows = client.query("SELECT Id, Name, Price FROM Products", &[]).wait().unwrap();
    for row in rows {
        let r = row.unwrap();
        products.push(Product {
            id: r.get("Id").unwrap(),
            name: r.get("Name").unwrap(),
            price: r.get("Price").unwrap(),
        });
    }
    products
}

pub fn add_product(name: &str, price: f64) {
    let mut client = db::get_connection_blocking();
    let query = format!("INSERT INTO Products (Name, Price) VALUES ('{}', {})", name, price);
    client.simple_query(query).wait().unwrap();
}

pub fn edit_product(id: i32, name: &str, price: f64) {
    let mut client = db::get_connection_blocking();
    let query = format!("UPDATE Products SET Name='{}', Price={} WHERE Id={}", name, price, id);
    client.simple_query(query).wait().unwrap();
}

pub fn delete_product(id: i32) {
    let mut client = db::get_connection_blocking();
    let query = format!("DELETE FROM Products WHERE Id={}", id);
    client.simple_query(query).wait().unwrap();
}
