package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.ProductAdapter;
import com.example.finalapp.models.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.finalapp.models.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import android.widget.Toast;
import java.util.List;

public class MainFinalActivity extends AppCompatActivity {

    private RecyclerView rvBestSellers, rvNewArrivals;
    private ProductAdapter bestSellersAdapter, newArrivalsAdapter;
    private List<Product> bestSellersList, newArrivalsList;
    private DatabaseReference productsRef;
    private View btnProfile, btnCartTab, btnNotification, btnShop, btnHome, btnCommunity;
    private FloatingActionButton fabCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_final);

        initViews();
        setupFirebase();
        fetchProducts();
    }

    private void initViews() {
        btnProfile = findViewById(R.id.btnProfile);
        btnCartTab = findViewById(R.id.layoutBottomCart);
        btnShop = findViewById(R.id.btnShop);
        btnHome = findViewById(R.id.btnHome);
        btnCommunity = findViewById(R.id.btnCommunity);
        fabCart = findViewById(R.id.fabCart);
        btnNotification = findViewById(R.id.btnNotification);

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, NotificationListActivity.class));
            });
        }

        // Setup bottom navigation highlight for current page
        setupBottomNavHighlight();

        // Best Sellers
        rvBestSellers = findViewById(R.id.rvBestSellers);
        bestSellersList = new ArrayList<>();
        bestSellersAdapter = new ProductAdapter(bestSellersList, true, product -> {
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        }, this::addToCart);
        rvBestSellers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBestSellers.setNestedScrollingEnabled(false);
        rvBestSellers.setAdapter(bestSellersAdapter);

        // New Arrivals
        rvNewArrivals = findViewById(R.id.rvNewArrivals);
        newArrivalsList = new ArrayList<>();
        newArrivalsAdapter = new ProductAdapter(newArrivalsList, product -> {
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        }, this::addToCart);
        rvNewArrivals.setLayoutManager(new LinearLayoutManager(this));
        rvNewArrivals.setNestedScrollingEnabled(false);
        rvNewArrivals.setAdapter(newArrivalsAdapter);

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                // Already on Home
            });
        }

        if (btnCommunity != null) {
            btnCommunity.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, AboutActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        View.OnClickListener openCart = v -> {
            startActivity(new Intent(MainFinalActivity.this, CartActivity.class));
            overridePendingTransition(0, 0);
        };

        if (btnCartTab != null) btnCartTab.setOnClickListener(openCart);
        if (fabCart != null) fabCart.setOnClickListener(openCart);

        // Shop button
        if (btnShop != null) {
            btnShop.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, ProductActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    private void setupBottomNavHighlight() {
        // Highlight "Trang chủ" button as current page
        View btnHome = findViewById(R.id.btnHome);
        if (btnHome != null && btnHome instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) btnHome;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(
                        ContextCompat.getColor(this, R.color.primary),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    );
                } else if (child instanceof TextView) {
                    ((TextView) child).setTextColor(
                        ContextCompat.getColor(this, R.color.primary)
                    );
                }
            }
        }
    }

    private void setupFirebase() {
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/");
        productsRef = db.getReference("products");
    }

    private void fetchProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bestSellersList.clear();
                newArrivalsList.clear();

                int count = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        product.id = data.getKey();
                        if (count < 4) {
                            bestSellersList.add(product);
                        } else {
                            newArrivalsList.add(product);
                        }
                        count++;
                    }
                }
                bestSellersAdapter.notifyDataSetChanged();
                newArrivalsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Error: " + error.getMessage());
            }
        });
    }

    private void addToCart(Product product) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/")
                .getReference("cart").child(userId).child(product.id);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int quantity = 1;
                if (snapshot.exists()) {
                    CartItem item = snapshot.getValue(CartItem.class);
                    if (item != null) {
                        quantity = item.quantity + 1;
                    }
                }
                cartRef.setValue(new CartItem(product.id, quantity))
                        .addOnSuccessListener(aVoid -> Toast.makeText(MainFinalActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(MainFinalActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}

