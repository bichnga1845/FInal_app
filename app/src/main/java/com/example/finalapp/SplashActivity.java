package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoContainer = findViewById(R.id.logo_container);
        View footerContainer = findViewById(R.id.footer_container);

        // Đảm bảo View bắt đầu từ trạng thái ẩn và hơi lệch vị trí
        logoContainer.setAlpha(0f);
        logoContainer.setScaleX(0.8f);
        logoContainer.setScaleY(0.8f);
        logoContainer.setVisibility(View.VISIBLE);

        footerContainer.setAlpha(0f);
        footerContainer.setTranslationY(50f);
        footerContainer.setVisibility(View.VISIBLE);

        // Chạy hiệu ứng mượt mà
        logoContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .start();

        footerContainer.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(1000)
                .setStartDelay(600)
                .start();

        // Chuyển màn sau 3.5 giây
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> destination = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                    ? MainFinalActivity.class
                    : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, destination));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3500);
    }
}