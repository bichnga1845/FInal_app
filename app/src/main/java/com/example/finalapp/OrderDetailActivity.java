package com.example.finalapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView txtOrderStatus, txtOrderStatusDetail, txtShippingCarrier;
    private TextView txtReceiverNamePhone, txtReceiverAddress;
    private TextView txtTotalAmount, txtOrderId, txtPaymentMethod, txtOrderTime, txtCompletionTime;
    private RecyclerView rvOrderProducts;
    private android.widget.Button btnReorder;
    private CheckoutAdapter adapter;
    private String orderId;
    private Order currentOrder;
    private String currentStatus;
    private DatabaseReference orderRef;
    private String lastKnownStatus = null;

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
            Toast.makeText(this, getString(R.string.str_error_order_id_not_found), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, getString(R.string.str_order_id_copied), Toast.LENGTH_SHORT).show();
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

        // Footer Actions
        btnReorder = findViewById(R.id.btnReorder);
        btnReorder.setOnClickListener(v -> onFooterActionClick());
        findViewById(R.id.btnGoHome).setOnClickListener(v -> {
            Intent intent = new Intent(OrderDetailActivity.this, MainFinalActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Extra Navigation: Store name click to go home
        findViewById(R.id.layoutStoreHeader).setOnClickListener(v -> {
            Intent intent = new Intent(OrderDetailActivity.this, MainFinalActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        findViewById(R.id.btnDownloadInvoice).setOnClickListener(v -> downloadInvoice());
    }

    private void updateFooterButton(String status) {
        if (btnReorder == null) return;
        int green = getResources().getColor(R.color.primary, getTheme());
        if ("Pending".equals(status) || "Processing".equals(status)) {
            btnReorder.setText(getString(R.string.str_cancel_order));
            btnReorder.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFE53935));
        } else if ("Shipped".equals(status)) {
            btnReorder.setText(getString(R.string.contact_shop));
            btnReorder.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(green));
        } else {
            btnReorder.setText(getString(R.string.reorder_button));
            btnReorder.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(green));
        }
    }

    private void onFooterActionClick() {
        if ("Pending".equals(currentStatus) || "Processing".equals(currentStatus)) {
            Intent intent = new Intent(this, CancelOrderActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
        } else if ("Shipped".equals(currentStatus)) {
            startActivity(new Intent(this, ChatActivity.class));
        } else {
            reorderItems();
        }
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
                        cartRef.child(item.productId).setValue(new CartItem(item.productId, item.quantity));
                    }

                    Toast.makeText(OrderDetailActivity.this, getString(R.string.str_items_added_to_cart), Toast.LENGTH_SHORT).show();
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
                currentOrder = snapshot.getValue(Order.class);
                if (currentOrder != null) {
                    displayOrderDetails(currentOrder);
                    String status = currentOrder.status;
                    if (lastKnownStatus != null
                            && status != null
                            && !status.equals(lastKnownStatus)) {
                        String uid = FirebaseAuth.getInstance().getUid();
                        if (uid != null) {
                            com.example.finalapp.utils.NotificationHelper
                                    .orderStatusChanged(OrderDetailActivity.this, uid, orderId, mapStatus(status));
                        }
                    }
                    lastKnownStatus = status;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrderDetailActivity.this, getString(R.string.str_error_load_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadInvoice() {
        if (currentOrder == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
                return;
            }
        }

        AppToast.show(this, "Đang tải hóa đơn...");

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Bitmap bitmap = createInvoiceBitmap();
            if (bitmap != null) {
                Uri imageUri = saveBitmapToGallery(bitmap, "Invoice_" + currentOrder.orderId);
                if (imageUri != null) {
                    showSuccessPopup(imageUri);
                } else {
                    Toast.makeText(this, "Lỗi khi lưu hóa đơn", Toast.LENGTH_SHORT).show();
                }
            }
        }, 1500);
    }

    private Bitmap createInvoiceBitmap() {
        // Lấy view chứa thông tin đơn hàng (NestedScrollView)
        View statusView = findViewById(R.id.layoutStatus);
        if (statusView == null) return null;
        View content = (View) statusView.getParent();
        if (content == null) return null;

        int height = content.getHeight();
        int width = content.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, width, height, paint);
        content.draw(canvas);
        return bitmap;
    }

    private Uri saveBitmapToGallery(Bitmap bitmap, String filename) {
        OutputStream fos = null;
        Uri imageUri = null;
        ContentResolver resolver = getContentResolver();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename + ".png");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Ponsai_Invoices");
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File dir = new File(imagesDir, "Ponsai_Invoices");
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                }
                File image = new File(dir, filename + ".png");
                fos = new FileOutputStream(image);
                imageUri = Uri.fromFile(image);
            }
            
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
            return imageUri;
        } catch (Exception e) {
            return null;
        }
    }

    private void showSuccessPopup(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("Thành công!")
                .setMessage("Hóa đơn đã được lưu vào thư viện ảnh.")
                .setCancelable(true)
                .setPositiveButton("Xem hóa đơn", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void displayOrderDetails(Order order) {
        currentStatus = order.status;
        updateFooterButton(currentStatus);

        android.view.View btnRefundView = findViewById(R.id.btnRefund);
        if (btnRefundView != null) {
            btnRefundView.setVisibility(
                    "Delivered".equals(currentStatus) ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        txtOrderStatus.setText(getStatusText(order.status));
        txtOrderStatusDetail.setText(getStatusDetail(order.status));
        txtShippingCarrier.setText(getString(R.string.shipping_carrier_default));
        txtReceiverAddress.setText(order.addressDetail);
        if (order.receiverName != null && order.receiverPhone != null) {
            txtReceiverNamePhone.setText(order.receiverName + " - " + order.receiverPhone);
        } else {
            txtReceiverNamePhone.setText(getString(R.string.receiver_info_label));
        }

        DecimalFormat df = new DecimalFormat("#,###đ");
        txtTotalAmount.setText(df.format(order.totalAmount));
        txtOrderId.setText(order.orderId);
        txtPaymentMethod.setText(order.paymentMethod != null ? order.paymentMethod : getString(R.string.payment_cash));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        txtOrderTime.setText(sdf.format(new Date(order.timestamp)));
        
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        long now = System.currentTimeMillis();
        if ("Delivered".equals(order.status)) {
            txtCompletionTime.setText(getString(R.string.delivery_success));
        } else if ("Pending".equals(order.status) || "Processing".equals(order.status)) {
            txtCompletionTime.setText(sdfDate.format(new Date(now + 2L * 24 * 60 * 60 * 1000)));
        } else if ("Shipped".equals(order.status)) {
            txtCompletionTime.setText(sdfDate.format(new Date(now + 1L * 24 * 60 * 60 * 1000)));
        } else {
            txtCompletionTime.setText("--");
        }

        if (order.items != null) {
            adapter = new CheckoutAdapter(order.items);
            rvOrderProducts.setAdapter(adapter);
        }
    }

    private String mapStatus(String status) {
        if (status == null) return "update";
        switch (status) {
            case "Processing": return "confirmed";
            case "Shipped":    return "shipping";
            case "Delivered":  return "delivered";
            case "Cancelled":  return "cancelled";
            default:           return "update";
        }
    }

    private String getStatusText(String status) {
        if (status == null) return getString(R.string.processing_label);
        switch (status) {
            case "Pending": return getString(R.string.status_pending);
            case "Processing": return getString(R.string.status_preparing);
            case "Shipped": return getString(R.string.status_shipping);
            case "Delivered": return getString(R.string.order_status_completed);
            case "Cancelled": return getString(R.string.status_cancelled);
            default: return status;
        }
    }

    private String getStatusDetail(String status) {
        if (status == null) return getString(R.string.status_default_desc);
        switch (status) {
            case "Delivered": return getString(R.string.order_status_thanks);
            case "Cancelled": return getString(R.string.status_cancelled_desc);
            case "Shipped": return getString(R.string.status_shipping_desc);
            default: return getString(R.string.status_preparing_desc);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền bộ nhớ!", Toast.LENGTH_SHORT).show();
                downloadInvoice();
            } else {
                Toast.makeText(this, "Cần quyền bộ nhớ để lưu hóa đơn", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
