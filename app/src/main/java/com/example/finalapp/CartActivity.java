package com.example.finalapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.CartAdapter;
import com.example.finalapp.models.CartItem;
import com.example.finalapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView rvCart;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItemList;
    private TextView txtTotal;
    private Button btnCheckout;
    private ImageButton btnBack;
    private LinearLayout layoutEmpty;

    private DatabaseReference cartRef, productsRef;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();

        initViews();
        setupFirebase();
        fetchCartItems();
    }

    private void initViews() {
        rvCart = findViewById(R.id.rvCart);
        txtTotal = findViewById(R.id.txtTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnBack = findViewById(R.id.btnBack);
        layoutEmpty = findViewById(R.id.layoutEmptyCart);

        cartItemList = new ArrayList<>();
        cartAdapter = new CartAdapter(cartItemList, new CartAdapter.OnCartChangeListener() {
            @Override
            public void onQuantityChange(CartItem item, int newQuantity) {
                updateQuantity(item.productId, newQuantity);
            }

            @Override
            public void onRemoveItem(CartItem item) {
                removeItem(item.productId);
            }
        });

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnCheckout.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng thanh toán đang phát triển!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupFirebase() {
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/");
        cartRef = db.getReference("cart").child(userId);
        productsRef = db.getReference("products");
    }

    private void fetchCartItems() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItemList.clear();
                if (!snapshot.exists()) {
                    updateUI(true);
                    return;
                }

                final int totalItems = (int) snapshot.getChildrenCount();
                final int[] loadedItems = {0};

                for (DataSnapshot data : snapshot.getChildren()) {
                    String pId = data.getKey();
                    int quantity = data.getValue(Integer.class);
                    CartItem item = new CartItem(pId, quantity);
                    cartItemList.add(item);

                    // Fetch product details for each item
                    productsRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot productSnapshot) {
                            item.product = productSnapshot.getValue(Product.class);
                            loadedItems[0]++;
                            if (loadedItems[0] == totalItems) {
                                cartAdapter.notifyDataSetChanged();
                                calculateTotal();
                                updateUI(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CartError", error.getMessage());
            }
        });
    }

    private void updateQuantity(String pId, int quantity) {
        cartRef.child(pId).setValue(quantity);
    }

    private void removeItem(String pId) {
        cartRef.child(pId).removeValue();
    }

    private void calculateTotal() {
        double total = 0;
        for (CartItem item : cartItemList) {
            if (item.product != null) {
                total += item.product.getPrice() * item.quantity;
            }
        }
        DecimalFormat df = new DecimalFormat("#,###đ");
        txtTotal.setText(df.format(total));
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvCart.setVisibility(View.GONE);
            txtTotal.setText("0đ");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvCart.setVisibility(View.VISIBLE);
        }
    }
}