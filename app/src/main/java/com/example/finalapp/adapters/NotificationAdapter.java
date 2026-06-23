package com.example.finalapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.R;
import com.example.finalapp.models.Notification;
import com.example.finalapp.utils.LocalizationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        String localizedTitle = LocalizationHelper.getLocalizedNotifTitle(holder.itemView.getContext(), notification.title);
        String localizedMessage = LocalizationHelper.getLocalizedNotifMessage(holder.itemView.getContext(), notification.message, notification.targetId);
        
        holder.txtTitle.setText(localizedTitle);
        holder.txtMessage.setText(localizedMessage);
        holder.txtTime.setText(getRelativeTime(notification.timestamp, holder.itemView.getContext()));
        holder.viewUnread.setVisibility(notification.isRead ? View.GONE : View.VISIBLE);

        if ("order".equals(notification.type)) {
            holder.imgIcon.setImageResource(R.drawable.ic_shopping_cart);
        } else if ("promo".equals(notification.type)) {
            holder.imgIcon.setImageResource(R.drawable.ic_ticket);
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_notifications);
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String getRelativeTime(long timestamp, android.content.Context ctx) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60000) return ctx.getString(R.string.str_just_now);
        if (diff < 3600000) return (diff / 60000) + ctx.getString(R.string.str_minutes_ago);
        if (diff < 86400000) return (diff / 3600000) + ctx.getString(R.string.str_hours_ago);
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtTitle, txtMessage, txtTime;
        View viewUnread;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgNotifIcon);
            txtTitle = itemView.findViewById(R.id.txtNotifTitle);
            txtMessage = itemView.findViewById(R.id.txtNotifMessage);
            txtTime = itemView.findViewById(R.id.txtNotifTime);
            viewUnread = itemView.findViewById(R.id.viewUnread);
        }
    }
}
