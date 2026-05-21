package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finalapp.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Man Ho so - layout theo wireframe: header avatar nho + welcome card + grid menu 3x2 + info card
public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "extra_uid";

    // UID test mac dinh (Nga Tran Thi Bich trong firebase_migration.json)
    // Khi nhom tich hop Firebase Auth xong, LoginActivity se truyen UID that qua Intent
    private static final String DEFAULT_UID = "69a9a035fde9b32594ffb37e";

    // Header
    private TextView tvName;
    private TextView tvHeaderLogout;

    // Welcome card
    private TextView tvWelcomeEmail;
    private TextView tvWelcomeNameBig;

    // Info card
    private TextView tvPhone;
    private TextView tvAddress;
    private TextView tvEmail;
    private TextView tvRole;

    // Grid menu items
    private LinearLayout menuEditProfile;
    private LinearLayout menuChangePassword;
    private LinearLayout menuForgotPassword;
    private LinearLayout menuAddress;
    private LinearLayout menuPayment;
    private LinearLayout menuLogout;

    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        resolveUid();
        loadUserFromFirebase();
        setupMenuClicks();
    }

    private void bindViews() {
        tvName = findViewById(R.id.tv_profile_name);
        tvHeaderLogout = findViewById(R.id.tv_header_logout);

        tvWelcomeEmail = findViewById(R.id.tv_welcome_email);
        tvWelcomeNameBig = findViewById(R.id.tv_welcome_name_big);

        tvPhone = findViewById(R.id.tv_profile_phone);
        tvAddress = findViewById(R.id.tv_profile_address);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvRole = findViewById(R.id.tv_profile_role);

        menuEditProfile = findViewById(R.id.menu_edit_profile);
        menuChangePassword = findViewById(R.id.menu_change_password);
        menuForgotPassword = findViewById(R.id.menu_forgot_password);
        menuAddress = findViewById(R.id.menu_address);
        menuPayment = findViewById(R.id.menu_payment);
        menuLogout = findViewById(R.id.menu_logout);
    }

    private void resolveUid() {
        String uidFromIntent = getIntent().getStringExtra(EXTRA_UID);
        currentUid = !TextUtils.isEmpty(uidFromIntent) ? uidFromIntent : DEFAULT_UID;
    }

    private void loadUserFromFirebase() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProfileActivity.this,
                            "Khong tim thay thong tin nguoi dung",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    Toast.makeText(ProfileActivity.this,
                            "Du lieu khong hop le",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                user.uid = currentUid;
                bindUserToUi(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Loi tai du lieu: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUserToUi(User user) {
        String name = TextUtils.isEmpty(user.name) ? "Chua co ten" : user.name;
        String email = TextUtils.isEmpty(user.email) ? "Chua co email" : user.email;

        tvName.setText(name);
        tvWelcomeEmail.setText(email);
        tvWelcomeNameBig.setText(name.toUpperCase());

        tvPhone.setText(TextUtils.isEmpty(user.phone) ? "Chưa cập nhật" : user.phone);
        tvAddress.setText(TextUtils.isEmpty(user.address) ? "Chưa cập nhật" : user.address);
        tvEmail.setText(email);

        String roleLabel = "admin".equalsIgnoreCase(user.role) ? "Quản trị viên" : "Khách hàng";
        tvRole.setText(roleLabel);
    }

    private void setupMenuClicks() {
        // Header "Dang xuat"
        tvHeaderLogout.setOnClickListener(v -> handleLogout());

        // Grid menu items
        menuEditProfile.setOnClickListener(v -> openActivity("com.example.finalapp.EditProfileActivity",
                "Man Chinh sua ho so se duoc lam sau"));

        menuChangePassword.setOnClickListener(v -> openActivity("com.example.finalapp.ChangePasswordActivity",
                "Man Doi mat khau se duoc lam sau"));

        menuForgotPassword.setOnClickListener(v -> openActivity("com.example.finalapp.ForgotPasswordActivity",
                "Man Quen mat khau se duoc lam sau"));

        menuAddress.setOnClickListener(v -> openActivity("com.example.finalapp.SelectAddressActivity",
                "Man Dia chi se duoc lam sau"));

        menuPayment.setOnClickListener(v -> openActivity("com.example.finalapp.PaymentMethodActivity",
                "Man Thanh toan se duoc lam sau"));

        menuLogout.setOnClickListener(v -> handleLogout());
    }

    /**
     * Mo activity neu ton tai trong project, neu chua co thi show Toast.
     * Cho phep tao stub man hinh truoc khi tat ca cac man duoc code xong.
     */
    private void openActivity(String fullyQualifiedName, String notReadyMessage) {
        try {
            Class<?> cls = Class.forName(fullyQualifiedName);
            Intent intent = new Intent(this, cls);
            intent.putExtra(EXTRA_UID, currentUid);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, notReadyMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLogout() {
        Toast.makeText(this, "Đăng xuất", Toast.LENGTH_SHORT).show();
        // Sau khi tich hop Firebase Auth: FirebaseAuth.getInstance().signOut();
        finish();
    }
}
