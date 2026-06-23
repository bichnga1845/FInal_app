package com.example.finalapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finalapp.utils.KeyboardUtils;
import com.example.finalapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtPhone, edtEmail, edtFullName, edtPassword, edtRePassword, edtDob;
    private Spinner spinnerGender;
    private CheckBox cbTerms;
    private Button btnRegister;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
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
        edtDob = findViewById(R.id.edtDob);
        spinnerGender = findViewById(R.id.spinnerGender);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);

        // Spinner giới tính
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.str_select_gender), getString(R.string.str_gender_male), getString(R.string.str_gender_female), getString(R.string.str_gender_other)});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // Toggle hiện/ẩn mật khẩu
        ImageView imgTogglePassword = findViewById(R.id.imgTogglePassword);
        if (imgTogglePassword != null) {
            imgTogglePassword.setOnClickListener(v -> {
                boolean isHidden = edtPassword.getTransformationMethod()
                        instanceof PasswordTransformationMethod;
                if (isHidden) {
                    edtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    imgTogglePassword.setImageResource(R.drawable.ic_eye_on);
                } else {
                    edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    imgTogglePassword.setImageResource(R.drawable.ic_eye_off);
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

        ImageView imgToggleRePassword = findViewById(R.id.imgToggleRePassword);
        if (imgToggleRePassword != null) {
            imgToggleRePassword.setOnClickListener(v -> {
                boolean isHidden = edtRePassword.getTransformationMethod()
                        instanceof PasswordTransformationMethod;
                if (isHidden) {
                    edtRePassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    imgToggleRePassword.setImageResource(R.drawable.ic_eye_on);
                } else {
                    edtRePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    imgToggleRePassword.setImageResource(R.drawable.ic_eye_off);
                }
                edtRePassword.setSelection(edtRePassword.getText().length());
            });
        }

        // DatePicker ngày sinh
        edtDob.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format("%02d/%02d/%04d", day, month + 1, year);
                edtDob.setText(date);
            }, cal.get(Calendar.YEAR) - 18, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void attemptRegister() {
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String fullName = edtFullName.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String rePassword = edtRePassword.getText().toString();

        if (TextUtils.isEmpty(phone) || !phone.matches("^0\\d{9,10}$")) {
            edtPhone.setError(getString(R.string.str_error_phone));
            edtPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.str_error_email));
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(fullName) || fullName.length() < 2) {
            edtFullName.setError(getString(R.string.str_error_name));
            edtFullName.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError(getString(R.string.str_error_password_length));
            edtPassword.requestFocus();
            return;
        }

        if (!password.equals(rePassword)) {
            edtRePassword.setError(getString(R.string.str_error_password_mismatch));
            edtRePassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, getString(R.string.str_error_terms), Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        KeyboardUtils.hideKeyboard(this);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        btnRegister.setEnabled(true);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.str_register_failed);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, getString(R.string.str_error_firebase_user), Toast.LENGTH_SHORT).show();
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
                        showSuccessAndGoToLogin();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.str_error_save_user);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSuccessAndGoToLogin() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.str_register_success_title))
                .setMessage(getString(R.string.str_register_success_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.str_login_now), (dialog, which) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }
}
