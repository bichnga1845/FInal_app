package com.example.finalapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finalapp.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

// Man chinh sua ho so - update node /users/{uid} tren Firebase
public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "extra_uid";

    private static final String DEFAULT_UID = "69a9a035fde9b32594ffb37e";

    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etAddress;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private ImageView btnBack;

    private String currentUid;
    private DatabaseReference userRef;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        rootView = findViewById(R.id.edit_profile_root);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        resolveUid();
        loadUserFromFirebase();
        setupButtons();
    }

    private void bindViews() {
        etName = findViewById(R.id.et_edit_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        etAddress = findViewById(R.id.et_edit_address);
        btnSave = findViewById(R.id.btn_save_profile);
        btnCancel = findViewById(R.id.btn_cancel_edit);
        btnBack = findViewById(R.id.btn_back);
    }

    private void resolveUid() {
        String uidFromIntent = getIntent().getStringExtra(EXTRA_UID);
        currentUid = !TextUtils.isEmpty(uidFromIntent) ? uidFromIntent : DEFAULT_UID;
    }

    private void loadUserFromFirebase() {
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(EditProfileActivity.this,
                            "Khong tim thay nguoi dung",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                User user = snapshot.getValue(User.class);
                if (user == null) return;
                etName.setText(safe(user.name));
                etEmail.setText(safe(user.email));
                etPhone.setText(safe(user.phone));
                etAddress.setText(safe(user.address));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this,
                        "Loi tai du lieu: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        // ===== Validate =====
        if (TextUtils.isEmpty(name)) {
            etName.setError("Vui lòng nhập họ và tên");
            etName.requestFocus();
            return;
        }
        if (name.length() < 2) {
            etName.setError("Họ tên phải có ít nhất 2 ký tự");
            etName.requestFocus();
            return;
        }
        if (!TextUtils.isEmpty(phone)) {
            // Validate SDT VN: 10-11 so, bat dau 0
            if (!phone.matches("^0\\d{9,10}$")) {
                etPhone.setError("Số điện thoại không hợp lệ (10-11 số, bắt đầu 0)");
                etPhone.requestFocus();
                return;
            }
        }

        // ===== Update Firebase: chi update 3 truong, khong dung toMap() de tranh ghi de email/role =====
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        btnSave.setEnabled(false);
        userRef.updateChildren(updates, (error, ref) -> {
            btnSave.setEnabled(true);
            if (error == null) {
                Toast.makeText(EditProfileActivity.this,
                        "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this,
                        "Lỗi lưu: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
