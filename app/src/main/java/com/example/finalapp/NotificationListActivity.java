package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.NotificationAdapter;
import com.example.finalapp.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationListActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private LinearLayout layoutEmpty;
    private ImageButton btnBack;
    private TextView btnMarkAllRead;

    private DatabaseReference notificationsRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        initViews();
        setupFirebase();
        fetchNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnBack = findViewById(R.id.btnBack);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, notification -> {
            // Đánh dấu đã đọc
            notificationsRef.child(notification.id).child("isRead").setValue(true);
            
            // Mở màn hình chi tiết
            Intent intent = new Intent(NotificationListActivity.this, NotificationDetailActivity.class);
            intent.putExtra("NOTIFICATION_ID", notification.id);
            startActivity(intent);
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        // Test: Long click vào tiêu đề để tạo thông báo mẫu
        findViewById(R.id.txtHeaderTitle).setOnLongClickListener(v -> {
            createTestNotifications();
            return true;
        });
    }

    private void createTestNotifications() {
        String[] titles = {"Đơn hàng thành công", "Khuyến mãi cực hot", "Chào mừng bạn mới", "Hệ thống bảo trì"};
        String[] messages = {
            "Đơn hàng #BS123 của bạn đã được giao thành công. Đừng quên đánh giá sản phẩm nhé!",
            "Nhập mã BONSAI50 để được giảm giá 50% cho tất cả các loại cây trong hôm nay.",
            "Cảm ơn bạn đã tham gia cộng đồng Bonsai Shop. Hãy bắt đầu mua sắm ngay thôi!",
            "Hệ thống sẽ bảo trì từ 0h đến 2h sáng mai. Rất xin lỗi vì sự bất tiện này."
        };
        String[] types = {"order", "promo", "system", "system"};

        for (int i = 0; i < titles.length; i++) {
            String id = notificationsRef.push().getKey();
            Notification notif = new Notification(
                id,
                titles[i],
                messages[i],
                types[i],
                System.currentTimeMillis() - (i * 3600000L), // Mỗi cái cách nhau 1 giờ
                "test_target_id"
            );
            if (id != null) {
                notificationsRef.child(id).setValue(notif);
            }
        }
        android.widget.Toast.makeText(this, "Đã tạo 4 thông báo mẫu", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void markAllAsRead() {
        for (Notification notification : notificationList) {
            if (!notification.isRead) {
                notificationsRef.child(notification.id).child("isRead").setValue(true);
            }
        }
    }

    private void setupFirebase() {
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);
    }

    private void fetchNotifications() {
        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification != null) {
                        notification.id = data.getKey();
                        notificationList.add(notification);
                    }
                }
                // Sắp xếp thông báo mới nhất lên đầu
                Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
                
                adapter.notifyDataSetChanged();
                layoutEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
