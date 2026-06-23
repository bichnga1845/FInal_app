package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button btnExploreCollection = findViewById(R.id.btnExploreCollection);
        Button btnBuyNow = findViewById(R.id.btnBuyNow);
        Button btnFinalCta = findViewById(R.id.btnFinalCta);

        if (btnExploreCollection != null) {
            btnExploreCollection.setOnClickListener(v -> openCatalog());
        }

        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> openCatalog());
        }

        if (btnFinalCta != null) {
            btnFinalCta.setOnClickListener(v -> openCatalog());
        }

        TextView txtContactFromAssets = findViewById(R.id.txtContactFromAssets);
        if (txtContactFromAssets != null) {
            String content = loadAboutFromAssets();
            if (!content.isEmpty()) {
                txtContactFromAssets.setText(content);
            }
        }
    }

    private String loadAboutFromAssets() {
        StringBuilder sb = new StringBuilder();
        try {
            String fileName = Locale.getDefault().getLanguage().equals("vi") ? "about_shop_vi.txt" : "about_shop_en.txt";
            InputStream is = getAssets().open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void openCatalog() {
        startActivity(new Intent(this, ProductActivity.class));
    }
}
