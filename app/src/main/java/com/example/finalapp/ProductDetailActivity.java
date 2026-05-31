package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalapp.adapters.ProductAdapter;
import com.example.finalapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private TextView txtName, txtSpecies, txtPrice, txtAge, txtHeight, txtDifficulty, txtWater, txtLight, txtDesc;
    private Button btnAddToCart;
    private ImageButton btnBack, btnFavorite;
    private RecyclerView rvRelated;
    
    private String productId;
    private DatabaseReference productsRef, cartRef;
    private FirebaseAuth mAuth;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null) {
            Toast.makeText(this, "Không tìm thấy sản phẩm!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupFirebase();
        fetchProductDetail();
    }

    private void initViews() {
        imgProduct = findViewById(R.id.imgProductLarge);
        txtName = findViewById(R.id.txtDetailName);
        txtSpecies = findViewById(R.id.txtDetailSpecies);
        txtPrice = findViewById(R.id.txtDetailPrice);
        txtAge = findViewById(R.id.txtDetailAge);
        txtHeight = findViewById(R.id.txtDetailHeight);
        txtDifficulty = findViewById(R.id.txtDetailDifficulty);
        txtWater = findViewById(R.id.txtDetailWater);
        txtLight = findViewById(R.id.txtDetailLight);
        txtDesc = findViewById(R.id.txtDetailDesc);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        rvRelated = findViewById(R.id.rvRelatedProducts);

        btnBack.setOnClickListener(v -> finish());
        
        btnAddToCart.setOnClickListener(v -> addToCart());
        
        rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/");
        productsRef = db.getReference("products");
        cartRef = db.getReference("cart");
    }

    private void fetchProductDetail() {
        productsRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentProduct = snapshot.getValue(Product.class);
                if (currentProduct != null) {
                    currentProduct.id = snapshot.getKey();
                    displayProduct(currentProduct);
                    fetchRelatedProducts(currentProduct.getCategoryId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Detail Error: " + error.getMessage());
            }
        });
    }

    private void displayProduct(Product p) {
        txtName.setText(p.getName());
        txtSpecies.setText(p.species != null ? "Loài: " + p.species : "Bonsai nghệ thuật");
        
        DecimalFormat df = new DecimalFormat("#,###đ");
        txtPrice.setText(df.format(p.getPrice()));
        
        txtAge.setText(p.age != null ? p.age : "5 Năm");
        txtHeight.setText(p.height != null ? p.height : "30 cm");
        txtDifficulty.setText(p.careLevel != null ? p.careLevel : "Cơ bản");
        txtWater.setText(p.waterInfo != null ? p.waterInfo : "Mỗi 2 ngày");
        txtLight.setText(p.lightInfo != null ? p.lightInfo : "Bán phần");
        txtDesc.setText(p.description != null ? p.description : "Chưa có mô tả.");

        Glide.with(this)
                .load(p.getImageUrl())
                .placeholder(R.mipmap.ic_banner)
                .into(imgProduct);
    }

    private void fetchRelatedProducts(String categoryId) {
        productsRef.orderByChild("category").equalTo(categoryId).limitToFirst(6)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> related = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    String key = data.getKey();
                    if (p != null && key != null && !key.equals(productId)) {
                        p.id = key;
                        related.add(p);
                    }
                }
                ProductAdapter adapter = new ProductAdapter(related, true, product -> {
                    // Click vào SP liên quan thì mở lại màn hình này với ID mới
                    Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                    intent.putExtra("PRODUCT_ID", product.id);
                    startActivity(intent);
                });
                rvRelated.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addToCart() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        cartRef.child(userId).child(productId).setValue(1) // Tạm thời set số lượng là 1
                .addOnSuccessListener(aVoid -> Toast.makeText(ProductDetailActivity.this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProductDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}