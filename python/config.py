import pyodbc

# ─── CHANGE THESE TO MATCH YOUR SQL SERVER ───────────────────────────────────
SQL_SERVER   = "ACER-NITROV15-F\SQLEXPRESS"          # e.g. ".\SQLEXPRESS" or "192.168.1.10"
SQL_DATABASE = "SampleDB"
SQL_DRIVER   = "{ODBC Driver 17 for SQL Server}"   # or "SQL Server"

# Leave empty strings to use Windows Authentication
SQL_USERNAME = "sa"   # e.g. "sa"
SQL_PASSWORD = "admin123"   # e.g. "YourPassword123"
# ─────────────────────────────────────────────────────────────────────────────

SECRET_KEY = "change-me-in-production-please"


def get_connection():
    if SQL_USERNAME and SQL_PASSWORD:
        conn_str = (
            f"DRIVER={SQL_DRIVER};"
            f"SERVER={SQL_SERVER};"
            f"DATABASE={SQL_DATABASE};"
            f"UID={SQL_USERNAME};"
            f"PWD={SQL_PASSWORD};"
        )
    else:
        conn_str = (
            f"DRIVER={SQL_DRIVER};"
            f"SERVER={SQL_SERVER};"
            f"DATABASE={SQL_DATABASE};"
            "Trusted_Connection=yes;"
        )
    return pyodbc.connect(conn_str)