package com.example.finalapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.ProductAdapter;
import com.example.finalapp.models.CartItem;
import com.example.finalapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CategoryProductActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_NAME = "category_name";
    public static final String EXTRA_CATEGORY_KEY  = "category_key";

    private static final String[] NON_BONSAI_KEYWORDS = {
            "pot", "pots", "tool", "tools", "chậu", "dụng cụ",
            "accessory", "accessories", "soil", "fertilizer", "phân"
    };

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private final List<Product> productList = new ArrayList<>();
    private TextView txtEmpty;
    private String categoryName;
    private View btnCartHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_product);

        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        TextView txtTitle = findViewById(R.id.txtTitle);
        if (txtTitle != null) txtTitle.setText(categoryName);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        btnCartHeader = findViewById(R.id.btnCartHeader);
        if (btnCartHeader != null) {
            btnCartHeader.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        }

        txtEmpty   = findViewById(R.id.txtEmpty);
        rvProducts = findViewById(R.id.rvProducts);

        adapter = new ProductAdapter(productList, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        }, (product, sourceView, productImage) -> {
            addToCart(product);
            animateFlyToCart(productImage);
        });
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setAdapter(adapter);

        loadBonsaiProducts();
    }

    private void loadBonsaiProducts() {
        FirebaseDatabase.getInstance()
                .getReference("categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Set<String> excludedCategoryIds = new HashSet<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String id   = data.getKey();
                            String name = data.child("name").getValue(String.class);
                            if (name == null) name = "";
                            if (isNonBonsai(name)) excludedCategoryIds.add(id);
                        }
                        fetchProducts(excludedCategoryIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        fetchProducts(new HashSet<>());
                    }
                });
    }

    private void fetchProducts(Set<String> excludedCategoryIds) {
        FirebaseDatabase.getInstance()
                .getReference("products")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Product> allBonsai = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Product p = data.getValue(Product.class);
                            if (p == null) continue;
                            p.id = data.getKey();
                            String catId = p.getCategoryId();
                            if (catId != null && excludedCategoryIds.contains(catId)) continue;
                            if (p.getName() != null && isNonBonsai(p.getName())) continue;
                            allBonsai.add(p);
                        }
                        assignToCategory(allBonsai);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void assignToCategory(List<Product> allBonsai) {
        if (allBonsai.isEmpty()) { showEmpty(); return; }

        long seed = categoryName != null ? categoryName.hashCode() : 0;
        Collections.shuffle(allBonsai, new Random(seed));

        int total = allBonsai.size();
        int chunk  = Math.max(1, total / 4);
        int index  = getCategoryIndex();
        int from   = index * chunk;
        int to     = (index == 3) ? total : Math.min(from + chunk, total);

        if (from >= total) {
            productList.addAll(allBonsai);
        } else {
            productList.addAll(allBonsai.subList(from, to));
        }

        if (productList.isEmpty()) {
            showEmpty();
        } else {
            txtEmpty.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private int getCategoryIndex() {
        if (categoryName == null) return 0;
        switch (categoryName) {
            case "Bonsai trong nhà":  return 0;
            case "Bonsai ngoài trời": return 1;
            case "Cây cho người mới": return 2;
            case "Bộ sưu tập hiếm":  return 3;
            default:                  return 0;
        }
    }

    private void showEmpty() {
        txtEmpty.setVisibility(View.VISIBLE);
        rvProducts.setVisibility(View.GONE);
    }

    private boolean isNonBonsai(String name) {
        String lower = name.toLowerCase();
        for (String kw : NON_BONSAI_KEYWORDS) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private void addToCart(Product product) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, getString(R.string.str_login_required_purchase), Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference cartRef = FirebaseDatabase
                .getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/")
                .getReference("cart").child(userId).child(product.id);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int qty = 1;
                if (snapshot.exists()) {
                    CartItem item = snapshot.getValue(CartItem.class);
                    if (item != null) qty = item.quantity + 1;
                }
                cartRef.setValue(new CartItem(product.id, qty))
                        .addOnSuccessListener(v -> showCartToast())
                        .addOnFailureListener(e -> Toast.makeText(CategoryProductActivity.this,
                                getString(R.string.error_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCartToast() {
        View v = getLayoutInflater().inflate(R.layout.toast_cart, null);
        Toast t = new Toast(this);
        t.setView(v);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
    }

    private void animateFlyToCart(ImageView productImage) {
        if (btnCartHeader == null || productImage == null) return;

        int[] srcLoc = new int[2];
        productImage.getLocationOnScreen(srcLoc);
        float startX = srcLoc[0] + productImage.getWidth() / 2f;
        float startY = srcLoc[1] + productImage.getHeight() / 2f;

        int[] dstLoc = new int[2];
        btnCartHeader.getLocationOnScreen(dstLoc);
        float endX = dstLoc[0] + btnCartHeader.getWidth() / 2f;
        float endY = dstLoc[1] + btnCartHeader.getHeight() / 2f;

        ImageView dot = new ImageView(this);
        if (productImage.getDrawable() != null) {
            dot.setImageDrawable(productImage.getDrawable());
            dot.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            dot.setImageResource(R.drawable.ic_shopping_cart);
        }

        int size = (int) (56 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        dot.setClipToOutline(true);
        dot.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        dot.setElevation(8f);

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        decorView.addView(dot, params);

        float ctrlX = (startX + endX) / 2f;
        float ctrlY = Math.min(startY, endY) - 300f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(550);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            float x = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * ctrlX + t * t * endX;
            float y = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * ctrlY + t * t * endY;
            dot.setX(x - size / 2f);
            dot.setY(y - size / 2f);
            float sc = 1f - 0.6f * t;
            dot.setScaleX(sc);
            dot.setScaleY(sc);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                decorView.removeView(dot);
                if (btnCartHeader != null) {
                    btnCartHeader.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100)
                            .withEndAction(() -> btnCartHeader.animate()
                                    .scaleX(1f).scaleY(1f).setDuration(100).start())
                            .start();
                }
            }
        });
        animator.start();
    }
}
