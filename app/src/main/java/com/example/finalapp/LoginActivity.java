package com.example.finalapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import com.example.finalapp.receivers.NetworkReceiver;
import com.example.finalapp.utils.NetworkUtil;
import com.example.finalapp.utils.KeyboardUtils;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements NetworkReceiver.NetworkChangeListener {

    private EditText edtEmail;
    private EditText edtPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin;
    private FirebaseAuth auth;
    private NetworkReceiver networkReceiver;

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);

        loadSavedCredentials();

        btnLogin.setOnClickListener(v -> attemptLogin());

        ImageView imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(v -> finish());

        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        TextView txtSignUp = findViewById(R.id.txtSignUp);
        txtSignUp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

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

        if (auth.getCurrentUser() != null) {
            openMainScreen();
            return;
        }

        // Khởi tạo NetworkReceiver
        networkReceiver = new NetworkReceiver(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Đăng ký lắng nghe sự kiện mạng khi vào màn hình
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(networkReceiver, filter);
        }
        
        // Kiểm tra trạng thái mạng ngay lúc đầu
        updateNetworkUI(NetworkUtil.isNetworkAvailable(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Hủy đăng ký để tránh rò rỉ bộ nhớ
        unregisterReceiver(networkReceiver);
    }

    @Override
    public void onNetworkChanged(boolean isConnected) {
        updateNetworkUI(isConnected);
    }

    private void updateNetworkUI(boolean isConnected) {
        if (!isConnected) {
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.5f);
            btnLogin.setText(getString(R.string.str_no_internet));
            AppToast.show(this, getString(R.string.str_check_connection));
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
            btnLogin.setText(getString(R.string.str_login));
        }
    }

    private void attemptLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.str_error_email));
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError(getString(R.string.str_error_password));
            edtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        KeyboardUtils.hideKeyboard(this);

        if (cbRememberMe.isChecked()) {
            saveCredentials(email, password);
        } else {
            clearCredentials();
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        openMainScreen();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.str_login_failed);
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

    private void saveCredentials(String email, String password) {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        pref.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .putBoolean(KEY_REMEMBER, true)
                .apply();
    }

    private void clearCredentials() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        pref.edit().clear().apply();
    }

    private void loadSavedCredentials() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean remember = pref.getBoolean(KEY_REMEMBER, false);
        if (remember) {
            edtEmail.setText(pref.getString(KEY_EMAIL, ""));
            edtPassword.setText(pref.getString(KEY_PASSWORD, ""));
            cbRememberMe.setChecked(true);
        }
    }
}
