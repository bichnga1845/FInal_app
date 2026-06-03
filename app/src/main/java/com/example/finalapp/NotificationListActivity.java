package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

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
