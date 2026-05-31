package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

// Man chon phuong thuc thanh toan: COD hoac chuyen khoan
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

        // Phuong thuc mac dinh truyen vao (neu co)
        String initial = getIntent().getStringExtra(EXTRA_INITIAL_METHOD);
        if (METHOD_BANK.equals(initial)) {
            selectMethod(METHOD_BANK);
        } else {
            selectMethod(METHOD_COD);
        }

        optionCod.setOnClickListener(v -> selectMethod(METHOD_COD));
        optionBank.setOnClickListener(v -> selectMethod(METHOD_BANK));
        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> confirmSelection());
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
        String label = METHOD_COD.equals(selectedMethod)
                ? "Thanh toán khi nhận hàng"
                : "Chuyển khoản ngân hàng";
        Toast.makeText(this,
                "Đã chọn: " + label,
                Toast.LENGTH_SHORT).show();

        Intent result = new Intent();
        result.putExtra(RESULT_PAYMENT_METHOD, selectedMethod);
        setResult(RESULT_OK, result);
        finish();
    }
}
