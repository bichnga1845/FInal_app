package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

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
    }

    private void openCatalog() {
        startActivity(new Intent(this, ProductActivity.class));
    }
}
