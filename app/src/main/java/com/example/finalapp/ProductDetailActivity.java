package com.example.finalapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
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

import com.example.finalapp.models.CartItem;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgProduct, btnCartDetail, btnChatDetail, btnShare;
    private View layoutDetailTopBar;
    private TextView txtName, txtSpecies, txtPrice, txtAge, txtHeight, txtDifficulty, txtWater, txtLight, txtDesc, txtSize;
    private View layoutBonsaiStats, layoutPotStats, layoutWaterLight;
    private Button btnAddToCart;
    private ImageButton btnBack;
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
            Toast.makeText(this, getString(R.string.str_error_product_not_found), Toast.LENGTH_SHORT).show();
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
        txtSize = findViewById(R.id.txtDetailSize);
        layoutBonsaiStats = findViewById(R.id.layoutBonsaiStats);
        layoutPotStats = findViewById(R.id.layoutPotStats);
        layoutWaterLight = findViewById(R.id.layoutWaterLight);
        txtDesc = findViewById(R.id.txtDetailDesc);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBack = findViewById(R.id.btnBack);
        btnCartDetail = findViewById(R.id.btnCartDetail);
        btnChatDetail = findViewById(R.id.btnChatDetail);
        btnShare = findViewById(R.id.btnShareProduct);
        layoutDetailTopBar = findViewById(R.id.layoutDetailTopBar);
        rvRelated = findViewById(R.id.rvRelatedProducts);

        btnBack.setOnClickListener(v -> finish());
        btnCartDetail.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        btnChatDetail.setOnClickListener(v -> openChatWithProduct());
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareProduct());
        }

        // Đổi màu bar khi cuộn
        int colorTransparent = android.graphics.Color.TRANSPARENT;
        int colorGreen = getResources().getColor(R.color.primary, null);
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scrollDetail);
        scrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    float threshold = getResources().getDisplayMetrics().density * 60;
                    float fraction = Math.min(1f, scrollY / threshold);
                    int color = (Integer) new ArgbEvaluator().evaluate(fraction, colorTransparent, colorGreen);
                    layoutDetailTopBar.setBackgroundColor(color);
                });

        btnAddToCart.setOnClickListener(v -> {
            addToCart();
            animateFlyToCart(imgProduct);
        });
        
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
        txtName.setText(p.getLocalizedName());
        txtSpecies.setText(p.species != null ? getString(R.string.species_label, p.species) : getString(R.string.bonsai_art));
        
        DecimalFormat df = new DecimalFormat("#,###đ");
        txtPrice.setText(df.format(p.getPrice()));
        
        if (p.isPot()) {
            layoutBonsaiStats.setVisibility(android.view.View.GONE);
            layoutPotStats.setVisibility(android.view.View.VISIBLE);
            if (layoutWaterLight != null) layoutWaterLight.setVisibility(android.view.View.GONE);
            // Lấy kích thước: ưu tiên field size, rồi height, rồi parse từ tên
            String potSize = p.size != null && !p.size.isEmpty() ? p.size
                    : p.height != null && !p.height.isEmpty() ? p.height
                    : extractSizeFromName(p.name);
            txtSize.setText(potSize != null ? potSize : "—");
        } else {
            layoutBonsaiStats.setVisibility(android.view.View.VISIBLE);
            layoutPotStats.setVisibility(android.view.View.GONE);
            if (layoutWaterLight != null) layoutWaterLight.setVisibility(android.view.View.VISIBLE);
            txtAge.setText(p.age != null ? p.age : getString(R.string.years_old, "5"));
            txtHeight.setText(p.height != null ? p.height : "30 cm");
            txtDifficulty.setText(p.careLevel != null ? p.careLevel : getString(R.string.difficulty_label));
            txtWater.setText(p.waterInfo != null ? p.waterInfo : getString(R.string.watering_freq));
            txtLight.setText(p.lightInfo != null ? p.lightInfo : getString(R.string.light_full));
        }
        txtDesc.setText(p.getLocalizedDescription() != null && !p.getLocalizedDescription().isEmpty() ? p.getLocalizedDescription() : getString(R.string.no_description));

        Glide.with(this)
                .load(p.getImageUrl())
                .placeholder(R.mipmap.ic_banner)
                .into(imgProduct);
    }

    private String extractSizeFromName(String name) {
        if (name == null) return null;
        String n = name.toLowerCase();

        // Nếu đã có 2 chiều trong tên (VD: "10 x 7cm") thì dùng luôn
        java.util.regex.Matcher mDouble = java.util.regex.Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s*x\\s*(\\d+(?:[.,]\\d+)?)\\s*cm", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(name);
        if (mDouble.find()) return mDouble.group(1) + " x " + mDouble.group(2) + " cm";

        // Lấy số cm đầu tiên trong tên
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s*cm", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(name);
        if (!m.find()) return null;
        double d = Double.parseDouble(m.group(1).replace(",", "."));

        // Sinh kích thước 2 chiều theo hình dạng chậu
        if (n.contains("round") || n.contains("tròn") || n.contains("circular")) {
            return String.format("Ø %.0f x %.0f cm", d, (double) Math.round(d * 0.6));
        }
        if (n.contains("square") || n.contains("cube") || n.contains("mame")) {
            return String.format("%.0f x %.0f cm", d, (double) Math.round(d * 0.8));
        }
        if (n.contains("cascade")) {
            return String.format("%.0f x %.0f cm", d, (double) Math.round(d * 1.3));
        }
        if (n.contains("oval")) {
            return String.format("%.0f x %.0f cm", d, (double) Math.round(d * 0.65));
        }
        if (n.contains("hexagon")) {
            return String.format("%.0f cm (lục giác)", d);
        }
        if (n.contains("shallow") || n.contains("dish")) {
            return String.format("%.0f x %.0f cm", d, (double) Math.max(3, Math.round(d * 0.25)));
        }
        return String.format("%.0f x %.0f cm", d, (double) Math.round(d * 0.65));
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
                    Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                    intent.putExtra("PRODUCT_ID", product.id);
                    startActivity(intent);
                }, (product, sourceView, img) -> {
                    addToCartById(product.id);
                    animateFlyToCart(img);
                });
                rvRelated.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCartToast() {
        android.view.View v = getLayoutInflater().inflate(R.layout.toast_cart, null);
        android.widget.Toast t = new android.widget.Toast(this);
        t.setView(v);
        t.setDuration(android.widget.Toast.LENGTH_SHORT);
        t.show();
    }

    private void animateFlyFromView(View source) {
        if (btnCartDetail == null || source == null) return;

        int[] srcLoc = new int[2];
        source.getLocationOnScreen(srcLoc);
        float startX = srcLoc[0] + source.getWidth() / 2f;
        float startY = srcLoc[1] + source.getHeight() / 2f;

        int[] dstLoc = new int[2];
        btnCartDetail.getLocationOnScreen(dstLoc);
        float endX = dstLoc[0] + btnCartDetail.getWidth() / 2f;
        float endY = dstLoc[1] + btnCartDetail.getHeight() / 2f;

        ImageView dot = new ImageView(this);
        dot.setImageResource(R.drawable.ic_shopping_cart);
        dot.setColorFilter(getResources().getColor(R.color.white, null));
        dot.setBackgroundColor(getResources().getColor(R.color.primary, null));
        int size = (int) (36 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        dot.setPadding(size / 5, size / 5, size / 5, size / 5);
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
        float ctrlY = Math.min(startY, endY) - 250f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            float x = (1-t)*(1-t)*startX + 2*(1-t)*t*ctrlX + t*t*endX;
            float y = (1-t)*(1-t)*startY + 2*(1-t)*t*ctrlY + t*t*endY;
            dot.setX(x - size / 2f);
            dot.setY(y - size / 2f);
            dot.setScaleX(1f - t * 0.6f);
            dot.setScaleY(1f - t * 0.6f);
            dot.setAlpha(1f - t * 0.2f);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                decorView.removeView(dot);
                btnCartDetail.animate().scaleX(1.35f).scaleY(1.35f).setDuration(100)
                        .withEndAction(() -> btnCartDetail.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();
            }
        });
        animator.start();
    }

    private void animateFlyToCart(ImageView source) {
        if (btnCartDetail == null || source == null) return;

        int[] srcLoc = new int[2];
        source.getLocationOnScreen(srcLoc);
        float startX = srcLoc[0] + source.getWidth() / 2f;
        float startY = srcLoc[1] + source.getHeight() / 2f;

        int[] dstLoc = new int[2];
        btnCartDetail.getLocationOnScreen(dstLoc);
        float endX = dstLoc[0] + btnCartDetail.getWidth() / 2f;
        float endY = dstLoc[1] + btnCartDetail.getHeight() / 2f;

        ImageView dot = new ImageView(this);
        if (source.getDrawable() != null) {
            dot.setImageDrawable(source.getDrawable());
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
            float x = (1-t)*(1-t)*startX + 2*(1-t)*t*ctrlX + t*t*endX;
            float y = (1-t)*(1-t)*startY + 2*(1-t)*t*ctrlY + t*t*endY;
            dot.setX(x - size / 2f);
            dot.setY(y - size / 2f);
            dot.setScaleX(1f - t * 0.5f);
            dot.setScaleY(1f - t * 0.5f);
            dot.setAlpha(1f - t * 0.3f);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                decorView.removeView(dot);
                btnCartDetail.animate().scaleX(1.35f).scaleY(1.35f).setDuration(100)
                        .withEndAction(() -> btnCartDetail.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();
            }
        });
        animator.start();
    }

    private void addToCartById(String pid) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference itemRef = cartRef.child(userId).child(pid);
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int qty = 1;
                if (snapshot.exists()) {
                    CartItem existing = snapshot.getValue(CartItem.class);
                    if (existing != null) qty = existing.quantity + 1;
                }
                itemRef.setValue(new CartItem(pid, qty))
                        .addOnSuccessListener(v -> showCartToast());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addToCart() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, getString(R.string.str_error_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference itemRef = cartRef.child(userId).child(productId);
        
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int quantity = 1;
                if (snapshot.exists()) {
                    CartItem existingItem = snapshot.getValue(CartItem.class);
                    if (existingItem != null) {
                        quantity = existingItem.quantity + 1;
                    }
                }
                CartItem newItem = new CartItem(productId, quantity);
                itemRef.setValue(newItem)
                        .addOnSuccessListener(aVoid -> showCartToast())
                        .addOnFailureListener(e -> Toast.makeText(ProductDetailActivity.this, getString(R.string.error_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProductDetailActivity.this, getString(R.string.str_error_firebase_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatWithProduct() {
        Intent intent = new Intent(this, ChatActivity.class);
        if (currentProduct != null) {
            intent.putExtra("PRODUCT_ID", currentProduct.id);
            intent.putExtra("PRODUCT_NAME", currentProduct.getLocalizedName());
            intent.putExtra("PRODUCT_PRICE", currentProduct.getVndPrice());
            intent.putExtra("PRODUCT_OLD_PRICE", currentProduct.getVndOldPrice());
            String img = currentProduct.getImageUrl();
            if (img != null) intent.putExtra("PRODUCT_IMAGE", img);
        }
        startActivity(intent);
    }

    private void shareProduct() {
        if (currentProduct == null) return;
        
        // Kỹ thuật Security: Yêu cầu quyền danh bạ để demo
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.READ_CONTACTS}, 104);
            return;
        }

        String shareText = "Xem thử cây Bonsai này nhé: " + currentProduct.getLocalizedName() 
                + "\nGiá: " + new DecimalFormat("#,###đ").format(currentProduct.getPrice())
                + "\nTải App Ponsai ngay!";
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Chia sẻ qua"));
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 104) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền danh bạ!", Toast.LENGTH_SHORT).show();
                shareProduct();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền danh bạ để xem danh sách gợi ý chia sẻ", Toast.LENGTH_SHORT).show();
            }
        }
    }
}