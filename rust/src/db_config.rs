use tiberius::{Client, Config, AuthMethod};
use tokio::net::TcpStream;
use tokio_util::compat::{Compat, TokioAsyncWriteCompatExt};

pub struct DbConfig {
    pub server: String,
    pub database: String,
    pub username: String,
    pub password: String,
}

impl DbConfig {
    pub fn new(server: String, database: String, username: String, password: String) -> Self {
        Self {
            server,
            database,
            username,
            password,
        }
    }

    pub async fn create_client(&self) -> Result<Client<Compat<TcpStream>>, Box<dyn std::error::Error + Send + Sync>> {
        let mut config = Config::new();
        
        config.host(&self.server);
        config.database(&self.database);
        config.authentication(AuthMethod::sql_server(&self.username, &self.password));
        
        config.trust_cert();

        let tcp = TcpStream::connect(config.get_addr()).await?;
        tcp.set_nodelay(true)?;

        let client = Client::connect(config, tcp.compat_write()).await?;
        Ok(client)
    }
}