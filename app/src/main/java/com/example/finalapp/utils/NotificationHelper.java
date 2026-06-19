package com.example.finalapp.utils;

import android.content.Context;

import com.example.finalapp.R;
import com.example.finalapp.models.Notification;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

public class NotificationHelper {

    private static DatabaseReference ref(String userId) {
        return FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId);
    }

    public static void push(String userId, String title, String message, String type, String targetId) {
        DatabaseReference notifRef = ref(userId);
        String id = notifRef.push().getKey();
        if (id == null) return;
        Notification notif = new Notification(id, title, message, type, System.currentTimeMillis(), targetId);
        notifRef.child(id).setValue(notif);
    }

    /** Gọi ngay sau khi đặt hàng thành công */
    public static void orderPlaced(Context ctx, String userId, String orderId) {
        String shortId = shortId(orderId);
        push(userId,
                ctx.getString(R.string.notif_order_placed_title),
                ctx.getString(R.string.notif_order_placed_msg, shortId),
                "order", orderId);
    }

    /** Gọi khi trạng thái đơn hàng thay đổi */
    public static void orderStatusChanged(Context ctx, String userId, String orderId, String newStatus) {
        String shortId = shortId(orderId);
        String title, message;
        switch (newStatus) {
            case "confirmed":
                title   = ctx.getString(R.string.notif_order_confirmed_title);
                message = ctx.getString(R.string.notif_order_confirmed_msg, shortId);
                break;
            case "shipping":
                title   = ctx.getString(R.string.notif_order_shipping_title);
                message = ctx.getString(R.string.notif_order_shipping_msg, shortId);
                break;
            case "delivered":
                title   = ctx.getString(R.string.notif_order_delivered_title);
                message = ctx.getString(R.string.notif_order_delivered_msg, shortId);
                break;
            case "cancelled":
                title   = ctx.getString(R.string.notif_order_cancelled_title);
                message = ctx.getString(R.string.notif_order_cancelled_msg, shortId);
                break;
            default:
                title   = ctx.getString(R.string.notif_order_updated_title);
                message = ctx.getString(R.string.notif_order_updated_msg, shortId);
        }
        push(userId, title, message, "order", orderId);
    }

    /** Thông báo khuyến mãi */
    public static void promo(String userId, String title, String message, String targetProductId) {
        push(userId, title, message, "promo", targetProductId);
    }

    /** Thông báo hệ thống */
    public static void system(String userId, String title, String message) {
        push(userId, title, message, "system", null);
    }

    private static String shortId(String id) {
        if (id == null || id.length() < 6) return id;
        return id.substring(0, 6).toUpperCase();
    }
}
