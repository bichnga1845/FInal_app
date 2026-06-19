package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PaymentMethodActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_METHOD = "extra_initial_method";
    public static final String RESULT_PAYMENT_METHOD = "payment_method";

    public static final String METHOD_COD = "cod";
    public static final String METHOD_BANK = "bank_transfer";

    private LinearLayout optionCod;
    private LinearLayout optionBank;
    private RadioButton rbCod;
    private RadioButton rbBank;
    private View bankInfoCard;
    private MaterialButton btnConfirm;
    private ImageView btnBack;

    private String selectedMethod = METHOD_COD;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_method);

        View root = findViewById(R.id.payment_method_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        setupUserRef();
        loadSavedMethod();

        optionCod.setOnClickListener(v -> selectMethod(METHOD_COD));
        optionBank.setOnClickListener(v -> selectMethod(METHOD_BANK));
        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void setupUserRef() {
        String uid = null;
        String uidFromIntent = getIntent().getStringExtra("extra_uid");
        if (uidFromIntent != null && !uidFromIntent.isEmpty()) {
            uid = uidFromIntent;
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        }
    }

    private void loadSavedMethod() {
        if (userRef == null) {
            selectMethod(METHOD_COD);
            return;
        }
        btnConfirm.setEnabled(false);
        userRef.child("paymentMethod").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String saved = snapshot.getValue(String.class);
                selectMethod(METHOD_BANK.equals(saved) ? METHOD_BANK : METHOD_COD);
                btnConfirm.setEnabled(true);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                selectMethod(METHOD_COD);
                btnConfirm.setEnabled(true);
            }
        });
    }

    private void bindViews() {
        optionCod = findViewById(R.id.option_cod);
        optionBank = findViewById(R.id.option_bank);
        rbCod = findViewById(R.id.rb_cod);
        rbBank = findViewById(R.id.rb_bank);
        bankInfoCard = findViewById(R.id.bank_info_card);
        btnConfirm = findViewById(R.id.btn_confirm_payment);
        btnBack = findViewById(R.id.btn_back);
    }

    private void selectMethod(String method) {
        selectedMethod = method;
        boolean isCod = METHOD_COD.equals(method);

        rbCod.setChecked(isCod);
        rbBank.setChecked(!isCod);

        optionCod.setSelected(isCod);
        optionBank.setSelected(!isCod);

        // Hien thi khung thong tin chuyen khoan khi chon "Chuyen khoan"
        bankInfoCard.setVisibility(isCod ? View.GONE : View.VISIBLE);
    }

    private void confirmSelection() {
        // Lưu vào Firebase
        if (userRef != null) {
            userRef.child("paymentMethod").setValue(selectedMethod);
        }

        String label = METHOD_COD.equals(selectedMethod)
                ? getString(R.string.str_pay_cod_label)
                : getString(R.string.str_pay_bank_title);
        AppToast.show(this, getString(R.string.str_pay_selected, label));

        Intent result = new Intent();
        result.putExtra(RESULT_PAYMENT_METHOD, selectedMethod);
        setResult(RESULT_OK, result);
        finish();
    }
}
