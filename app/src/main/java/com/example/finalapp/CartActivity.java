package com.example.finalapp;

import android.content.Intent;
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
    private TextView txtSubtotal, txtShipping, txtTotal;
    private Button btnCheckout;
    private ImageButton btnBack;
    private LinearLayout layoutEmpty;

    private DatabaseReference cartRef, productsRef;
    private String userId;
    private final double SHIPPING_FEE = 35000;
    private final double DISCOUNT = 0; // Mock discount like in image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupFirebase();
        fetchCartItems();
    }

    private void initViews() {
        rvCart = findViewById(R.id.rvCart);
        txtSubtotal = findViewById(R.id.txtSubtotal);
        txtShipping = findViewById(R.id.txtShipping);
        txtTotal = findViewById(R.id.txtTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnBack = findViewById(R.id.btnBack);

        cartItemList = new ArrayList<>();
        cartAdapter = new CartAdapter(cartItemList, new CartAdapter.OnCartChangeListener() {
            @Override
            public void onQuantityChange(CartItem item, int newQuantity) {
                cartRef.child(item.productId).setValue(newQuantity);
            }

            @Override
            public void onRemoveItem(CartItem item) {
                cartRef.child(item.productId).removeValue();
            }
        });

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);

        btnCheckout.setOnClickListener(v -> {
            if (cartItemList.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng của bạn đang trống", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(CartActivity.this, CheckoutActivity.class));
            }
        });
        btnBack.setOnClickListener(v -> finish());
        txtShipping.setText(formatMoney(SHIPPING_FEE));
    }

    private void setupFirebase() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        cartRef = db.getReference("cart").child(userId);
        productsRef = db.getReference("products");
    }

    private void fetchCartItems() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItemList.clear();
                if (!snapshot.exists()) {
                    calculateTotal(0);
                    return;
                }

                final long totalCount = snapshot.getChildrenCount();
                final int[] loadedCount = {0};

                for (DataSnapshot data : snapshot.getChildren()) {
                    String pId = data.getKey();
                    Integer qty = data.getValue(Integer.class);
                    int quantity = (qty != null) ? qty : 1;
                    
                    CartItem item = new CartItem(pId, quantity);
                    cartItemList.add(item);

                    productsRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot pSnapshot) {
                            item.product = pSnapshot.getValue(Product.class);
                            loadedCount[0]++;
                            if (loadedCount[0] == totalCount) {
                                cartAdapter.notifyDataSetChanged();
                                updatePriceSummary();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePriceSummary() {
        double subtotal = 0;
        for (CartItem item : cartItemList) {
            if (item.product != null) {
                subtotal += item.product.getVndPrice() * item.quantity;
            }
        }
        calculateTotal(subtotal);
    }

    private void calculateTotal(double subtotal) {
        txtSubtotal.setText(formatMoney(subtotal));
        double finalTotal = subtotal + SHIPPING_FEE - DISCOUNT;
        if (finalTotal < 0) finalTotal = 0;
        txtTotal.setText(formatMoney(finalTotal));
    }

    private String formatMoney(double amount) {
        return new DecimalFormat("#,###đ").format(amount);
    }
}