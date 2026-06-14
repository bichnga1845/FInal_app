package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        setupMenuClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tai lai du lieu moi khi quay ve man Ho so (vd: sau khi sua o EditProfile)
        // => Ho so tu cap nhat ngay, khong can thoat ra vao lai.
        loadUserFromFirebase();
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
                    AppToast.show(ProfileActivity.this, R.string.str_profile_user_not_found);
                    return;
                }
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    AppToast.show(ProfileActivity.this, R.string.str_profile_invalid_data);
                    return;
                }
                user.uid = currentUid;
                bindUserToUi(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AppToast.showLong(ProfileActivity.this,
                        getString(R.string.str_profile_load_error, error.getMessage()));
            }
        });
    }

    private void bindUserToUi(User user) {
        String name = TextUtils.isEmpty(user.name) ? getString(R.string.str_profile_no_name) : user.name;
        String email = TextUtils.isEmpty(user.email) ? getString(R.string.str_profile_no_email) : user.email;

        tvName.setText(name);
        tvWelcomeEmail.setText(email);
        tvWelcomeNameBig.setText(name.toUpperCase());

        tvPhone.setText(TextUtils.isEmpty(user.phone) ? getString(R.string.str_not_updated) : user.phone);
        tvAddress.setText(TextUtils.isEmpty(user.address) ? getString(R.string.str_not_updated) : user.address);
        tvEmail.setText(email);

        String roleLabel = "admin".equalsIgnoreCase(user.role)
                ? getString(R.string.str_role_admin)
                : getString(R.string.str_role_customer);
        tvRole.setText(roleLabel);
    }

    private void setupMenuClicks() {
        // Header "Dang xuat"
        tvHeaderLogout.setOnClickListener(v -> handleLogout());

        // Grid menu items
        menuEditProfile.setOnClickListener(v -> openActivity("com.example.finalapp.EditProfileActivity"));
        menuChangePassword.setOnClickListener(v -> openActivity("com.example.finalapp.ChangePasswordActivity"));
        menuForgotPassword.setOnClickListener(v -> openActivity("com.example.finalapp.ForgotPasswordActivity"));
        menuAddress.setOnClickListener(v -> openActivity("com.example.finalapp.SelectAddressActivity"));
        menuPayment.setOnClickListener(v -> openActivity("com.example.finalapp.PaymentMethodActivity"));

        menuLogout.setOnClickListener(v -> handleLogout());
    }

    /**
     * Mo activity neu ton tai trong project, neu chua co thi show popup.
     * Cho phep tao stub man hinh truoc khi tat ca cac man duoc code xong.
     */
    private void openActivity(String fullyQualifiedName) {
        try {
            Class<?> cls = Class.forName(fullyQualifiedName);
            Intent intent = new Intent(this, cls);
            intent.putExtra(EXTRA_UID, currentUid);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            AppToast.show(this, R.string.str_coming_soon);
        }
    }

    private void handleLogout() {
        AppToast.show(this, R.string.str_logout);
        // Sau khi tich hop Firebase Auth: FirebaseAuth.getInstance().signOut();
        finish();
    }
}
