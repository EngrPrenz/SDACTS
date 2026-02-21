# app.rb
# ------------------------------------------------------------
# Ruby Web GUI using Sinatra + Microsoft SQL Server
#
# Features:
#  - Login system (Users table + bcrypt password hashing)
#  - Product management (CRUD: Add/Edit/Delete)
#  - Search products by name
#
# Database:
#  - MultiLang_Ruby_DB
#
# NOTE:
#  - Uses SQL Authentication (sa)
#  - Connects through TCP at 127.0.0.1:1433
# ------------------------------------------------------------

require "sinatra"                         # Web framework
require "sinatra/reloader" if development? # Auto reload changes in development
require "sequel"                          # DB toolkit / query helper
require "tiny_tds"                        # SQL Server driver
require "bcrypt"                          # Secure password hashing

# -------------------------
# Session setup
# -------------------------
# Sessions allow us to remember if a user is logged in.
# We store the logged-in user's id in session[:user_id]
enable :sessions

# Sinatra/Rack requires a session secret >= 64 chars for encryption
# You can generate your own (example):
# ruby -e "require 'securerandom'; puts SecureRandom.hex(64)"
set :session_secret, "a9f1c3e7b2d64c8e9a1f3c7e2b6d4c8e9a1f3c7e2b6d4c8e9a1f3c7e2b6d4c8e"

# Ensure Sinatra loads views from ./views
set :views, File.join(File.dirname(__FILE__), "views")

# -------------------------
# Database configuration (EDIT THESE)
# -------------------------
DB_NAME = "MultiLang_Ruby_DB"          # DB name in SSMS
DB_USER = "sa"                         # SQL login username
DB_PASS = "admin123"  # <-- CHANGE THIS to your sa password

# Create a DB connection using Sequel + TinyTDS
DB = Sequel.connect(
  adapter: "tinytds",
  host: "127.0.0.1",      # local machine
  port: 1433,             # SQL TCP port (we set this in SQL Server config)
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASS,
  tds_version: "7.4"
)

# -------------------------
# Helper methods
# -------------------------
helpers do
  # Returns true if a session user_id exists
  def logged_in?
    !!session[:user_id]
  end

  # Redirect to login if user isn't logged in
  def require_login
    redirect "/login" unless logged_in?
  end
end

# ------------------------------------------------------------
# ONE-TIME SETUP ROUTE
# ------------------------------------------------------------
# Visit /setup once to create an admin account:
#   username: admin
#   password: admin123
#
# After you create the admin user, you can remove this route
# (for security) OR keep it if teacher allows.
get "/setup" do
  existing = DB[:users].where(username: "admin").first
  return "Admin already exists. Go to /login" if existing

  hash = BCrypt::Password.create("admin123")
  DB[:users].insert(username: "admin", passwordhash: hash)

  "Created admin user. Username: admin Password: admin123"
end

# ------------------------------------------------------------
# AUTH ROUTES
# ------------------------------------------------------------

# Show the login page (form)
get "/login" do
  erb :login
end

# Handle login form submission
post "/login" do
  # Find the user by username
  user = DB[:users].where(username: params[:username]).first

  # Verify password using bcrypt hash
  if user && BCrypt::Password.new(user[:passwordhash]) == params[:password]
    # Store user id in session to mark them as logged in
    session[:user_id] = user[:id]
    redirect "/products"
  else
    @error = "Invalid username or password"
    erb :login
  end
end

# Logout clears session and goes back to login page
get "/logout" do
  session.clear
  redirect "/login"
end

# Redirect home to products page
get "/" do
  redirect "/products"
end

# ------------------------------------------------------------
# PRODUCTS: LIST + SEARCH
# ------------------------------------------------------------
get "/products" do
  require_login

  # q is the search text from URL: /products?q=abc
  q = params[:q].to_s.strip

  # Base query: get all products sorted newest first
  ds = DB[:products].order(Sequel.desc(:id))

  # If user typed a search, filter by product name (case-insensitive)
  if q != ""
    ds = ds.where(Sequel.ilike(:name, "%#{q}%"))
  end

  @q = q
  @products = ds.all
  erb :products
end

# ------------------------------------------------------------
# PRODUCTS: CREATE
# ------------------------------------------------------------
# Show "Add Product" form
get "/products/new" do
  require_login
  erb :new_product
end

# Insert the new product into DB
post "/products" do
  require_login

  name  = params[:name].to_s.strip
  price = params[:price].to_f
  qty   = params[:qty].to_i

  # Simple validation
  if name == ""
    @error = "Name is required"
    return erb :new_product
  end

  DB[:products].insert(
    name: name,
    price: price,
    qty: qty,
    createdat: Sequel.function(:SYSDATETIME),
    updatedat: nil
  )

  redirect "/products"
end

# ------------------------------------------------------------
# PRODUCTS: UPDATE
# ------------------------------------------------------------
# Show edit form for one product
get "/products/:id/edit" do
  require_login

  @product = DB[:products].where(id: params[:id].to_i).first
  halt 404, "Not found" unless @product

  erb :edit_product
end

# Update product in DB
post "/products/:id" do
  require_login

  id = params[:id].to_i
  product = DB[:products].where(id: id).first
  halt 404, "Not found" unless product

  name  = params[:name].to_s.strip
  price = params[:price].to_f
  qty   = params[:qty].to_i

  if name == ""
    @error = "Name is required"
    @product = product
    return erb :edit_product
  end

  DB[:products].where(id: id).update(
    name: name,
    price: price,
    qty: qty,
    updatedat: Sequel.function(:SYSDATETIME)
  )

  redirect "/products"
end

# ------------------------------------------------------------
# PRODUCTS: DELETE
# ------------------------------------------------------------
post "/products/:id/delete" do
  require_login
  DB[:products].where(id: params[:id].to_i).delete
  redirect "/products"
end
