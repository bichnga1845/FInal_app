package com.example.finalapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Man Doi mat khau - dung Firebase Auth: re-authenticate roi updatePassword()
public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrent;
    private EditText etNew;
    private EditText etConfirm;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private ImageView btnBack;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        View root = findViewById(R.id.change_password_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        bindViews();
        setupButtons();
    }

    private void bindViews() {
        etCurrent = findViewById(R.id.et_current_password);
        etNew = findViewById(R.id.et_new_password);
        etConfirm = findViewById(R.id.et_confirm_password);
        btnSave = findViewById(R.id.btn_save_password);
        btnCancel = findViewById(R.id.btn_cancel_password);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String current = etCurrent.getText().toString();
        String pwNew = etNew.getText().toString();
        String confirm = etConfirm.getText().toString();

        // ===== Validate input =====
        if (TextUtils.isEmpty(current)) {
            etCurrent.setError(getString(R.string.str_cp_err_current_required));
            etCurrent.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(pwNew)) {
            etNew.setError(getString(R.string.str_cp_err_new_required));
            etNew.requestFocus();
            return;
        }
        if (pwNew.length() < 6) {
            etNew.setError(getString(R.string.str_cp_err_new_too_short));
            etNew.requestFocus();
            return;
        }
        if (pwNew.equals(current)) {
            etNew.setError(getString(R.string.str_cp_err_new_same));
            etNew.requestFocus();
            return;
        }
        if (!pwNew.equals(confirm)) {
            etConfirm.setError(getString(R.string.str_cp_err_confirm_mismatch));
            etConfirm.requestFocus();
            return;
        }

        // ===== Kiem tra da dang nhap chua =====
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            AppToast.showLong(this, getString(R.string.str_cp_need_login));
            return;
        }

        // ===== Re-authenticate roi update password =====
        // Firebase yeu cau xac thuc lai voi mat khau cu truoc khi cho doi
        btnSave.setEnabled(false);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);
        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (!reauthTask.isSuccessful()) {
                        btnSave.setEnabled(true);
                        AppToast.show(this, R.string.str_cp_wrong_current);
                        return;
                    }
                    user.updatePassword(pwNew)
                            .addOnCompleteListener(updateTask -> {
                                btnSave.setEnabled(true);
                                if (updateTask.isSuccessful()) {
                                    AppToast.show(this, R.string.str_cp_success);
                                    finish();
                                } else {
                                    String msg = updateTask.getException() != null
                                            ? updateTask.getException().getMessage()
                                            : "";
                                    AppToast.showLong(this, getString(R.string.str_cp_error, msg));
                                }
                            });
                });
    }
}
