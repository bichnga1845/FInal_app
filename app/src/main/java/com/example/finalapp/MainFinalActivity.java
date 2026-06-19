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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finalapp.adapters.BannerAdapter;
import com.example.finalapp.adapters.BannerAdapter.BannerItem;
import com.example.finalapp.adapters.ReviewAdapter;
import com.example.finalapp.adapters.ReviewAdapter.ReviewItem;

import com.example.finalapp.adapters.ProductAdapter;
import com.example.finalapp.adapters.SearchResultAdapter;
import com.example.finalapp.models.CartItem;
import com.example.finalapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainFinalActivity extends AppCompatActivity {

    private RecyclerView rvBestSellers, rvNewArrivals;
    private ProductAdapter bestSellersAdapter, newArrivalsAdapter;
    private List<Product> bestSellersList, newArrivalsList;
    private List<Product> allProducts = new ArrayList<>();
    private DatabaseReference productsRef, usersRef;
    private View btnProfile, btnCartTab, btnNotification, btnShop, btnHome, btnCommunity, btnCartHeader;
    private TextView txtUserName, tvGreeting;
    private EditText edtSearch;
    private CardView cardSearchResults;
    private SearchResultAdapter searchResultAdapter;
    private List<Product> searchResults = new ArrayList<>();

    private RecyclerView rvReviews;

    private ViewPager2 bannerPager;
    private LinearLayout bannerDots;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());

    private BannerItem[] BANNER_ITEMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_final);

        initViews();
        setupFirebase();
        BANNER_ITEMS = new BannerItem[]{
            new BannerItem(R.mipmap.ic_poster_bonsai,
                    getString(R.string.banner1_tag), getString(R.string.banner1_title), getString(R.string.banner1_desc)),
            new BannerItem(R.mipmap.ic_poster_bonsai2,
                    getString(R.string.banner2_tag), getString(R.string.banner2_title), getString(R.string.banner2_desc)),
            new BannerItem(R.mipmap.ic_poster_bonsai3,
                    getString(R.string.banner3_tag), getString(R.string.banner3_title), getString(R.string.banner3_desc)),
            new BannerItem(R.mipmap.ic_poster_bonsai4,
                    getString(R.string.banner4_tag), getString(R.string.banner4_title), getString(R.string.banner4_desc))
        };
        setupBanner();
        setupSearch();
        setupReviews();
        fetchProducts();
        loadUserProfile();
    }

    private void setupBanner() {
        bannerPager = findViewById(R.id.bannerPager);
        bannerDots  = findViewById(R.id.bannerDots);
        if (bannerPager == null) return;

        List<BannerItem> items = new ArrayList<>();
        for (BannerItem item : BANNER_ITEMS) items.add(item);

        BannerAdapter adapter = new BannerAdapter(items);
        bannerPager.setAdapter(adapter);

        for (int i = 0; i < BANNER_ITEMS.length; i++) {
            View dot = new View(this);
            int dp = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp, dp);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0
                    ? R.drawable.bg_dot_active
                    : R.drawable.bg_dot_inactive);
            bannerDots.addView(dot);
        }

        bannerPager.setOffscreenPageLimit(1);

        if (bannerPager.getChildCount() > 0) {
            bannerPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        bannerPager.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(
                    event.getAction() != android.view.MotionEvent.ACTION_UP &&
                    event.getAction() != android.view.MotionEvent.ACTION_CANCEL);
            return false;
        });

        bannerPager.setPageTransformer((page, position) -> {
            if (position < -1 || position > 1) {
                page.setAlpha(0f);
                return;
            }
            float absPos = Math.abs(position);
            float scale = 0.85f + (1 - absPos) * 0.15f;
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(0.4f + (1 - absPos) * 0.6f);
            float vMargin = page.getHeight() * (1 - scale) / 2f;
            float hMargin = page.getWidth() * (1 - scale) / 2f;
            page.setTranslationX(position < 0 ? hMargin - vMargin / 2f : -hMargin + vMargin / 2f);
        });

        bannerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });

        startAutoSlide();
    }

    private void updateDots(int selected) {
        if (bannerDots == null) return;
        for (int i = 0; i < bannerDots.getChildCount(); i++) {
            bannerDots.getChildAt(i).setBackgroundResource(
                    i == selected ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        }
    }

    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (bannerPager == null) return;
            int next = (bannerPager.getCurrentItem() + 1) % BANNER_ITEMS.length;
            bannerPager.setCurrentItem(next, true);
            bannerHandler.postDelayed(this, 2000);
        }
    };

    private void startAutoSlide() {
        bannerHandler.removeCallbacks(bannerRunnable);
        bannerHandler.postDelayed(bannerRunnable, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAutoSlide();
        selectTab(btnHome);
        loadUserProfile();
        // Cập nhật status đơn hàng theo thời gian mỗi khi mở app
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            com.example.finalapp.utils.OrderStatusUpdater.checkAllForUser(uid);
        }
    }

    private void setupReviews() {
        rvReviews = findViewById(R.id.rvReviews);
        if (rvReviews == null) return;

        // Nội dung review cố định, tên sẽ được thay bằng tên thật từ Firebase
        String[] texts  = { getString(R.string.review1_text), getString(R.string.review2_text),
                             getString(R.string.review3_text), getString(R.string.review4_text) };
        String[] dates  = { getString(R.string.review1_date), getString(R.string.review2_date),
                             getString(R.string.review3_date), getString(R.string.review4_date) };
        int[]    stars  = { 5, 5, 4, 5 };
        int[]    colors = { 0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFFE91E63 };
        String[] fallbackNames = { getString(R.string.review1_name), getString(R.string.review2_name),
                                   getString(R.string.review3_name), getString(R.string.review4_name) };

        List<ReviewItem> reviews = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            reviews.add(new ReviewItem(fallbackNames[i], stars[i], texts[i], dates[i], colors[i]));
        }

        ReviewAdapter reviewAdapter = new ReviewAdapter(reviews);
        LinearLayoutManager reviewLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvReviews.setLayoutManager(reviewLayoutManager);
        rvReviews.setNestedScrollingEnabled(false);
        rvReviews.setAdapter(reviewAdapter);

        rvReviews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                scaleReviewItems(rv, reviewLayoutManager);
            }
        });
        rvReviews.post(() -> scaleReviewItems(rvReviews, reviewLayoutManager));

        // Tên Việt random thực tế
        String[] namePool = {
            "Minh Tuấn", "Thu Hằng", "Quốc Bảo", "Lan Anh", "Hữu Nghĩa",
            "Thanh Thảo", "Đức Khải", "Mỹ Linh", "Văn Hùng", "Ngọc Trâm",
            "Phúc An", "Bích Ngọc", "Trọng Nhân", "Khánh Linh", "Hoàng Nam"
        };
        java.util.Collections.shuffle(java.util.Arrays.asList(namePool));
        for (int i = 0; i < reviews.size(); i++) {
            reviews.get(i).name = namePool[i];
        }
        reviewAdapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        edtSearch = findViewById(R.id.edtSearch);
        cardSearchResults = findViewById(R.id.cardSearchResults);
        if (edtSearch == null || cardSearchResults == null) return;

        RecyclerView rvResults = cardSearchResults.findViewById(R.id.rvSearchResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        searchResultAdapter = new SearchResultAdapter(searchResults, product -> {
            hideSearchResults();
            edtSearch.setText("");
            edtSearch.clearFocus();
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
        rvResults.setAdapter(searchResultAdapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString().trim());
            }
        });

        edtSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideSearchResults();
        });
    }

    private void filterSearch(String query) {
        searchResults.clear();
        if (query.isEmpty()) { hideSearchResults(); return; }
        String lower = query.toLowerCase();
        for (Product p : allProducts) {
            String n = p.getLocalizedName() != null ? p.getLocalizedName().toLowerCase() : "";
            String nEn = p.getName() != null ? p.getName().toLowerCase() : "";
            if (n.contains(lower) || nEn.contains(lower)) {
                searchResults.add(p);
                if (searchResults.size() >= 8) break;
            }
        }
        if (searchResults.isEmpty()) { hideSearchResults(); return; }
        searchResultAdapter.notifyDataSetChanged();
        positionAndShowDropdown();
    }

    private void positionAndShowDropdown() {
        if (cardSearchResults == null || edtSearch == null) return;
        int[] loc = new int[2];
        edtSearch.getLocationInWindow(loc);
        int topPx = loc[1] + edtSearch.getHeight() - (int)(24 * getResources().getDisplayMetrics().density);
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        cardSearchResults.getLayoutParams();
        lp.topMargin = topPx;
        cardSearchResults.setLayoutParams(lp);
        cardSearchResults.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        if (cardSearchResults != null) cardSearchResults.setVisibility(View.GONE);
    }

    private void initViews() {
        txtUserName = findViewById(R.id.txtUserName);
        tvGreeting  = findViewById(R.id.tvGreeting);
        updateGreeting();
        btnProfile = findViewById(R.id.btnProfile);
        btnCartTab = findViewById(R.id.layoutBottomCart);
        btnCartHeader = findViewById(R.id.btnCartHeader);
        btnShop = findViewById(R.id.btnShop);
        btnHome = findViewById(R.id.btnHome);
        btnCommunity = findViewById(R.id.btnCommunity);
        btnNotification = findViewById(R.id.btnNotification);

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v ->
                startActivity(new Intent(MainFinalActivity.this, NotificationListActivity.class)));
        }

        if (btnCartHeader != null) {
            btnCartHeader.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, CartActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        View btnChatHeader = findViewById(R.id.btnChatHeader);
        if (btnChatHeader != null) {
            btnChatHeader.setOnClickListener(v ->
                startActivity(new Intent(MainFinalActivity.this, ChatActivity.class)));
        }

        setupBottomNavHighlight();

        // Best Sellers
        rvBestSellers = findViewById(R.id.rvBestSellers);
        bestSellersList = new ArrayList<>();
        bestSellersAdapter = new ProductAdapter(bestSellersList, true, product -> {
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        }, (product, sourceView, img) -> {
            addToCart(product);
            animateFlyToCart(img);
        });
        rvBestSellers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBestSellers.setNestedScrollingEnabled(false);
        rvBestSellers.setAdapter(bestSellersAdapter);

        // New Arrivals
        rvNewArrivals = findViewById(R.id.rvNewArrivals);
        newArrivalsList = new ArrayList<>();
        newArrivalsAdapter = new ProductAdapter(newArrivalsList, true, product -> {
            Intent intent = new Intent(MainFinalActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        }, (product, sourceView, img) -> {
            addToCart(product);
            animateFlyToCart(img);
        });
        rvNewArrivals.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNewArrivals.setNestedScrollingEnabled(false);
        rvNewArrivals.setAdapter(newArrivalsAdapter);

        // Category cards
        View cardCat1 = findViewById(R.id.cardCat1);
        View cardCat2 = findViewById(R.id.cardCat2);
        View cardCat3 = findViewById(R.id.cardCat3);
        View cardCat4 = findViewById(R.id.cardCat4);
        View cardCta  = findViewById(R.id.cardCta);

        if (cardCat1 != null) cardCat1.setOnClickListener(v -> openCategory("Bonsai trong nhà"));
        if (cardCat2 != null) cardCat2.setOnClickListener(v -> openCategory("Bonsai ngoài trời"));
        if (cardCat3 != null) cardCat3.setOnClickListener(v -> openCategory("Cây cho người mới"));
        if (cardCat4 != null) cardCat4.setOnClickListener(v -> openCategory("Bộ sưu tập hiếm"));
        if (cardCta  != null) cardCta.setOnClickListener(v -> openProduct());

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (btnHome != null) btnHome.setOnClickListener(v -> { /* already home */ });

        if (btnCommunity != null) {
            btnCommunity.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, AboutActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        View.OnClickListener openCart = v -> {
            startActivity(new Intent(MainFinalActivity.this, CartActivity.class));
            overridePendingTransition(0, 0);
        };
        if (btnCartTab != null) btnCartTab.setOnClickListener(openCart);

        if (btnShop != null) {
            btnShop.setOnClickListener(v -> {
                startActivity(new Intent(MainFinalActivity.this, ProductActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    private void openProduct() {
        startActivity(new Intent(this, ProductActivity.class));
        overridePendingTransition(0, 0);
    }

    private void openCategory(String categoryName) {
        Intent intent = new Intent(this, CategoryProductActivity.class);
        intent.putExtra(CategoryProductActivity.EXTRA_CATEGORY_NAME, categoryName);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void selectTab(View selected) {
        int[] tabIds = {R.id.btnHome, R.id.btnShop, R.id.layoutBottomCart, R.id.btnCommunity, R.id.btnProfile};
        for (int id : tabIds) {
            View tab = findViewById(id);
            if (tab != null) tab.setSelected(tab == selected);
        }
    }

    private void setupBottomNavHighlight() {
        selectTab(btnHome);
    }

    private void scaleReviewItems(RecyclerView rv, LinearLayoutManager lm) {
        int center = rv.getWidth() / 2;
        for (int i = lm.findFirstVisibleItemPosition(); i <= lm.findLastVisibleItemPosition(); i++) {
            View child = lm.findViewByPosition(i);
            if (child == null) continue;
            float childCenter = child.getLeft() + child.getWidth() / 2f;
            float distance = Math.abs(center - childCenter);
            float maxDist = rv.getWidth() / 2f;
            float fraction = Math.min(distance / maxDist, 1f);
            float scale = 1f - 0.08f * fraction;
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(1f - 0.3f * fraction);
        }
    }

    private void setupFirebase() {
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://finalapp-c65a2-default-rtdb.firebaseio.com/");
        productsRef = db.getReference("products");
        usersRef = db.getReference("users");
    }

    private void updateGreeting() {
        if (tvGreeting == null) return;
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "CHÀO BUỔI SÁNG,";
        } else if (hour < 18) {
            greeting = "CHÀO BUỔI CHIỀU,";
        } else {
            greeting = "CHÀO BUỔI TỐI,";
        }
        tvGreeting.setText(greeting);
    }

    private void loadUserProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || txtUserName == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = null;
                        if (snapshot.exists()) {
                            name = snapshot.child("name").getValue(String.class);

                            // Load avatar
                            String avatarUrl = snapshot.child("avatarUrl").getValue(String.class);
                            android.widget.ImageView ivHomeAvatar = findViewById(R.id.ivHomeAvatar);
                            if (ivHomeAvatar != null) {
                                EditProfileActivity.loadAvatarInto(avatarUrl, ivHomeAvatar);
                            }
                        }
                        if (name == null || name.isEmpty()) {
                            com.google.firebase.auth.FirebaseUser user =
                                    FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                                name = user.getDisplayName();
                            }
                        }
                        if (name != null && !name.isEmpty()) {
                            txtUserName.setText(name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bestSellersList.clear();
                newArrivalsList.clear();
                allProducts.clear();

                int count = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        product.id = data.getKey();
                        allProducts.add(product);
                        if (count < 6) {
                            bestSellersList.add(product);
                        } else if (newArrivalsList.size() < 4) {
                            newArrivalsList.add(product);
                        }
                        count++;
                    }
                }
                bestSellersAdapter.notifyDataSetChanged();
                newArrivalsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Error: " + error.getMessage());
            }
        });
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
                    if (item != null) quantity = item.quantity + 1;
                }
                cartRef.setValue(new CartItem(product.id, quantity))
                        .addOnSuccessListener(aVoid -> showCartToast())
                        .addOnFailureListener(e -> Toast.makeText(MainFinalActivity.this, getString(R.string.error_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show());
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
}
