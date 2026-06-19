package com.example.finalapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.finalapp.models.CartItem;
import com.example.finalapp.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class RefundRequestActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private TextView txtProductName, txtProductVariant, txtProductPrice, txtQuantity;
    private TextView txtReason, txtSolution, txtRefundAmount, txtCharCount;
    private EditText edtDescription, edtEmail;
    private Button btnSubmit;

    private String orderId;
    private String refundType;
    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refund_request);

        orderId = getIntent().getStringExtra("ORDER_ID");
        refundType = getIntent().getStringExtra("REFUND_TYPE");

        initViews();
        setupToolbar();
        loadOrderDetails();
        setupInputListeners();
    }

    private void initViews() {
        imgProduct = findViewById(R.id.imgProduct);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductVariant = findViewById(R.id.txtProductVariant);
        txtProductPrice = findViewById(R.id.txtProductPrice);
        txtQuantity = findViewById(R.id.txtQuantity);
        txtReason = findViewById(R.id.txtReason);
        txtSolution = findViewById(R.id.txtSolution);
        txtRefundAmount = findViewById(R.id.txtRefundAmount);
        txtCharCount = findViewById(R.id.txtCharCount);
        edtDescription = findViewById(R.id.edtDescription);
        edtEmail = findViewById(R.id.edtEmail);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.btnSelectReason).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(getString(R.string.str_refund_reason1));
            popup.getMenu().add(getString(R.string.str_refund_reason2));
            popup.getMenu().add(getString(R.string.str_refund_reason3));
            popup.getMenu().add(getString(R.string.str_refund_reason4));
            popup.getMenu().add(getString(R.string.str_refund_reason5));
            
            popup.setOnMenuItemClickListener(item -> {
                txtReason.setText(item.getTitle());
                validateForm();
                return true;
            });
            popup.show();
        });

        findViewById(R.id.btnSelectSolution).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(getString(R.string.str_refund_solution1));
            popup.getMenu().add(getString(R.string.str_refund_solution2));
            popup.getMenu().add(getString(R.string.str_refund_solution3));

            popup.setOnMenuItemClickListener(item -> {
                txtSolution.setText(item.getTitle());
                validateForm();
                return true;
            });
            popup.show();
        });

        btnSubmit.setOnClickListener(v -> submitRefundRequest());

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            edtEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadOrderDetails() {
        if (orderId == null) return;
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                order = snapshot.getValue(Order.class);
                if (order != null && order.items != null && !order.items.isEmpty()) {
                    displayOrderInfo(order);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayOrderInfo(Order order) {
        CartItem firstItem = order.items.get(0);
        if (firstItem.product != null) {
            txtProductName.setText(firstItem.product.getLocalizedName());
            txtProductVariant.setText(firstItem.product.category);
            txtProductPrice.setText(formatMoney(firstItem.product.getVndPrice()));
            if (firstItem.product.imageUrl != null && !firstItem.product.imageUrl.isEmpty()) {
                Glide.with(this).load(firstItem.product.imageUrl).into(imgProduct);
            }
        }
        txtQuantity.setText(getString(R.string.str_quantity_prefix) + firstItem.quantity);
        txtRefundAmount.setText(formatMoney(order.totalAmount));
    }

    private void setupInputListeners() {
        edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                txtCharCount.setText(s.length() + "/2000");
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateForm() {
        boolean isValid = !txtReason.getText().toString().equals(getString(R.string.str_refund_reason_placeholder))
                && !txtSolution.getText().toString().equals("-")
                && !edtEmail.getText().toString().isEmpty();

        if (isValid) {
            btnSubmit.setEnabled(true);
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            btnSubmit.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
        } else {
            btnSubmit.setEnabled(false);
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEEEEEE));
            btnSubmit.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.placeholder));
        }
    }

    private void submitRefundRequest() {
        if (orderId == null) return;
        btnSubmit.setEnabled(false);
        // Cập nhật status đơn hàng → Refund trên Firebase
        FirebaseDatabase.getInstance()
                .getReference("orders")
                .child(orderId)
                .child("status")
                .setValue("Refund")
                .addOnSuccessListener(unused -> showSuccessPopup())
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    android.widget.Toast.makeText(this,
                            "Lỗi: " + e.getMessage(),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessPopup() {
        View dimOverlay = findViewById(R.id.dimOverlay);
        View layoutPopup = findViewById(R.id.layoutSuccessPopup);

        dimOverlay.setVisibility(View.VISIBLE);
        layoutPopup.setVisibility(View.VISIBLE);

        // Animate popup vào
        layoutPopup.setAlpha(0f);
        layoutPopup.setScaleX(0.85f);
        layoutPopup.setScaleY(0.85f);
        layoutPopup.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).start();

        layoutPopup.findViewById(R.id.btnBackHome).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, MainFinalActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }

    private String formatMoney(double amount) {
        return new DecimalFormat("#,###đ").format(amount);
    }
}
