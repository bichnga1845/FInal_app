package com.example.finalapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

// Man Quen mat khau - dung Firebase Auth sendPasswordResetEmail()
public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private MaterialButton btnSend;
    private MaterialButton btnBackToLogin;
    private ImageView btnBack;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        View root = findViewById(R.id.forgot_password_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_forgot_email);
        btnSend = findViewById(R.id.btn_send_reset);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        btnBackToLogin.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();

        // ===== Validate =====
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        // ===== Goi Firebase Auth =====
        btnSend.setEnabled(false);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnSend.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Loi khong xac dinh";
                        Toast.makeText(this,
                                "Không gửi được: " + msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
