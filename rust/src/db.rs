use tiberius::{Config, AuthMethod};
use tokio_util::compat::TokioAsyncWriteCompatExt;
use std::env;
use futures::executor::block_on;

pub async fn get_connection() -> Result<tiberius::Client<tokio_util::compat::Compat<tokio::net::TcpStream>>, Box<dyn std::error::Error>> {
    let server = env::var("DB_SERVER")?;
    let database = env::var("DB_NAME")?;
    let user = env::var("DB_USER")?;
    let password = env::var("DB_PASSWORD")?;

    let mut config = Config::new();
    config.host(&server);
    config.port(1433);
    config.database(&database);
    config.authentication(AuthMethod::sql_server(user, password));

    let tcp = tokio::net::TcpStream::connect(config.get_addr()).await?;
    tcp.set_nodelay(true)?;
    let client = tiberius::Client::connect(config, tcp.compat_write()).await?;
    Ok(client)
}

// Convenience function to block-on for sync-style
pub fn get_connection_blocking() -> tiberius::Client<tokio_util::compat::Compat<tokio::net::TcpStream>> {
    block_on(get_connection()).unwrap()
}
