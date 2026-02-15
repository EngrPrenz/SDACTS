mod app;
mod db_config;
mod database;
mod models;

use app::App;

fn main() -> Result<(), eframe::Error> {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([1024.0, 768.0])
            .with_min_inner_size([800.0, 600.0]),
        ..Default::default()
    };
    
    eframe::run_native(
        "MSSQL CRUD System",
        options,
        Box::new(|cc| Ok(Box::new(App::new(cc)))),
    )
}