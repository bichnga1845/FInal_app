package com.example.finalapp.utils;

import android.content.Context;
import com.example.finalapp.R;

public class LocalizationHelper {

    public static String getLocalizedNotifTitle(Context context, String title) {
        if (title == null) return "";
        if (title.contains("Đặt hàng thành công")) return context.getString(R.string.notif_order_placed_title);
        if (title.contains("Đơn hàng đã được xác nhận")) return context.getString(R.string.notif_order_confirmed_title);
        if (title.contains("Đơn hàng đang giao")) return context.getString(R.string.notif_order_shipping_title);
        if (title.contains("Giao hàng thành công")) return context.getString(R.string.notif_order_delivered_title);
        if (title.contains("Đơn hàng đã bị huỷ")) return context.getString(R.string.notif_order_cancelled_title);
        if (title.contains("Cập nhật đơn hàng")) return context.getString(R.string.notif_order_updated_title);
        return title;
    }

    public static String getLocalizedNotifMessage(Context context, String message, String targetId) {
        if (message == null) return "";
        if (message.contains("tiếp nhận") && message.contains("đã được")) 
            return context.getString(R.string.notif_order_placed_msg, targetId);
        if (message.contains("cửa hàng xác nhận") && message.contains("đang chuẩn bị")) 
            return context.getString(R.string.notif_order_confirmed_msg, targetId);
        if (message.contains("đang trên đường đến bạn")) 
            return context.getString(R.string.notif_order_shipping_msg, targetId);
        if (message.contains("đã giao thành công")) 
            return context.getString(R.string.notif_order_delivered_msg, targetId);
        if (message.contains("đã được huỷ")) 
            return context.getString(R.string.notif_order_cancelled_msg, targetId);
        if (message.contains("cập nhật mới")) 
            return context.getString(R.string.notif_order_updated_msg, targetId);
        return message;
    }
}
