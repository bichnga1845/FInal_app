package com.example.finalapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import com.example.finalapp.utils.NetworkUtil;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SecurityActivity extends AppCompatActivity {

    private static final int PERM_SMS_CODE = 103;

    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvResult = findViewById(R.id.tvResult);

        // Giữ lại màn hình này để giảng viên chấm điểm phần Security Demo
        findViewById(R.id.btnRequestMicro).setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        });

        findViewById(R.id.btnRequestStorage).setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        });

        findViewById(R.id.btnReadSMS).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                readSMS();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERM_SMS_CODE);
            }
        });

        findViewById(R.id.btnReadContacts).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                readContacts();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 104);
            }
        });

        findViewById(R.id.btnCheckNetwork).setOnClickListener(v -> {
            boolean isConnected = NetworkUtil.isNetworkAvailable(this);
            String type = NetworkUtil.getNetworkType(this);
            String status = isConnected ? getString(R.string.str_connected) : getString(R.string.str_disconnected);
            tvResult.setText(getString(R.string.str_network_status, status) + "\n" + getString(R.string.str_network_type, type));
        });

        findViewById(R.id.btnToggleWifi).setOnClickListener(v -> {
            toggleWifi();
        });
    }

    private void toggleWifi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Trên Android 10+, không thể tự bật/tắt Wifi bằng code, phải mở settings
                Toast.makeText(this, "Android 10+ yêu cầu người dùng tự bật trong Settings", Toast.LENGTH_LONG).show();
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            } else {
                boolean isEnabled = wifiManager.isWifiEnabled();
                wifiManager.setWifiEnabled(!isEnabled);
                Toast.makeText(this, (isEnabled ? "Đang tắt" : "Đang bật") + " Wifi...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Cấp quyền thành công!", Toast.LENGTH_SHORT).show();
            if (requestCode == PERM_SMS_CODE) readSMS();
            if (requestCode == 104) readContacts();
        }
    }

    private void readSMS() {
        StringBuilder sb = new StringBuilder("--- DEMO TỰ ĐỘNG BẮT OTP (SECURITY) ---\n");
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");

        if (cursor != null && cursor.moveToFirst()) {
            int count = 0;
            do {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                if (body.toLowerCase().contains("otp") || body.toLowerCase().contains("mã")) {
                    sb.append("Phát hiện OTP: ").append(body).append("\n\n");
                    count++;
                }
            } while (cursor.moveToNext() && count < 3);
            cursor.close();
            if (count == 0) sb.append("Không tìm thấy tin nhắn chứa OTP nào.");
        } else {
            sb.append("Hộp thư trống.");
        }
        tvResult.setText(sb.toString());
    }

    private void readContacts() {
        StringBuilder sb = new StringBuilder("--- DEMO QUYỀN DANH BẠ (REFERRAL) ---\n");
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int count = 0;
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append("Gợi ý chia sẻ cho: ").append(name).append(" (").append(number).append(")\n");
                count++;
            } while (cursor.moveToNext() && count < 5);
            cursor.close();
        } else {
            sb.append("Không có danh bạ.");
        }
        tvResult.setText(sb.toString());
    }
}
