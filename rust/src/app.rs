use eframe::egui;
use crate::db_config::DbConfig;
use crate::database::Database;
use crate::models::{User, Product};
use std::sync::Arc;
use tokio::sync::Mutex;

#[derive(PartialEq)]
enum Screen {
    Login,
    Main,
}

#[derive(PartialEq)]
enum Tab {
    Users,
    Products,
}

pub struct App {
    runtime: tokio::runtime::Runtime,
    db: Arc<Mutex<Database>>,
    
    // Login screen
    current_screen: Screen,
    login_username: String,
    login_password: String,
    login_error: String,
    logged_in_user: Option<User>,
    
    // Main screen
    current_tab: Tab,
    
    // Users tab
    users: Vec<User>,
    users_loading: bool,
    users_error: String,
    user_form_username: String,
    user_form_password: String,
    selected_user_id: Option<i32>,
    
    // Products tab
    products: Vec<Product>,
    products_loading: bool,
    products_error: String,
    product_form_name: String,
    product_form_price: String,
    selected_product_id: Option<i32>,
    
    // Database config
    db_server: String,
    db_database: String,
    db_username: String,
    db_password: String,
    show_db_config: bool,
}

impl Default for App {
    fn default() -> Self {
    let runtime = tokio::runtime::Runtime::new().unwrap();
    
    // Database connection settings - UPDATED
    let server = "localhost".to_string();
    let database = "SampleDB".to_string();
    let username = "sa".to_string();
    let password = "admin123".to_string();  // ← Correct password
    
    let config = DbConfig::new(
        server.clone(),
        database.clone(),
        username.clone(),
        password.clone(),
    );
    
    let db = Arc::new(Mutex::new(Database::new(config)));
        
        Self {
            runtime,
            db,
            current_screen: Screen::Login,
            login_username: String::new(),
            login_password: String::new(),
            login_error: String::new(),
            logged_in_user: None,
            current_tab: Tab::Users,
            users: Vec::new(),
            users_loading: false,
            users_error: String::new(),
            user_form_username: String::new(),
            user_form_password: String::new(),
            selected_user_id: None,
            products: Vec::new(),
            products_loading: false,
            products_error: String::new(),
            product_form_name: String::new(),
            product_form_price: String::new(),
            selected_product_id: None,
            db_server: server,
            db_database: database,
            db_username: username,
            db_password: password,
            show_db_config: false,
        }
    }
}

impl App {
    pub fn new(_cc: &eframe::CreationContext<'_>) -> Self {
        Self::default()
    }

