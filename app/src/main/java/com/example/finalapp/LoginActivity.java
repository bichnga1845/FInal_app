package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnLogin;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            openMainScreen();
            return;
        }

        edtEmail = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());

        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        TextView txtContinueWith = findViewById(R.id.txtContinueWith);
        txtContinueWith.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Vui long nhap email hop le");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui long nhap mat khau");
            edtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        openMainScreen();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Dang nhap that bai";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainFinalActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
