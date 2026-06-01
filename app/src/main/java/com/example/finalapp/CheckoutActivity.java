package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
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
    private String selectedPaymentMethod = "COD";
    private double subtotal = 0;
    private double discount = 0;
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
            // Intent intent = new Intent(CheckoutActivity.this, SelectAddressActivity.class);
            // intent.putExtra(SelectAddressActivity.EXTRA_UID, userId);
            // startActivityForResult(intent, REQUEST_CODE_ADDRESS);
            Toast.makeText(this, "Tính năng chọn địa chỉ đang phát triển", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardPayment).setOnClickListener(v -> {
            // Intent intent = new Intent(CheckoutActivity.this, PaymentMethodActivity.class);
            // intent.putExtra(PaymentMethodActivity.EXTRA_INITIAL_METHOD, selectedPaymentMethod);
            // startActivityForResult(intent, REQUEST_CODE_PAYMENT);
            Toast.makeText(this, "Tính năng chọn phương thức thanh toán đang phát triển", Toast.LENGTH_SHORT).show();
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
                    String pId = data.getKey();
                    Integer qty = data.getValue(Integer.class);
                    int quantity = (qty != null) ? qty : 1;
                    CartItem item = new CartItem(pId, quantity);
                    cartItemList.add(item);

                    productsRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot pSnapshot) {
                            item.product = pSnapshot.getValue(Product.class);
                            if (item.product != null) {
                                subtotal += item.product.getPrice() * item.quantity;
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
                subtotal + SHIPPING_FEE,
                selectedAddressId,
                txtAddressDetail.getText().toString(),
                selectedPaymentMethod
        );
        order.orderId = orderId;

        ordersRef.child(orderId).setValue(order).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Clear cart
                FirebaseDatabase.getInstance().getReference("cart").child(userId).removeValue();
                Toast.makeText(CheckoutActivity.this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
                // Navigate to main or success screen
                Intent intent = new Intent(CheckoutActivity.this, MainFinalActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CheckoutActivity.this, "Lỗi khi đặt hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_ADDRESS) {
                // selectedAddressId = data.getStringExtra(SelectAddressActivity.RESULT_SELECTED_ADDRESS_ID);
                // fetchAddressDetail(selectedAddressId);
            } else if (requestCode == REQUEST_CODE_PAYMENT) {
                // selectedPaymentMethod = data.getStringExtra(PaymentMethodActivity.RESULT_PAYMENT_METHOD);
                // String label = PaymentMethodActivity.METHOD_COD.equals(selectedPaymentMethod)
                //         ? "Thanh toán khi nhận hàng"
                //         : "Chuyển khoản ngân hàng";
                // txtPaymentMethodDesc.setText(label);
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