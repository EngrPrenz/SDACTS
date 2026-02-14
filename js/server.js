const express = require("express");
const sql = require("mssql/msnodesqlv8");
const path = require("path");

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

const config = {
    server: "localhost\\SQLEXPRESS",
    database: "SampleDB",
    options: { trustedConnection: true }
};

let pool;

async function startServer() {
    try {
        pool = await sql.connect(config);
        console.log("✅ Connected to SQL Server");

        // Serve login page
        app.get("/", (req, res) => {
            res.sendFile(path.join(__dirname, "public", "login.html"));
        });

        // Login endpoint
        app.post("/login", async (req, res) => {
            const { username, password } = req.body;
            try {
                const result = await pool.request()
                    .input("username", sql.NVarChar, username)
                    .input("password", sql.NVarChar, password)
                    .query("SELECT * FROM Users WHERE Username=@username AND Password=@password");

                res.json({ success: result.recordset.length > 0 });
            } catch (err) {
                res.status(500).send(err.message);
            }
        });

        // CRUD endpoints
        app.get("/products", async (req, res) => {
            try {
                const result = await pool.request().query("SELECT * FROM Products");
                res.json(result.recordset);
            } catch (err) {
                res.status(500).send(err.message);
            }
        });

        app.post("/add", async (req, res) => {
            const { name, price } = req.body;
            try {
                await pool.request()
                    .input("name", sql.NVarChar, name)
                    .input("price", sql.Decimal(18,2), price)
                    .query("INSERT INTO Products (Name, Price) VALUES (@name, @price)");
                res.json({ success: true });
            } catch (err) {
                res.status(500).send(err.message);
            }
        });

        app.put("/update/:id", async (req, res) => {
            const { id } = req.params;
            const { name, price } = req.body;
            try {
                await pool.request()
                    .input("id", sql.Int, id)
                    .input("name", sql.NVarChar, name)
                    .input("price", sql.Decimal(18,2), price)
                    .query("UPDATE Products SET Name=@name, Price=@price WHERE Id=@id");
                res.json({ success: true });
            } catch (err) {
                res.status(500).send(err.message);
            }
        });

        app.delete("/delete/:id", async (req, res) => {
            const { id } = req.params;
            try {
                await pool.request()
                    .input("id", sql.Int, id)
                    .query("DELETE FROM Products WHERE Id=@id");
                res.json({ success: true });
            } catch (err) {
                res.status(500).send(err.message);
            }
        });

        app.listen(3000, () => console.log("🚀 Server running at http://localhost:3000"));

    } catch (err) {
        console.error("❌ Database connection failed:", err);
    }
}

startServer();
