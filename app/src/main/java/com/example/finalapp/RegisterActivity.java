package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finalapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtPhone;
    private EditText edtEmail;
    private EditText edtFullName;
    private EditText edtPassword;
    private EditText edtRePassword;
    private CheckBox cbTerms;
    private Button btnRegister;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        bindViews();

        ImageView imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void bindViews() {
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtFullName = findViewById(R.id.edtFullName);
        edtPassword = findViewById(R.id.edtPassword);
        edtRePassword = findViewById(R.id.edtRePassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void attemptRegister() {
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String fullName = edtFullName.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String rePassword = edtRePassword.getText().toString();

        if (TextUtils.isEmpty(phone) || !phone.matches("^0\\d{9,10}$")) {
            edtPhone.setError("Vui long nhap so dien thoai hop le");
            edtPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Vui long nhap email hop le");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(fullName) || fullName.length() < 2) {
            edtFullName.setError("Vui long nhap ho ten");
            edtFullName.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mat khau phai co it nhat 6 ky tu");
            edtPassword.requestFocus();
            return;
        }

        if (!password.equals(rePassword)) {
            edtRePassword.setError("Mat khau nhap lai khong khop");
            edtRePassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Vui long dong y dieu khoan", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        btnRegister.setEnabled(true);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Dang ky that bai";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, "Khong lay duoc tai khoan vua tao", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveUserProfile(firebaseUser.getUid(), fullName, email, phone);
                });
    }

    private void saveUserProfile(String uid, String fullName, String email, String phone) {
        User user = new User(uid, fullName, email, phone, "", "user", "");
        FirebaseDatabase.getInstance().getReference("users")
                .child(uid)
                .setValue(user.toMap())
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        openMainScreen();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Khong luu duoc thong tin nguoi dung";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainFinalActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }
}
