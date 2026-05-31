package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.CategoryAdapter;
import com.example.finalapp.adapters.ProductAdapter;
import com.example.finalapp.models.Category;
import com.example.finalapp.models.Product;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvProducts, rvCategories;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Product> allProductsList;
    private List<Product> filteredProductsList;
    private List<Category> categoryList;
    private DatabaseReference productsRef, categoriesRef;
    private TextView txtCurrentCategory;
    private EditText edtSearch;
    private String currentCategoryId = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        
        // Mặc định chọn tab Sản phẩm khi vừa vào
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_order); // ID nav_order hiện đang giữ tên "Sản phẩm"
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_about) {
                    startActivity(new Intent(MainActivity.this, AboutActivity.class));
                    return true;
                }
                // Có thể thêm các tab khác ở đây
                return true;
            });
        }

        fetchCategories();
        fetchAllProducts();
    }

    private void initViews() {
        txtCurrentCategory = findViewById(R.id.txtCurrentCategoryName);
        if (txtCurrentCategory != null) txtCurrentCategory.setText("TẤT CẢ");

        edtSearch = findViewById(R.id.edtSearch);
        setupSearch();

        // Setup Categories
        rvCategories = findViewById(R.id.rvCategories);
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            currentCategoryId = category.getId();
            filterProducts(edtSearch.getText().toString());
            if (txtCurrentCategory != null) txtCurrentCategory.setText(category.getName().toUpperCase());
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);

        // Setup Products
        rvProducts = findViewById(R.id.rvProducts);
        allProductsList = new ArrayList<>();
        filteredProductsList = new ArrayList<>();
        productAdapter = new ProductAdapter(filteredProductsList);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setAdapter(productAdapter);

        // Firebase references
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/");
        productsRef = db.getReference("products");
        categoriesRef = db.getReference("categories");
    }

    private void fetchCategories() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                // Thêm mục "Tất cả" nếu muốn
                categoryList.add(new Category("all", "Tất cả"));
                
                Log.d("FirebaseDebug", "========== CATEGORIES DEBUG ==========");
                for (DataSnapshot data : snapshot.getChildren()) {
                    Category category = data.getValue(Category.class);
                    if (category != null) {
                        category.setId(data.getKey()); // Lấy key làm ID
                        categoryList.add(category);
                        Log.d("FirebaseDebug", "Category ID: [" + data.getKey() + "] | Name: " + category.getName());
                    }
                }
                Log.d("FirebaseDebug", "Total categories loaded: " + categoryList.size());
                Log.d("FirebaseDebug", "=====================================");
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Lỗi Categories: " + error.getMessage());
            }
        });
    }

    private void fetchAllProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        product.id = data.getKey(); // Quan trọng: Gán ID từ Key
                        allProductsList.add(product);

                        // Debug logging for first few products
                        if (allProductsList.size() <= 3) {
                            Log.d("FirebaseDebug", "========== PRODUCT DEBUG ==========");
                            Log.d("FirebaseDebug", "Product ID: " + product.id);
                            Log.d("FirebaseDebug", "Product Name: " + product.getName());
                            Log.d("FirebaseDebug", "CategoryId (camelCase): [" + product.categoryId + "]");
                            Log.d("FirebaseDebug", "Category_id (snake_case): [" + product.category_id + "]");
                            Log.d("FirebaseDebug", "getCategoryId() result: [" + product.getCategoryId() + "]");
                            Log.d("FirebaseDebug", "ImageUrl (camelCase): [" + product.imageUrl + "]");
                            Log.d("FirebaseDebug", "Image_url (snake_case): [" + product.image_url + "]");
                            Log.d("FirebaseDebug", "getImageUrl() result: [" + product.getImageUrl() + "]");
                            Log.d("FirebaseDebug", "==================================");
                        }
                    }
                }
                Log.d("FirebaseDebug", "Tổng số sản phẩm tải từ Firebase: " + allProductsList.size());
                filterProducts(""); // Mặc định hiện tất cả
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Lỗi Products: " + error.getMessage());
            }
        });
    }

    private void setupSearch() {
        if (edtSearch == null) return;
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProducts(String query) {
        filteredProductsList.clear();
        String searchQuery = query.toLowerCase().trim();
        String targetCategoryId = (currentCategoryId != null) ? currentCategoryId.trim() : "all";

        Log.d("FirebaseDebug", "--- LỌC SẢN PHẨM ---");
        Log.d("FirebaseDebug", "Đang chọn Category ID: [" + targetCategoryId + "]");
        Log.d("FirebaseDebug", "Tổng số SP trong bộ nhớ: " + allProductsList.size());

        for (Product p : allProductsList) {
            String pCategoryId = (p.getCategoryId() != null) ? p.getCategoryId().trim() : "";
            
            // Log thử vài sản phẩm đầu tiên để xem dữ liệu thật
            if (allProductsList.indexOf(p) < 5) {
                Log.d("FirebaseDebug", "SP: " + p.getName() + " | CategoryID của SP: [" + pCategoryId + "] | Image: " + p.getImageUrl());
            }

            // Fix: Use equalsIgnoreCase for case-insensitive comparison
            boolean matchesCategory = targetCategoryId.equalsIgnoreCase("all") ||
                pCategoryId.equalsIgnoreCase(targetCategoryId);

            boolean matchesSearch = searchQuery.isEmpty() || 
                (p.getName() != null && p.getName().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filteredProductsList.add(p);
                Log.d("FirebaseDebug", "✓ Matching: " + p.getName());
            } else {
                if (!matchesCategory) {
                    Log.d("FirebaseDebug", "✗ Category mismatch: " + p.getName() + " (expect: " + targetCategoryId + ", got: " + pCategoryId + ")");
                }
            }
        }
        
        Log.d("FirebaseDebug", "Kết quả tìm thấy: " + filteredProductsList.size());
        productAdapter.notifyDataSetChanged();
    }

    // Xóa method cũ nếu còn
    private void filterProductsByCategory(String categoryId) {
        currentCategoryId = categoryId;
        filterProducts(edtSearch.getText().toString());
    }
}