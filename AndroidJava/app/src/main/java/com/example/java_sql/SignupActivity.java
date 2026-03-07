package com.example.java_sql;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    EditText username, password;
    Button signupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        username = findViewById(R.id.signupUsername);
        password = findViewById(R.id.signupPassword);
        signupBtn = findViewById(R.id.signupBtn);

        signupBtn.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String user = username.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {

            Connection connection = DatabaseHelper.connectionclass();

            if (connection != null) {
                try {
                    // Inserts into the Users table from SampleDB
                    String sql = "INSERT INTO Users (Username, Password) VALUES (?, ?)";
                    PreparedStatement st = connection.prepareStatement(sql);
                    st.setString(1, user);
                    st.setString(2, pass);

                    int result = st.executeUpdate();

                    st.close();
                    connection.close();

                    runOnUiThread(() -> {
                        if (result > 0) {
                            Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error: Username may already exist", Toast.LENGTH_SHORT).show()
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