package com.example.finalapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.CheckoutAdapter;
import com.example.finalapp.models.CartItem;
import com.example.finalapp.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView txtOrderStatus, txtOrderStatusDetail, txtShippingCarrier;
    private TextView txtReceiverNamePhone, txtReceiverAddress;
    private TextView txtTotalAmount, txtOrderId, txtPaymentMethod, txtOrderTime, txtCompletionTime;
    private RecyclerView rvOrderProducts;
    private CheckoutAdapter adapter;
    private String orderId;
    private DatabaseReference orderRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        initViews();
        setupToolbar();

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId != null) {
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        txtOrderStatus = findViewById(R.id.txtOrderStatus);
        txtOrderStatusDetail = findViewById(R.id.txtOrderStatusDetail);
        txtShippingCarrier = findViewById(R.id.txtShippingCarrier);
        txtReceiverNamePhone = findViewById(R.id.txtReceiverNamePhone);
        txtReceiverAddress = findViewById(R.id.txtReceiverAddress);
        txtTotalAmount = findViewById(R.id.txtTotalAmount);
        txtOrderId = findViewById(R.id.txtOrderId);
        txtPaymentMethod = findViewById(R.id.txtPaymentMethod);
        txtOrderTime = findViewById(R.id.txtOrderTime);
        txtCompletionTime = findViewById(R.id.txtCompletionTime);
        rvOrderProducts = findViewById(R.id.rvOrderProducts);

        rvOrderProducts.setLayoutManager(new LinearLayoutManager(this));

        // Copy Order ID
        findViewById(R.id.btnCopyOrderId).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Order ID", txtOrderId.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép mã đơn hàng", Toast.LENGTH_SHORT).show();
        });

        // Support Section
        findViewById(R.id.btnRefund).setOnClickListener(v -> {
            Intent intent = new Intent(OrderDetailActivity.this, RefundSelectionActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
        });

        findViewById(R.id.btnContactShop).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:0937325868"));
            startActivity(intent);
        });

        // Footer Action
        findViewById(R.id.btnReorder).setOnClickListener(v -> reorderItems());

        // Extra Navigation: Store name click to go home
        findViewById(R.id.layoutStoreHeader).setOnClickListener(v -> {
            Intent intent = new Intent(OrderDetailActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void reorderItems() {
        if (orderId == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Order order = snapshot.getValue(Order.class);
                if (order != null && order.items != null) {
                    String uId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("cart").child(uId);

                    for (CartItem item : order.items) {
                        // Reset quantity if needed or just copy
                        cartRef.child(item.productId).setValue(new CartItem(item.productId, item.quantity));
                    }

                    Toast.makeText(OrderDetailActivity.this, "Đã thêm các sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OrderDetailActivity.this, CartActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Order order = snapshot.getValue(Order.class);
                if (order != null) {
                    displayOrderDetails(order);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayOrderDetails(Order order) {
        txtOrderStatus.setText(getStatusText(order.status));
        txtOrderStatusDetail.setText(getStatusDetail(order.status));
        
        // Mock shipping carrier as it's not in the model
        txtShippingCarrier.setText("Nhanh - Giao Hàng Tiết Kiệm");
        
        txtReceiverAddress.setText(order.addressDetail);
        if (order.receiverName != null && order.receiverPhone != null) {
            txtReceiverNamePhone.setText(order.receiverName + " - " + order.receiverPhone);
        } else {
            txtReceiverNamePhone.setText("Thông tin người nhận");
        }

        DecimalFormat df = new DecimalFormat("#,###đ");
        txtTotalAmount.setText(df.format(order.totalAmount));
        
        txtOrderId.setText(order.orderId);
        txtPaymentMethod.setText(order.paymentMethod != null ? order.paymentMethod : "Tiền mặt");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        txtOrderTime.setText(sdf.format(new Date(order.timestamp)));
        
        if ("Delivered".equals(order.status)) {
            txtCompletionTime.setText("Giao hàng thành công");
        } else if ("Pending".equals(order.status) || "Processing".equals(order.status)) {
            txtCompletionTime.setText("Dự kiến 2-3 ngày");
        } else {
            txtCompletionTime.setText("Đang xử lý...");
        }

        if (order.items != null) {
            adapter = new CheckoutAdapter(order.items);
            rvOrderProducts.setAdapter(adapter);
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "Đang xử lý";
        switch (status) {
            case "Pending": return "Chờ xác nhận";
            case "Processing": return "Đang chuẩn bị hàng";
            case "Shipped": return "Đang giao hàng";
            case "Delivered": return "Đơn hàng đã hoàn thành";
            case "Cancelled": return "Đã hủy";
            default: return status;
        }
    }

    private String getStatusDetail(String status) {
        if (status == null) return "Đơn hàng của bạn đang được hệ thống xử lý.";
        switch (status) {
            case "Delivered": return "Cảm ơn bạn đã mua sắm tại Bonsai Shop!";
            case "Cancelled": return "Đơn hàng đã được hủy thành công.";
            case "Shipped": return "Đơn hàng đang trên đường đến tay bạn.";
            default: return "Chúng tôi sẽ sớm giao hàng cho bạn.";
        }
    }
}