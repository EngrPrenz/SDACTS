package com.example.java_sql;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseHelper {

    public static Connection connectionclass() {

        Connection con = null;

        String ip = "192.168.100.143";
        String port = "1433";
        String instance = "SQLEXPRESS";
        String username = "sa";
        String password = "admin123";
        String databasename = "SampleDB";  // <-- updated

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");

            String connectionUrl =
                    "jdbc:jtds:sqlserver://" + ip + ":" + port +
                            ";instance=" + instance +
                            ";databasename=" + databasename +
                            ";user=" + username +
                            ";password=" + password + ";";

            con = DriverManager.getConnection(connectionUrl);

        } catch (Exception exception) {
            Log.e("DB_ERROR", "Connection Error: " + exception.getMessage());
        }

        return con;
    }
}