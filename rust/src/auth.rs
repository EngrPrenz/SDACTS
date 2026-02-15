use crate::db;
use tiberius::Row;

pub fn login(username: &str, password: &str) -> bool {
    let mut client = db::get_connection_blocking();

    let query = format!(
        "SELECT COUNT(*) FROM Users WHERE Username = '{}' AND Password = '{}'",
        username, password
    );

    let row: Row = client.query(query, &[]).wait().unwrap().into_iter().next().unwrap();
    let count: i64 = row.get(0).unwrap();
    count > 0
}
