use eframe::{egui, epi};
use dotenv::dotenv;
use std::env;

mod db;
mod auth;
mod products;

struct App {
    logged_in: bool,
    username: String,
    password: String,
    search: String,
    products: Vec<products::Product>,
    displayed_products: Vec<products::Product>,
    new_name: String,
    new_price: String,
    selected_index: Option<usize>,
}

impl Default for App {
    fn default() -> Self {
        dotenv().ok();
        let all_products = products::get_products();
        Self {
            logged_in: false,
            username: "".to_string(),
            password: "".to_string(),
            search: "".to_string(),
            products: all_products.clone(),
            displayed_products: all_products,
            new_name: "".to_string(),
            new_price: "".to_string(),
            selected_index: None,
        }
    }
}

impl epi::App for App {
    fn name(&self) -> &str { "Rust Products GUI" }

    fn update(&mut self, ctx: &egui::Context, _: &epi::Frame) {
        egui::CentralPanel::default().show(ctx, |ui| {
            if !self.logged_in {
                ui.heading("Login");
                ui.horizontal(|ui| {
                    ui.label("Username:");
                    ui.text_edit_singleline(&mut self.username);
                });
                ui.horizontal(|ui| {
                    ui.label("Password:");
                    ui.text_edit_singleline(&mut self.password);
                });
                if ui.button("Login").clicked() {
                    if auth::login(&self.username, &self.password) {
                        self.logged_in = true;
                        self.products = products::get_products();
                        self.displayed_products = self.products.clone();
                    } else {
                        ui.label("Login failed!");
                    }
                }
            } else {
                ui.heading("Products");
                ui.horizontal(|ui| {
                    ui.label("Search:");
                    if ui.text_edit_singleline(&mut self.search).changed() {
                        self.displayed_products = self.products
                            .iter()
                            .filter(|p| p.name.contains(&self.search))
                            .cloned()
                            .collect();
                    }
                });

                egui::ScrollArea::vertical().show(ui, |ui| {
                    for (i, product) in self.displayed_products.iter().enumerate() {
                        ui.horizontal(|ui| {
                            if ui.selectable_label(Some(i) == self.selected_index, &product.name).clicked() {
                                self.selected_index = Some(i);
                                self.new_name = product.name.clone();
                                self.new_price = product.price.to_string();
                            }
                            ui.label(format!("${}", product.price));
                        });
                    }
                });

                ui.horizontal(|ui| {
                    ui.text_edit_singleline(&mut self.new_name);
                    ui.text_edit_singleline(&mut self.new_price);
                    if ui.button("Add").clicked() {
                        if let Ok(price) = self.new_price.parse() {
                            products::add_product(&self.new_name, price);
                            self.products = products::get_products();
                            self.displayed_products = self.products.clone();
                        }
                    }
                    if ui.button("Edit").clicked() {
                        if let (Some(idx), Ok(price)) = (self.selected_index, self.new_price.parse::<f64>()) {
                            let id = self.displayed_products[idx].id;
                            products::edit_product(id, &self.new_name, price);
                            self.products = products::get_products();
                            self.displayed_products = self.products.clone();
                        }
                    }
                    if ui.button("Delete").clicked() {
                        if let Some(idx) = self.selected_index {
                            let id = self.displayed_products[idx].id;
                            products::delete_product(id);
                            self.products = products::get_products();
                            self.displayed_products = self.products.clone();
                        }
                    }
                    if ui.button("Clear").clicked() {
                        self.new_name.clear();
                        self.new_price.clear();
                        self.selected_index = None;
                    }
                });
            }
        });
    }
}

fn main() {
    let app = App::default();
    let native_options = eframe::NativeOptions::default();
    eframe::run_native(Box::new(app), native_options);
}
