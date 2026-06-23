package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalapp.models.Notification;
import com.example.finalapp.utils.LocalizationHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationDetailActivity extends AppCompatActivity {

    private ImageView imgIcon;
    private TextView txtTitle, txtTime, txtMessage;
    private Button btnAction;
    private ImageButton btnBack;

    private String notificationId;
    private DatabaseReference notifRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        notificationId = getIntent().getStringExtra("NOTIFICATION_ID");
        if (notificationId == null) {
            finish();
            return;
        }

        initViews();
        fetchNotificationDetail();
    }

    private void initViews() {
        imgIcon = findViewById(R.id.imgTypeIcon);
        txtTitle = findViewById(R.id.txtDetailTitle);
        txtTime = findViewById(R.id.txtDetailTime);
        txtMessage = findViewById(R.id.txtDetailMessage);
        btnAction = findViewById(R.id.btnAction);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchNotificationDetail() {
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId).child(notificationId);
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Notification notif = snapshot.getValue(Notification.class);
                if (notif != null) {
                    displayDetail(notif);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayDetail(Notification notif) {
        String localizedTitle = LocalizationHelper.getLocalizedNotifTitle(this, notif.title);
        String localizedMessage = LocalizationHelper.getLocalizedNotifMessage(this, notif.message, notif.targetId);
        
        txtTitle.setText(localizedTitle);
        txtMessage.setText(localizedMessage);
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        txtTime.setText(sdf.format(new Date(notif.timestamp)));

        if ("order".equals(notif.type)) {
            imgIcon.setImageResource(R.drawable.ic_shopping_cart);
            btnAction.setText(getString(R.string.notification_order_detail_btn));
            btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", notif.targetId);
                startActivity(intent);
            });
        } else if ("promo".equals(notif.type)) {
            imgIcon.setImageResource(R.drawable.ic_ticket);
            btnAction.setText(getString(R.string.notification_promo_btn));
            btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainFinalActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        } else if ("product".equals(notif.type)) {
            imgIcon.setImageResource(R.drawable.ic_leaf); // Assuming ic_leaf exists
            btnAction.setText(getString(R.string.notification_product_btn));
            btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra("PRODUCT_ID", notif.targetId);
                startActivity(intent);
            });
        } else {
            imgIcon.setImageResource(R.drawable.ic_notifications);
            btnAction.setVisibility(View.GONE);
        }
    }
}
