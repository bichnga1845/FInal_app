package com.example.finalapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.models.Order;
import com.example.finalapp.utils.OrderStatusUpdater;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderTrackingActivity extends AppCompatActivity {

    private static final String[] TAB_KEYS = {
            "all", "Pending", "Processing", "Shipped", "Delivered", "Refund", "Cancelled"
    };

    private LinearLayout tabContainer;
    private RecyclerView rvOrders;
    private TextView tvEmpty;

    private final List<Order> allOrders = new ArrayList<>();
    private OrderListAdapter adapter;
    private String currentTab = "all";
    private final TextView[] tabViews = new TextView[TAB_KEYS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_tracking);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.order_tracking_header), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        tabContainer = findViewById(R.id.tabContainer);
        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupRecyclerView();
        setupTabs();
        loadOrders();
    }

    private void setupTabs() {
        String[] tabLabels = {
                getString(R.string.str_tab_all),
                getString(R.string.str_tab_pending),
                getString(R.string.str_tab_processing),
                getString(R.string.str_tab_shipped),
                getString(R.string.str_tab_delivered),
                getString(R.string.str_tab_refund),
                getString(R.string.str_tab_cancelled)
        };

        for (int i = 0; i < tabLabels.length; i++) {
            final String tabKey = TAB_KEYS[i];
            TextView tab = new TextView(this);
            tab.setText(tabLabels[i]);
            tab.setTextSize(13f);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(28, 0, 28, 0);
            tab.setTypeface(null, Typeface.NORMAL);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            tab.setLayoutParams(lp);
            tab.setBackgroundResource(android.R.color.transparent);

            tab.setOnClickListener(v -> selectTab(tabKey));
            tabContainer.addView(tab);
            tabViews[i] = tab;
        }
        selectTab("all");
    }

    private void selectTab(String tabKey) {
        currentTab = tabKey;
        int cream = getResources().getColor(R.color.text_button, getTheme());
        int creamAlpha = 0x99FFFFFF & cream; // mờ hơn cho tab không chọn

        for (int i = 0; i < TAB_KEYS.length; i++) {
            boolean selected = TAB_KEYS[i].equals(tabKey);
            tabViews[i].setTextColor(selected ? cream : 0xAAFFFFFF);
            tabViews[i].setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

            // Gạch chân tab đang chọn
            if (selected) {
                tabViews[i].setPaintFlags(
                        tabViews[i].getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            } else {
                tabViews[i].setPaintFlags(
                        tabViews[i].getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            }
        }
        filterAndShow();
    }

    private void setupRecyclerView() {
        adapter = new OrderListAdapter(new ArrayList<>(), order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.orderId);
            startActivity(intent);
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Cập nhật status trước khi load để UI luôn hiển thị đúng
        OrderStatusUpdater.checkAllForUser(uid);

        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("userId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allOrders.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Order order = ds.getValue(Order.class);
                            if (order != null) {
                                if (order.orderId == null) order.orderId = ds.getKey();
                                allOrders.add(order);
                            }
                        }
                        // Sắp xếp mới nhất lên đầu
                        Collections.sort(allOrders, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                        filterAndShow();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void filterAndShow() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : allOrders) {
            if ("all".equals(currentTab) || currentTab.equals(o.status)) {
                filtered.add(o);
            }
        }
        adapter.setData(filtered);
        rvOrders.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ===== Inline Adapter =====
    static class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.VH> {

        interface OnClickListener { void onClick(Order order); }

        private List<Order> data;
        private final OnClickListener listener;

        OrderListAdapter(List<Order> data, OnClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        void setData(List<Order> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_tracking, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Order order = data.get(position);
            String shortId = order.orderId != null && order.orderId.length() >= 6
                    ? order.orderId.substring(0, 6).toUpperCase() : order.orderId;
            h.tvOrderId.setText(h.itemView.getContext().getString(R.string.str_order_id_prefix) + shortId);

            // Trạng thái + màu
            String statusText = getStatusLabel(h.itemView, order.status);
            int statusColor = getStatusColor(h.itemView, order.status);
            h.tvOrderStatus.setText(statusText);
            h.tvOrderStatus.setTextColor(statusColor);

            int count = order.items != null ? order.items.size() : 0;
            h.tvItemCount.setText(h.itemView.getContext().getString(R.string.str_product_count, count));

            DecimalFormat df = new DecimalFormat("#,###đ");
            h.tvTotalAmount.setText(df.format(order.totalAmount));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            h.tvOrderDate.setText(sdf.format(new Date(order.timestamp)));

            h.itemView.setOnClickListener(v -> listener.onClick(order));
        }

        @Override
        public int getItemCount() { return data.size(); }

        private String getStatusLabel(View v, String status) {
            if (status == null) return "";
            switch (status) {
                case "Pending":    return v.getContext().getString(R.string.str_tab_pending);
                case "Processing": return v.getContext().getString(R.string.str_tab_processing);
                case "Shipped":    return v.getContext().getString(R.string.str_tab_shipped);
                case "Delivered":  return v.getContext().getString(R.string.str_tab_delivered);
                case "Refund":     return v.getContext().getString(R.string.str_tab_refund);
                case "Cancelled":  return v.getContext().getString(R.string.str_tab_cancelled);
                default: return status;
            }
        }

        private int getStatusColor(View v, String status) {
            if (status == null) return 0xFF888888;
            switch (status) {
                case "Delivered":  return v.getContext().getResources().getColor(R.color.primary, null);
                case "Cancelled":
                case "Refund":     return 0xFFE53935;
                case "Shipped":    return 0xFF1976D2;
                default:           return 0xFFF57C00;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvOrderStatus, tvItemCount, tvTotalAmount, tvOrderDate;
            VH(View v) {
                super(v);
                tvOrderId      = v.findViewById(R.id.tvOrderId);
                tvOrderStatus  = v.findViewById(R.id.tvOrderStatus);
                tvItemCount    = v.findViewById(R.id.tvItemCount);
                tvTotalAmount  = v.findViewById(R.id.tvTotalAmount);
                tvOrderDate    = v.findViewById(R.id.tvOrderDate);
            }
        }
    }
}
