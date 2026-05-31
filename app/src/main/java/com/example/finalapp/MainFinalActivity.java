package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;

public class MainFinalActivity extends AppCompatActivity {

    private RecyclerView rvBestSellers, rvNewArrivals;
    private ProductAdapter bestSellersAdapter, newArrivalsAdapter;
    private List<Product> bestSellersList, newArrivalsList;
    private DatabaseReference productsRef;
    private View btnProfile, btnCartTab;
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
        fabCart = findViewById(R.id.fabCart);

        // Best Sellers
        rvBestSellers = findViewById(R.id.rvBestSellers);
        bestSellersList = new ArrayList<>();
        bestSellersAdapter = new ProductAdapter(bestSellersList, true, product -> {
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
        rvBestSellers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBestSellers.setAdapter(bestSellersAdapter);

        // New Arrivals
        rvNewArrivals = findViewById(R.id.rvNewArrivals);
        newArrivalsList = new ArrayList<>();
        newArrivalsAdapter = new ProductAdapter(newArrivalsList, product -> {
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
        rvNewArrivals.setLayoutManager(new LinearLayoutManager(this));
        rvNewArrivals.setAdapter(newArrivalsAdapter);

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(MainFinalActivity.this, ProfileActivity.class)));
        }

        View.OnClickListener openCart = v -> startActivity(new Intent(MainFinalActivity.this, CartActivity.class));
        
        if (btnCartTab != null) btnCartTab.setOnClickListener(openCart);
        if (fabCart != null) fabCart.setOnClickListener(openCart);
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
}