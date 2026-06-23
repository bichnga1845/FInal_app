package com.example.finalapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
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


    // Header

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
    private LinearLayout menuOrders;

    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_header_bar), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
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
        menuOrders = findViewById(R.id.menu_orders);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void resolveUid() {
        String uidFromIntent = getIntent().getStringExtra(EXTRA_UID);
        if (!TextUtils.isEmpty(uidFromIntent)) {
            currentUid = uidFromIntent;
        } else {
            com.google.firebase.auth.FirebaseUser firebaseUser =
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            currentUid = firebaseUser != null ? firebaseUser.getUid() : null;
        }
        if (TextUtils.isEmpty(currentUid)) {
            finish();
        }
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

        tvWelcomeEmail.setText(email);
        tvWelcomeNameBig.setText(name.toUpperCase());

        // Load avatar
        ImageView ivAvatar = findViewById(R.id.iv_profile_avatar);
        if (ivAvatar != null) {
            EditProfileActivity.loadAvatarInto(user.avatarUrl, ivAvatar);
        }

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

        menuOrders.setOnClickListener(v -> startActivity(new Intent(this, OrderTrackingActivity.class)));
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
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.str_logout))
                .setMessage(getString(R.string.str_logout_confirm))
                .setPositiveButton(getString(R.string.str_logout), (dialog, which) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.str_cancel), null)
                .show();
    }
}