    fn handle_login(&mut self) {
        let username = self.login_username.clone();
        let password = self.login_password.clone();
        let db = self.db.clone();
        
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.authenticate_user(&username, &password).await
        });
        
        match result {
            Ok(Some(user)) => {
                self.logged_in_user = Some(user);
                self.current_screen = Screen::Main;
                self.login_error.clear();
                self.load_users();
                self.load_products();
            }
            Ok(None) => {
                self.login_error = "Invalid username or password".to_string();
            }
            Err(e) => {
                self.login_error = format!("Database error: {}", e);
            }
        }
    }

    fn update_db_config(&mut self) {
        let config = DbConfig::new(
            self.db_server.clone(),
            self.db_database.clone(),
            self.db_username.clone(),
            self.db_password.clone(),
        );
        
        self.db = Arc::new(Mutex::new(Database::new(config)));
        self.show_db_config = false;
    }

    fn load_users(&mut self) {
        self.users_loading = true;
        self.users_error.clear();
        
        let db = self.db.clone();
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.get_all_users().await
        });
        
        match result {
            Ok(users) => {
                self.users = users;
                self.users_error.clear();
            }
            Err(e) => {
                self.users_error = format!("Error loading users: {}", e);
            }
        }
        
        self.users_loading = false;
    }

    fn create_user(&mut self) {
        if self.user_form_username.is_empty() || self.user_form_password.is_empty() {
            self.users_error = "Username and password are required".to_string();
            return;
        }
        
        let username = self.user_form_username.clone();
        let password = self.user_form_password.clone();
        let db = self.db.clone();
        
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.create_user(&username, &password).await
        });
        
        match result {
            Ok(_) => {
                self.user_form_username.clear();
                self.user_form_password.clear();
                self.users_error.clear();
                self.load_users();
            }
            Err(e) => {
                self.users_error = format!("Error creating user: {}", e);
            }
        }
    }

    fn update_user(&mut self) {
        if let Some(id) = self.selected_user_id {
            if self.user_form_username.is_empty() || self.user_form_password.is_empty() {
                self.users_error = "Username and password are required".to_string();
                return;
            }
            
            let username = self.user_form_username.clone();
            let password = self.user_form_password.clone();
            let db = self.db.clone();
            
            let result = self.runtime.block_on(async move {
                let db = db.lock().await;
                db.update_user(id, &username, &password).await
            });
            
            match result {
                Ok(_) => {
                    self.user_form_username.clear();
                    self.user_form_password.clear();
                    self.selected_user_id = None;
                    self.users_error.clear();
                    self.load_users();
                }
                Err(e) => {
                    self.users_error = format!("Error updating user: {}", e);
                }
            }
        }
    }

    fn delete_user(&mut self, id: i32) {
        let db = self.db.clone();
        
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.delete_user(id).await
        });
        
        match result {
            Ok(_) => {
                self.users_error.clear();
                self.load_users();
            }
            Err(e) => {
                self.users_error = format!("Error deleting user: {}", e);
            }
        }
    }

    fn load_products(&mut self) {
        self.products_loading = true;
        self.products_error.clear();
        
        let db = self.db.clone();
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.get_all_products().await
        });
        
        match result {
            Ok(products) => {
                self.products = products;
                self.products_error.clear();
            }
            Err(e) => {
                self.products_error = format!("Error loading products: {}", e);
            }
        }
        
        self.products_loading = false;
    }

    fn create_product(&mut self) {
        if self.product_form_name.is_empty() || self.product_form_price.is_empty() {
            self.products_error = "Name and price are required".to_string();
            return;
        }
        
        let price = match self.product_form_price.parse::<f64>() {
            Ok(p) => p,
            Err(_) => {
                self.products_error = "Invalid price format".to_string();
                return;
            }
        };
        
        let name = self.product_form_name.clone();
        let db = self.db.clone();
        
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.create_product(&name, price).await
        });
        
        match result {
            Ok(_) => {
                self.product_form_name.clear();
                self.product_form_price.clear();
                self.products_error.clear();
                self.load_products();
            }
            Err(e) => {
                self.products_error = format!("Error creating product: {}", e);
            }
        }
    }

    fn update_product(&mut self) {
        if let Some(id) = self.selected_product_id {
            if self.product_form_name.is_empty() || self.product_form_price.is_empty() {
                self.products_error = "Name and price are required".to_string();
                return;
            }
            
            let price = match self.product_form_price.parse::<f64>() {
                Ok(p) => p,
                Err(_) => {
                    self.products_error = "Invalid price format".to_string();
                    return;
                }
            };
            
            let name = self.product_form_name.clone();
            let db = self.db.clone();
            
            let result = self.runtime.block_on(async move {
                let db = db.lock().await;
                db.update_product(id, &name, price).await
            });
            
            match result {
                Ok(_) => {
                    self.product_form_name.clear();
                    self.product_form_price.clear();
                    self.selected_product_id = None;
                    self.products_error.clear();
                    self.load_products();
                }
                Err(e) => {
                    self.products_error = format!("Error updating product: {}", e);
                }
            }
        }
    }

    fn delete_product(&mut self, id: i32) {
        let db = self.db.clone();
        
        let result = self.runtime.block_on(async move {
            let db = db.lock().await;
            db.delete_product(id).await
        });
        
        match result {
            Ok(_) => {
                self.products_error.clear();
                self.load_products();
            }
            Err(e) => {
                self.products_error = format!("Error deleting product: {}", e);
            }
        }
    }

    fn render_login(&mut self, ctx: &egui::Context) {
        egui::CentralPanel::default().show(ctx, |ui| {
            ui.vertical_centered(|ui| {
                ui.add_space(100.0);
                
                ui.heading("MSSQL CRUD System");
                ui.add_space(30.0);
                
                ui.horizontal(|ui| {
                    ui.label("Username:");
                    ui.text_edit_singleline(&mut self.login_username);
                });
                
                ui.horizontal(|ui| {
                    ui.label("Password:");
                    let password_edit = egui::TextEdit::singleline(&mut self.login_password)
                        .password(true);
                    ui.add(password_edit);
                });
                
                ui.add_space(10.0);
                
                if ui.button("Login").clicked() || (ui.input(|i| i.key_pressed(egui::Key::Enter))) {
                    self.handle_login();
                }
                
                if !self.login_error.is_empty() {
                    ui.add_space(10.0);
                    ui.colored_label(egui::Color32::RED, &self.login_error);
                }
                
                ui.add_space(20.0);
                
                if ui.button("Database Settings").clicked() {
                    self.show_db_config = true;
                }
            });
        });

        if self.show_db_config {
            egui::Window::new("Database Configuration")
                .collapsible(false)
                .show(ctx, |ui| {
                    ui.horizontal(|ui| {
                        ui.label("Server:");
                        ui.text_edit_singleline(&mut self.db_server);
                    });
                    
                    ui.horizontal(|ui| {
                        ui.label("Database:");
                        ui.text_edit_singleline(&mut self.db_database);
                    });
                    
                    ui.horizontal(|ui| {
                        ui.label("Username:");
                        ui.text_edit_singleline(&mut self.db_username);
                    });
                    
                    ui.horizontal(|ui| {
                        ui.label("Password:");
                        ui.add(egui::TextEdit::singleline(&mut self.db_password).password(true));
                    });
                    
                    ui.add_space(10.0);
                    
                    ui.horizontal(|ui| {
                        if ui.button("Save").clicked() {
                            self.update_db_config();
                        }
                        
                        if ui.button("Cancel").clicked() {
                            self.show_db_config = false;
                        }
                    });
                });
        }
    }

    fn render_main(&mut self, ctx: &egui::Context) {
        egui::TopBottomPanel::top("top_panel").show(ctx, |ui| {
            ui.horizontal(|ui| {
                ui.heading("MSSQL CRUD System");
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if let Some(user) = &self.logged_in_user {
                        ui.label(format!("Logged in as: {}", user.username));
                    }
                    if ui.button("Logout").clicked() {
                        self.current_screen = Screen::Login;
                        self.logged_in_user = None;
                        self.login_username.clear();
                        self.login_password.clear();
                    }
                });
            });
        });

        egui::SidePanel::left("side_panel").show(ctx, |ui| {
            ui.heading("Menu");
            ui.separator();
            
            if ui.selectable_label(self.current_tab == Tab::Users, "Users").clicked() {
                self.current_tab = Tab::Users;
                self.load_users();
            }
            
            if ui.selectable_label(self.current_tab == Tab::Products, "Products").clicked() {
                self.current_tab = Tab::Products;
                self.load_products();
            }
        });

        egui::CentralPanel::default().show(ctx, |ui| {
            match self.current_tab {
                Tab::Users => self.render_users_tab(ui),
                Tab::Products => self.render_products_tab(ui),
            }
        });
    }

    fn render_users_tab(&mut self, ui: &mut egui::Ui) {
        ui.heading("Users Management");
        ui.separator();
        
        // Form section
        ui.horizontal(|ui| {
            ui.label("Username:");
            ui.text_edit_singleline(&mut self.user_form_username);
        });
        
        ui.horizontal(|ui| {
            ui.label("Password:");
            ui.text_edit_singleline(&mut self.user_form_password);
        });
        
        ui.horizontal(|ui| {
            if self.selected_user_id.is_none() {
                if ui.button("Create User").clicked() {
                    self.create_user();
                }
            } else {
                if ui.button("Update User").clicked() {
                    self.update_user();
                }
                if ui.button("Cancel").clicked() {
                    self.selected_user_id = None;
                    self.user_form_username.clear();
                    self.user_form_password.clear();
                }
            }
            
            if ui.button("Refresh").clicked() {
                self.load_users();
            }
        });
        
        if !self.users_error.is_empty() {
            ui.colored_label(egui::Color32::RED, &self.users_error);
        }
        
        ui.separator();
        
        // Table section
        if self.users_loading {
            ui.spinner();
        } else {
            // Collect actions to perform after iteration
            let mut user_to_edit: Option<User> = None;
            let mut user_to_delete: Option<i32> = None;
            
            egui::ScrollArea::vertical().show(ui, |ui| {
                use egui_extras::{Column, TableBuilder};
                
                TableBuilder::new(ui)
                    .striped(true)
                    .cell_layout(egui::Layout::left_to_right(egui::Align::Center))
                    .column(Column::exact(50.0))
                    .column(Column::remainder())
                    .column(Column::remainder())
                    .column(Column::exact(150.0))
                    .header(20.0, |mut header| {
                        header.col(|ui| { ui.heading("ID"); });
                        header.col(|ui| { ui.heading("Username"); });
                        header.col(|ui| { ui.heading("Password"); });
                        header.col(|ui| { ui.heading("Actions"); });
                    })
                    .body(|mut body| {
                        for user in &self.users {
                            body.row(20.0, |mut row| {
                                row.col(|ui| { ui.label(user.id.to_string()); });
                                row.col(|ui| { ui.label(&user.username); });
                                row.col(|ui| { ui.label("********"); });
                                row.col(|ui| {
                                    ui.horizontal(|ui| {
                                        if ui.small_button("Edit").clicked() {
                                            user_to_edit = Some(user.clone());
                                        }
                                        if ui.small_button("Delete").clicked() {
                                            user_to_delete = Some(user.id);
                                        }
                                    });
                                });
                            });
                        }
                    });
            });
            
            // Perform actions after iteration
            if let Some(user) = user_to_edit {
                self.selected_user_id = Some(user.id);
                self.user_form_username = user.username.clone();
                self.user_form_password = user.password.clone();
            }
            
            if let Some(id) = user_to_delete {
                self.delete_user(id);
            }
        }
    }

    fn render_products_tab(&mut self, ui: &mut egui::Ui) {
        ui.heading("Products Management");
        ui.separator();
        
        // Form section
        ui.horizontal(|ui| {
            ui.label("Name:");
            ui.text_edit_singleline(&mut self.product_form_name);
        });
        
        ui.horizontal(|ui| {
            ui.label("Price:");
            ui.text_edit_singleline(&mut self.product_form_price);
        });
        
        ui.horizontal(|ui| {
            if self.selected_product_id.is_none() {
                if ui.button("Create Product").clicked() {
                    self.create_product();
                }
            } else {
                if ui.button("Update Product").clicked() {
                    self.update_product();
                }
                if ui.button("Cancel").clicked() {
                    self.selected_product_id = None;
                    self.product_form_name.clear();
                    self.product_form_price.clear();
                }
            }
            
            if ui.button("Refresh").clicked() {
                self.load_products();
            }
        });
        
        if !self.products_error.is_empty() {
            ui.colored_label(egui::Color32::RED, &self.products_error);
        }
        
        ui.separator();
        
        // Table section
        if self.products_loading {
            ui.spinner();
        } else {
            // Collect actions to perform after iteration
            let mut product_to_edit: Option<Product> = None;
            let mut product_to_delete: Option<i32> = None;
            
            egui::ScrollArea::vertical().show(ui, |ui| {
                use egui_extras::{Column, TableBuilder};
                
                TableBuilder::new(ui)
                    .striped(true)
                    .cell_layout(egui::Layout::left_to_right(egui::Align::Center))
                    .column(Column::exact(50.0))
                    .column(Column::remainder())
                    .column(Column::exact(100.0))
                    .column(Column::exact(150.0))
                    .header(20.0, |mut header| {
                        header.col(|ui| { ui.heading("ID"); });
                        header.col(|ui| { ui.heading("Name"); });
                        header.col(|ui| { ui.heading("Price"); });
                        header.col(|ui| { ui.heading("Actions"); });
                    })
                    .body(|mut body| {
                        for product in &self.products {
                            body.row(20.0, |mut row| {
                                row.col(|ui| { ui.label(product.id.to_string()); });
                                row.col(|ui| { ui.label(&product.name); });
                                row.col(|ui| { ui.label(format!("${:.2}", product.price)); });
                                row.col(|ui| {
                                    ui.horizontal(|ui| {
                                        if ui.small_button("Edit").clicked() {
                                            product_to_edit = Some(product.clone());
                                        }
                                        if ui.small_button("Delete").clicked() {
                                            product_to_delete = Some(product.id);
                                        }
                                    });
                                });
                            });
                        }
                    });
            });
            
            // Perform actions after iteration
            if let Some(product) = product_to_edit {
                self.selected_product_id = Some(product.id);
                self.product_form_name = product.name.clone();
                self.product_form_price = product.price.to_string();
            }
            
            if let Some(id) = product_to_delete {
                self.delete_product(id);
            }
        }
    }
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        match self.current_screen {
            Screen::Login => self.render_login(ctx),
            Screen::Main => self.render_main(ctx),
        }
    }
}