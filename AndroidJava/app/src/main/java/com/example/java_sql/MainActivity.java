package com.example.java_sql;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    RecyclerView recyclerProducts;
    ArrayList<ProductModel> productList = new ArrayList<>();
    ProductAdapter adapter;

    EditText nameEdit, priceEdit, searchEdit;
    Button addBtn, updateBtn, deleteBtn, searchBtn, logoutBtn, clearBtn;

    String selectedProductId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEdit   = findViewById(R.id.editTextname);
        priceEdit  = findViewById(R.id.editTextemail);   // reusing same view ID, now for Price
        searchEdit = findViewById(R.id.editTextsearch);
        addBtn     = findViewById(R.id.addbtn);
        updateBtn  = findViewById(R.id.updatebtn);
        deleteBtn  = findViewById(R.id.deletebtn);
        searchBtn  = findViewById(R.id.searchbtn);
        logoutBtn  = findViewById(R.id.logoutbtn);
        clearBtn   = findViewById(R.id.clearbtn);

        recyclerProducts = findViewById(R.id.recyclerUsers);  // same view ID
        recyclerProducts.setLayoutManager(new LinearLayoutManager(this));

        loadProducts();

        // ---------- ADD ----------
        addBtn.setOnClickListener(v -> {
            String nameText  = nameEdit.getText().toString().trim();
            String priceText = priceEdit.getText().toString().trim();

            if (nameText.isEmpty() || priceText.isEmpty()) {
                Toast.makeText(this, "Name and Price are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPrice(priceText)) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();

            executorService.execute(() -> {
                Connection connection = DatabaseHelper.connectionclass();
                if (connection != null) {
                    try {
                        String sql = "INSERT INTO Products (Name, Price) VALUES (?, ?)";
                        PreparedStatement st = connection.prepareStatement(sql);
                        st.setString(1, nameText);
                        st.setBigDecimal(2, new java.math.BigDecimal(priceText));

                        int result = st.executeUpdate();
                        st.close();
                        connection.close();

                        runOnUiThread(() -> {
                            if (result > 0) {
                                Toast.makeText(this, "Product Added!", Toast.LENGTH_SHORT).show();
                                clearInputs();
                                loadProducts();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("DB_ERROR", "Insert failed: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(this, "Add Failed", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Connection Failed!", Toast.LENGTH_SHORT).show());
                }
            });
        });

        // ---------- UPDATE ----------
        updateBtn.setOnClickListener(v -> {
            if (selectedProductId == null) {
                Toast.makeText(this, "Select a product from the list to update", Toast.LENGTH_SHORT).show();
                return;
            }

            String nameText  = nameEdit.getText().toString().trim();
            String priceText = priceEdit.getText().toString().trim();

            if (nameText.isEmpty() || priceText.isEmpty()) {
                Toast.makeText(this, "Name and Price are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPrice(priceText)) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show();
                return;
            }

            executorService.execute(() -> {
                Connection connection = DatabaseHelper.connectionclass();
                if (connection != null) {
                    try {
                        String sql = "UPDATE Products SET Name = ?, Price = ? WHERE Id = ?";
                        PreparedStatement st = connection.prepareStatement(sql);
                        st.setString(1, nameText);
                        st.setBigDecimal(2, new java.math.BigDecimal(priceText));
                        st.setString(3, selectedProductId);

                        int result = st.executeUpdate();
                        st.close();
                        connection.close();

                        runOnUiThread(() -> {
                            if (result > 0) {
                                Toast.makeText(this, "Product Updated!", Toast.LENGTH_SHORT).show();
                                clearInputs();
                                loadProducts();
                            } else {
                                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("DB_ERROR", "Update failed: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });

        // ---------- DELETE ----------
        deleteBtn.setOnClickListener(v -> {
            if (selectedProductId == null) {
                Toast.makeText(this, "Select a product from the list to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes", (dialog, which) -> executorService.execute(() -> {
                        Connection connection = DatabaseHelper.connectionclass();
                        if (connection != null) {
                            try {
                                String sql = "DELETE FROM Products WHERE Id = ?";
                                PreparedStatement st = connection.prepareStatement(sql);
                                st.setString(1, selectedProductId);

                                int result = st.executeUpdate();
                                st.close();
                                connection.close();

                                runOnUiThread(() -> {
                                    if (result > 0) {
                                        Toast.makeText(this, "Product Deleted", Toast.LENGTH_SHORT).show();
                                        clearInputs();
                                        loadProducts();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("DB_ERROR", e.getMessage());
                            }
                        }
                    }))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // ---------- SEARCH ----------
        searchBtn.setOnClickListener(v -> {
            String searchName = searchEdit.getText().toString().trim();
            loadProducts(searchName);
        });

        // ---------- CLEAR ----------
        clearBtn.setOnClickListener(v -> {
            clearInputs();
            searchEdit.setText("");
            loadProducts();
        });

        // ---------- LOGOUT ----------
        logoutBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Logged Out Successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void clearInputs() {
        selectedProductId = null;
        nameEdit.setText("");
        priceEdit.setText("");
    }

    private boolean isValidPrice(String value) {
        try {
            new java.math.BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void loadProducts() {
        loadProducts(searchEdit.getText().toString().trim());
    }

    private void loadProducts(String searchName) {
        executorService.execute(() -> {
            Connection connection = DatabaseHelper.connectionclass();

            if (connection != null) {
                try {
                    String sql;
                    PreparedStatement st;

                    if (!searchName.isEmpty()) {
                        sql = "SELECT Id, Name, Price FROM Products WHERE Name LIKE ?";
                        st = connection.prepareStatement(sql);
                        st.setString(1, "%" + searchName + "%");
                    } else {
                        sql = "SELECT Id, Name, Price FROM Products";
                        st = connection.prepareStatement(sql);
                    }

                    ResultSet rs = st.executeQuery();
                    productList.clear();

                    while (rs.next()) {
                        productList.add(new ProductModel(
                                rs.getString("Id"),
                                rs.getString("Name"),
                                rs.getString("Price")
                        ));
                    }

                    rs.close();
                    st.close();
                    connection.close();

                    runOnUiThread(() -> {
                        adapter = new ProductAdapter(productList, product -> {
                            selectedProductId = product.id;
                            nameEdit.setText(product.name);
                            priceEdit.setText(product.price);
                        });
                        recyclerProducts.setAdapter(adapter);
                    });

                } catch (Exception e) {
                    Log.e("DB_ERROR", "Load Error: " + e.getMessage());
                }
            }
        });
    }
}