package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.CheckoutAdapter;
import com.example.finalapp.models.Address;
import com.example.finalapp.models.CartItem;
import com.example.finalapp.models.Order;
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

public class CheckoutActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADDRESS = 1001;
    private static final int REQUEST_CODE_PAYMENT = 1002;
    private static final int REQUEST_CODE_PROMO = 1003;

    private TextView txtAddressLabel, txtAddressDetail, txtReceiverPhone, txtPaymentMethodDesc;
    private TextView txtSubtotal, txtShipping, txtTotal;
    private RecyclerView rvOrderItems;
    private Button btnPlaceOrder, btnSelectPromo;
    private ImageButton btnBack;

    private List<CartItem> cartItemList;
    private CheckoutAdapter checkoutAdapter;

    private String userId;
    private String selectedAddressId;
    private String selectedPaymentMethod = PaymentMethodActivity.METHOD_COD;
    private double subtotal = 0;
    private double discount = 0; // Match Cart discount
    private final double SHIPPING_FEE = 35000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        initViews();
        fetchCartItems();
        fetchDefaultAddress();
    }

    private void initViews() {
        txtAddressLabel = findViewById(R.id.txtAddressLabel);
        txtAddressDetail = findViewById(R.id.txtAddressDetail);
        txtReceiverPhone = findViewById(R.id.txtReceiverPhone);
        txtPaymentMethodDesc = findViewById(R.id.txtPaymentMethodDesc);
        txtSubtotal = findViewById(R.id.txtSubtotal);
        txtShipping = findViewById(R.id.txtShipping);
        txtTotal = findViewById(R.id.txtTotal);
        rvOrderItems = findViewById(R.id.rvOrderItems);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnBack = findViewById(R.id.btnBack);
        btnSelectPromo = findViewById(R.id.btnSelectPromo);

        cartItemList = new ArrayList<>();
        checkoutAdapter = new CheckoutAdapter(cartItemList);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        rvOrderItems.setAdapter(checkoutAdapter);

        findViewById(R.id.cardAddress).setOnClickListener(v -> {
            Intent intent = new Intent(CheckoutActivity.this, SelectAddressActivity.class);
            intent.putExtra(SelectAddressActivity.EXTRA_UID, userId);
            startActivityForResult(intent, REQUEST_CODE_ADDRESS);
        });

        findViewById(R.id.cardPayment).setOnClickListener(v -> {
            Intent intent = new Intent(CheckoutActivity.this, PaymentMethodActivity.class);
            intent.putExtra(PaymentMethodActivity.EXTRA_INITIAL_METHOD, selectedPaymentMethod);
            startActivityForResult(intent, REQUEST_CODE_PAYMENT);
        });

        btnSelectPromo.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng mã giảm giá đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> finish());

        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        txtShipping.setText(formatMoney(SHIPPING_FEE));
    }

    private void fetchCartItems() {
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("cart").child(userId);
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItemList.clear();
                subtotal = 0;
                if (!snapshot.exists()) return;

                long totalCount = snapshot.getChildrenCount();
                final int[] loadedCount = {0};

                for (DataSnapshot data : snapshot.getChildren()) {
                    CartItem item = data.getValue(CartItem.class);
                    if (item == null) continue;
                    
                    if (item.productId == null) {
                        item.productId = data.getKey();
                    }
                    
                    cartItemList.add(item);
                    String pId = item.productId;

                    productsRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot pSnapshot) {
                            item.product = pSnapshot.getValue(Product.class);
                            if (item.product != null) {
                                subtotal += item.product.getVndPrice() * item.quantity;
                            }
                            loadedCount[0]++;
                            if (loadedCount[0] == totalCount) {
                                checkoutAdapter.notifyDataSetChanged();
                                updateSummary();
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

    private void fetchDefaultAddress() {
        DatabaseReference addrRef = FirebaseDatabase.getInstance().getReference("addresses").child(userId);
        addrRef.orderByChild("isDefault").equalTo(true).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Address addr = data.getValue(Address.class);
                        if (addr != null) {
                            selectedAddressId = data.getKey();
                            updateAddressUI(addr);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateAddressUI(Address addr) {
        txtAddressLabel.setText(addr.receiverName);
        txtReceiverPhone.setText(addr.phone);
        txtAddressDetail.setText(addr.detail);
    }

    private void updateSummary() {
        txtSubtotal.setText(formatMoney(subtotal));
        txtTotal.setText(formatMoney(subtotal + SHIPPING_FEE - discount));
    }

    private void placeOrder() {
        if (selectedAddressId == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ nhận hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cartItemList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        String orderId = ordersRef.push().getKey();
        
        Order order = new Order(
                userId,
                cartItemList,
                subtotal,
                SHIPPING_FEE,
                subtotal + SHIPPING_FEE - discount,
                selectedAddressId,
                txtAddressDetail.getText().toString(),
                txtAddressLabel.getText().toString(),
                txtReceiverPhone.getText().toString(),
                selectedPaymentMethod
        );
        order.orderId = orderId;

        ordersRef.child(orderId).setValue(order).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Clear cart
                FirebaseDatabase.getInstance().getReference("cart").child(userId).removeValue();
                showSuccessDialog(orderId);
            } else {
                Toast.makeText(CheckoutActivity.this, "Lỗi khi đặt hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog(String orderId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_success, null);
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Bo góc cho dialog background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnViewOrder).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(CheckoutActivity.this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
            finish();
        });

        dialogView.findViewById(R.id.btnContinueShopping).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(CheckoutActivity.this, ProductActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_ADDRESS) {
                selectedAddressId = data.getStringExtra(SelectAddressActivity.RESULT_SELECTED_ADDRESS_ID);
                if (selectedAddressId != null) {
                    fetchAddressDetail(selectedAddressId);
                }
            } else if (requestCode == REQUEST_CODE_PAYMENT) {
                selectedPaymentMethod = data.getStringExtra(PaymentMethodActivity.RESULT_PAYMENT_METHOD);
                String label = PaymentMethodActivity.METHOD_COD.equals(selectedPaymentMethod)
                        ? "Thanh toán khi nhận hàng"
                        : "Chuyển khoản ngân hàng";
                txtPaymentMethodDesc.setText(label);
            }
        }
    }

    private void fetchAddressDetail(String addressId) {
        DatabaseReference addrRef = FirebaseDatabase.getInstance().getReference("addresses").child(userId).child(addressId);
        addrRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Address addr = snapshot.getValue(Address.class);
                if (addr != null) {
                    updateAddressUI(addr);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String formatMoney(double amount) {
        return new DecimalFormat("#,###đ").format(amount);
    }
}