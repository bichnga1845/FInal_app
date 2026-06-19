package com.example.finalapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.CategoryAdapter;
import com.example.finalapp.adapters.ProductAdapter;
import com.example.finalapp.models.Category;
import com.example.finalapp.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.finalapp.models.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    private RecyclerView rvProducts, rvCategories;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Product> allProductsList;
    private List<Product> filteredProductsList;
    private List<Category> categoryList;
    private DatabaseReference productsRef, categoriesRef;
    private TextView txtCurrentCategory;
    private EditText edtSearch;
    private String currentCategoryId = "all";
    private TextView filterAll, filterPopular, filterNew;
    private String currentFilterType = "all";
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private View btnCartHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupFilters();
        setupBottomNavigation();
        setupCartHeader();

        fetchCategories();
        fetchAllProducts();
    }

    private void setupBottomNavigation() {
        // Home button
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            startActivity(new Intent(ProductActivity.this, MainFinalActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        // Shop button
        findViewById(R.id.btnShop).setOnClickListener(v -> {
            // Already on ProductActivity
        });

        // Cart button
        findViewById(R.id.layoutBottomCart).setOnClickListener(v -> {
            startActivity(new Intent(ProductActivity.this, CartActivity.class));
            overridePendingTransition(0, 0);
        });

        // Community button
        findViewById(R.id.btnCommunity).setOnClickListener(v -> {
            startActivity(new Intent(ProductActivity.this, AboutActivity.class));
            overridePendingTransition(0, 0);
        });

        // Profile button
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            startActivity(new Intent(ProductActivity.this, ProfileActivity.class));
            overridePendingTransition(0, 0);
        });

        // Highlight "Cửa hàng" button as current page
        setupBottomNavHighlight();
    }

    private void setupBottomNavHighlight() {
        selectTab(findViewById(R.id.btnShop));
    }

    private void selectTab(View selected) {
        int[] tabIds = {R.id.btnHome, R.id.btnShop, R.id.layoutBottomCart, R.id.btnCommunity, R.id.btnProfile};
        for (int id : tabIds) {
            View tab = findViewById(id);
            if (tab != null) tab.setSelected(tab == selected);
        }
    }

    private void initViews() {
        txtCurrentCategory = findViewById(R.id.txtCurrentCategoryName);
        if (txtCurrentCategory != null) txtCurrentCategory.setText(getString(R.string.str_filter_all_caps));

        edtSearch = findViewById(R.id.edtSearch);
        setupSearch();

        rvCategories = findViewById(R.id.rvCategories);
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            currentCategoryId = category.getId();
            filterProducts(edtSearch.getText().toString());
            if (txtCurrentCategory != null) txtCurrentCategory.setText(category.getName().toUpperCase());
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);

        rvProducts = findViewById(R.id.rvProducts);
        allProductsList = new ArrayList<>();
        filteredProductsList = new ArrayList<>();
        productAdapter = new ProductAdapter(filteredProductsList, product -> {
            Intent intent = new Intent(ProductActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        }, (product, sourceView, productImage) -> {
            addToCart(product);
            animateFlyToCart(productImage);
        });
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setNestedScrollingEnabled(false);
        rvProducts.setAdapter(productAdapter);

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/");
        productsRef = db.getReference("products");
        categoriesRef = db.getReference("categories");
    }

    private void setupFilters() {
        filterAll = findViewById(R.id.filter_all);
        filterPopular = findViewById(R.id.filter_popular);
        filterNew = findViewById(R.id.filter_new);

        if (filterAll != null) filterAll.setOnClickListener(v -> updateFilterTab("all"));
        if (filterPopular != null) filterPopular.setOnClickListener(v -> updateFilterTab("popular"));
        if (filterNew != null) filterNew.setOnClickListener(v -> updateFilterTab("new"));
    }

    private void updateFilterTab(String type) {
        currentFilterType = type;

        // Reset all tabs
        if (filterAll != null) {
            filterAll.setBackgroundResource(0);
            filterAll.setTextColor(ContextCompat.getColor(this, R.color.text_description));
            filterAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (filterPopular != null) {
            filterPopular.setBackgroundResource(0);
            filterPopular.setTextColor(ContextCompat.getColor(this, R.color.text_description));
            filterPopular.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (filterNew != null) {
            filterNew.setBackgroundResource(0);
            filterNew.setTextColor(ContextCompat.getColor(this, R.color.text_description));
            filterNew.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Highlight selected tab
        TextView selected = null;
        if (type.equals("all")) selected = filterAll;
        else if (type.equals("popular")) selected = filterPopular;
        else if (type.equals("new")) selected = filterNew;

        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_primary_rounded);
            selected.setTextColor(ContextCompat.getColor(this, R.color.white));
            selected.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        filterProducts(edtSearch.getText().toString());
    }

    private void fetchCategories() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                categoryList.add(new Category("all", getString(R.string.str_all_category)));

                Log.d("FirebaseDebug", "========== CATEGORIES DEBUG ==========");
                for (DataSnapshot data : snapshot.getChildren()) {
                    Category category = data.getValue(Category.class);
                    if (category != null) {
                        category.setId(data.getKey());
                        categoryList.add(category);
                        Log.d("FirebaseDebug", "Category ID: [" + data.getKey() + "] | Name: " + category.getName());
                    }
                }
                Log.d("FirebaseDebug", "Total categories loaded: " + categoryList.size());
                Log.d("FirebaseDebug", "=====================================");
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Lỗi Categories: " + error.getMessage());
            }
        });
    }

    private void fetchAllProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        product.id = data.getKey();
                        allProductsList.add(product);

                        if (allProductsList.size() <= 3) {
                            Log.d("FirebaseDebug", "========== PRODUCT DEBUG ==========");
                            Log.d("FirebaseDebug", "Product ID: " + product.id);
                            Log.d("FirebaseDebug", "Product Name: " + product.getName());
                            Log.d("FirebaseDebug", "CategoryId (camelCase): [" + product.categoryId + "]");
                            Log.d("FirebaseDebug", "Category_id (snake_case): [" + product.category_id + "]");
                            Log.d("FirebaseDebug", "getCategoryId() result: [" + product.getCategoryId() + "]");
                            Log.d("FirebaseDebug", "ImageUrl (camelCase): [" + product.imageUrl + "]");
                            Log.d("FirebaseDebug", "Image_url (snake_case): [" + product.image_url + "]");
                            Log.d("FirebaseDebug", "getImageUrl() result: [" + product.getImageUrl() + "]");
                            Log.d("FirebaseDebug", "==================================");
                        }
                    }
                }
                Log.d("FirebaseDebug", "Tổng số sản phẩm tải từ Firebase: " + allProductsList.size());
                filterProducts("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Lỗi Products: " + error.getMessage());
            }
        });
    }

    private void setupSearch() {
        if (edtSearch == null) return;
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterProducts(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProducts(String query) {
        List<Product> newList = new ArrayList<>();
        String searchQuery = query.toLowerCase().trim();
        String targetCategoryId = (currentCategoryId != null) ? currentCategoryId.trim() : "all";

        for (Product p : allProductsList) {
            String pCategoryId = (p.getCategoryId() != null) ? p.getCategoryId().trim() : "";

            boolean matchesCategory = targetCategoryId.equalsIgnoreCase("all") ||
                    pCategoryId.equalsIgnoreCase(targetCategoryId);

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (p.getName() != null && p.getName().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                newList.add(p);
            }
        }

        // Apply special filters (Popular/New)
        if (!currentFilterType.equals("all") && !newList.isEmpty()) {
            java.util.Collections.shuffle(newList);
            int limit = Math.min(newList.size(), 10);
            newList = new ArrayList<>(newList.subList(0, limit));
        }

        filteredProductsList.clear();
        filteredProductsList.addAll(newList);
        productAdapter.notifyDataSetChanged();
    }

    private void filterProductsByCategory(String categoryId) {
        currentCategoryId = categoryId;
        filterProducts(edtSearch.getText().toString());
    }

    private void setupCartHeader() {
        btnCartHeader = findViewById(R.id.btnCartHeader);
        if (btnCartHeader != null) {
            btnCartHeader.setOnClickListener(v -> {
                startActivity(new Intent(ProductActivity.this, CartActivity.class));
                overridePendingTransition(0, 0);
            });
        }
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
                            .withEndAction(() -> btnCartHeader.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                            .start();
                }
            }
        });
        animator.start();
    }

    private void addToCart(Product product) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, getString(R.string.str_login_required_purchase), Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/")
                .getReference("cart").child(userId).child(product.id);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int quantity = 1;
                if (snapshot.exists()) {
                    CartItem item = snapshot.getValue(CartItem.class);
                    if (item != null) {
                        quantity = item.quantity + 1;
                    }
                }
                cartRef.setValue(new CartItem(product.id, quantity))
                        .addOnSuccessListener(aVoid -> showCartToast())
                        .addOnFailureListener(e -> Toast.makeText(ProductActivity.this, getString(R.string.error_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}

