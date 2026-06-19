package com.example.finalapp.utils;

import androidx.annotation.NonNull;

import com.example.finalapp.models.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Tự động cập nhật trạng thái đơn hàng dựa vào thời gian đặt hàng:
 *   Pending     → Processing  sau 5 phút
 *   Processing  → Shipped     sau 10 phút
 *   Shipped     → Delivered   sau 1 ngày
 *
 * Gọi checkAndUpdate() khi load danh sách hoặc chi tiết đơn hàng.
 */
public class OrderStatusUpdater {

    private static final long PENDING_TO_PROCESSING_MS  = 5  * 60 * 1000L;   // 5 phút
    private static final long PROCESSING_TO_SHIPPED_MS  = 10 * 60 * 1000L;   // 10 phút
    private static final long SHIPPED_TO_DELIVERED_MS   = 24 * 60 * 60 * 1000L; // 1 ngày

    /** Kiểm tra và cập nhật status cho 1 đơn theo timestamp. */
    public static void checkAndUpdate(Order order) {
        if (order == null || order.orderId == null) return;
        // Bỏ qua đơn đã kết thúc
        if ("Delivered".equals(order.status)
                || "Cancelled".equals(order.status)
                || "Refund".equals(order.status)) return;

        long now = System.currentTimeMillis();
        long elapsed = now - order.timestamp;
        String newStatus = null;

        if ("Pending".equals(order.status) && elapsed >= PENDING_TO_PROCESSING_MS) {
            newStatus = "Processing";
        } else if ("Processing".equals(order.status) && elapsed >= PROCESSING_TO_SHIPPED_MS) {
            newStatus = "Shipped";
        } else if ("Shipped".equals(order.status) && elapsed >= SHIPPED_TO_DELIVERED_MS) {
            newStatus = "Delivered";
        }

        if (newStatus != null) {
            final String finalStatus = newStatus;
            FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(order.orderId)
                    .child("status")
                    .setValue(finalStatus);
        }
    }

    /** Quét toàn bộ đơn của user và cập nhật nếu cần. */
    public static void checkAllForUser(String uid) {
        if (uid == null) return;
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Order order = ds.getValue(Order.class);
                            if (order != null) {
                                if (order.orderId == null) order.orderId = ds.getKey();
                                checkAndUpdate(order);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
