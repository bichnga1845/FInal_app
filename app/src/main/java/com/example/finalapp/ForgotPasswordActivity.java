package com.example.finalapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

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
            etEmail.setError(getString(R.string.str_fp_err_email_required));
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.str_fp_err_email_invalid));
            etEmail.requestFocus();
            return;
        }

        // ===== Goi Firebase Auth =====
        btnSend.setEnabled(false);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnSend.setEnabled(true);
                    if (task.isSuccessful()) {
                        // Khong dong man ngay de nguoi dung kip doc thong bao
                        AppToast.showLong(this, getString(R.string.str_fp_sent));
                    } else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                        // Email chua duoc dang ky tai khoan Auth
                        etEmail.setError(getString(R.string.str_fp_email_not_found));
                        etEmail.requestFocus();
                        AppToast.showLong(this, getString(R.string.str_fp_email_not_found));
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "";
                        AppToast.showLong(this, getString(R.string.str_fp_error, msg));
                    }
                });
    }
}
