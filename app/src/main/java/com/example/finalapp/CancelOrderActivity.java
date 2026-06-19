package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;

public class CancelOrderActivity extends AppCompatActivity {

    private RadioGroup rgReasons;
    private MaterialButton btnConfirmCancel;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cancel_order);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cancel_order_header), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        orderId = getIntent().getStringExtra("ORDER_ID");
        rgReasons = findViewById(R.id.rgReasons);
        btnConfirmCancel = findViewById(R.id.btnConfirmCancel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnConfirmCancel.setOnClickListener(v -> confirmCancel());
    }

    private void confirmCancel() {
        if (rgReasons.getCheckedRadioButtonId() == -1) {
            AppToast.show(this, R.string.str_cancel_select_reason);
            return;
        }
        if (orderId == null) return;

        btnConfirmCancel.setEnabled(false);

        FirebaseDatabase.getInstance()
                .getReference("orders")
                .child(orderId)
                .child("status")
                .setValue("Cancelled")
                .addOnSuccessListener(unused -> {
                    AppToast.show(this, R.string.str_cancel_success);
                    // Quay về trang theo dõi đơn hàng
                    Intent intent = new Intent(this, OrderTrackingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmCancel.setEnabled(true);
                    AppToast.showLong(this, "Lỗi: " + e.getMessage());
                });
    }
}
