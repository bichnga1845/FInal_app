package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class RefundSelectionActivity extends AppCompatActivity {

    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refund_selection);

        orderId = getIntent().getStringExtra("ORDER_ID");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnReceivedIssue).setOnClickListener(v -> {
            startRefundRequest("RECEIVED_ISSUE");
        });

        findViewById(R.id.btnNotReceived).setOnClickListener(v -> {
            startRefundRequest("NOT_RECEIVED");
        });
    }

    private void startRefundRequest(String type) {
        Intent intent = new Intent(this, RefundRequestActivity.class);
        intent.putExtra("ORDER_ID", orderId);
        intent.putExtra("REFUND_TYPE", type);
        startActivity(intent);
    }
}
