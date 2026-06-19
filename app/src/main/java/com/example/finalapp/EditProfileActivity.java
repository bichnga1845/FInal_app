package com.example.finalapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.bumptech.glide.Glide;
import com.example.finalapp.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "extra_uid";
    private static final String DEFAULT_UID = "69a9a035fde9b32594ffb37e";

    private static final int AVATAR_SIZE = 256; // px, compress trước khi lưu
    private String pendingAvatarBase64 = null; // chỉ lưu lên Firebase khi bấm Lưu

    private ImageView ivEditAvatar;
    private EditText etName, etEmail, etPhone;
    private MaterialButton btnSave, btnCancel;
    private ImageView btnBack;

    private String currentUid;
    private DatabaseReference userRef;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_profile_root), (v, insets) -> {
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
        ivEditAvatar = findViewById(R.id.iv_edit_avatar);
        etName = findViewById(R.id.et_edit_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        btnSave = findViewById(R.id.btn_save_profile);
        btnCancel = findViewById(R.id.btn_cancel_edit);
        btnBack = findViewById(R.id.btn_back);
    }

    private void resolveUid() {
        String uidFromIntent = getIntent().getStringExtra(EXTRA_UID);
        currentUid = !TextUtils.isEmpty(uidFromIntent) ? uidFromIntent : DEFAULT_UID;
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
    }

    private void loadUserFromFirebase() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    AppToast.show(EditProfileActivity.this, R.string.str_edit_user_not_found);
                    return;
                }
                User user = snapshot.getValue(User.class);
                if (user == null) return;
                etName.setText(safe(user.name));
                etEmail.setText(safe(user.email));
                etPhone.setText(safe(user.phone));

                // Load avatar hiện tại
                String avatarUrl = snapshot.child("avatarUrl").getValue(String.class);
                loadAvatarInto(avatarUrl, ivEditAvatar);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AppToast.showLong(EditProfileActivity.this,
                        getString(R.string.str_profile_load_error, error.getMessage()));
            }
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveChanges());
        findViewById(R.id.avatarContainer).setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));
    }

    private void uploadAvatar(Uri uri) {
        btnSave.setEnabled(false);
        AppToast.show(this, R.string.str_uploading_avatar);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Đọc ảnh gốc
                InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap original = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                // Scale xuống AVATAR_SIZE x AVATAR_SIZE để tiết kiệm quota DB
                Bitmap scaled = Bitmap.createScaledBitmap(original, AVATAR_SIZE, AVATAR_SIZE, true);
                original.recycle();

                // Compress sang JPEG 80%
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                scaled.recycle();

                // Encode Base64
                String base64 = "data:image/jpeg;base64," +
                        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                // Chỉ lưu tạm, chưa ghi Firebase — chờ bấm "Lưu thay đổi"
                pendingAvatarBase64 = base64;
                runOnUiThread(() -> {
                    loadAvatarInto(base64, ivEditAvatar);
                    btnSave.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    AppToast.showLong(this, getString(R.string.str_avatar_upload_failed, e.getMessage()));
                    btnSave.setEnabled(true);
                });
            }
        });
        executor.shutdown();
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.str_err_name_required));
            etName.requestFocus();
            return;
        }
        if (name.length() < 2) {
            etName.setError(getString(R.string.str_err_name_too_short));
            etName.requestFocus();
            return;
        }
        if (!TextUtils.isEmpty(phone) && !phone.matches("^0\\d{9,10}$")) {
            etPhone.setError(getString(R.string.str_err_phone_invalid));
            etPhone.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        if (pendingAvatarBase64 != null) {
            updates.put("avatarUrl", pendingAvatarBase64);
        }

        btnSave.setEnabled(false);
        userRef.updateChildren(updates, (error, ref) -> {
            btnSave.setEnabled(true);
            if (error == null) {
                AppToast.show(this, R.string.str_edit_saved);
                setResult(RESULT_OK);
                finish();
            } else {
                AppToast.showLong(this, getString(R.string.str_edit_save_error, error.getMessage()));
            }
        });
    }

    /** Load avatarUrl (Base64 data URI hoặc https URL) vào ImageView bằng Glide */
    static void loadAvatarInto(String avatarUrl, ImageView target) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return;
        if (avatarUrl.startsWith("data:image")) {
            // Base64 data URI → decode lấy bytes
            String base64Data = avatarUrl.substring(avatarUrl.indexOf(",") + 1);
            byte[] bytes = Base64.decode(base64Data, Base64.NO_WRAP);
            Glide.with(target.getContext())
                    .load(bytes)
                    .placeholder(R.mipmap.ic_account)
                    .circleCrop()
                    .into(target);
        } else {
            Glide.with(target.getContext())
                    .load(avatarUrl)
                    .placeholder(R.mipmap.ic_account)
                    .circleCrop()
                    .into(target);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
