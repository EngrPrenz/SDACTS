package com.example.java_sql;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    EditText username, password;
    Button loginBtn, goSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.loginUsername);
        password = findViewById(R.id.loginPassword);
        loginBtn = findViewById(R.id.loginBtn);
        goSignup = findViewById(R.id.goSignup);

        loginBtn.setOnClickListener(v -> loginUser());

        goSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }

    private void loginUser() {

        String user = username.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {

            Connection connection = DatabaseHelper.connectionclass();

            if (connection != null) {
                try {
                    // Uses the Users table from SampleDB
                    String sql = "SELECT * FROM Users WHERE Username = ? AND Password = ?";
                    PreparedStatement st = connection.prepareStatement(sql);
                    st.setString(1, user);
                    st.setString(2, pass);

                    ResultSet rs = st.executeQuery();

                    if (rs.next()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
                        );
                    }

                    rs.close();
                    st.close();
                    connection.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show()
                    );
                }
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Connection Failed!", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}