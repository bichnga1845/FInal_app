package com.example.finalapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final int TYPE_USER = 0;
    private static final int TYPE_SHOP = 1;
    private static final int IMAGE_SIZE = 512;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach;
    private MessageAdapter adapter;
    private DatabaseReference chatRef;
    private String uid;
    private String userAvatarUrl = null;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) sendImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        // Header nhận padding top (status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_header), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });
        // Input bar lên trên bàn phím khi bàn phím mở
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_input_bar), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime  = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(bars.bottom, ime.bottom);
            v.setPadding(bars.left, 8, bars.right, bottom + 8);
            return insets;
        });

        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }

        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(uid);

        setupProductCard();

        rvMessages = findViewById(R.id.rvMessages);
        etMessage  = findViewById(R.id.etMessage);
        btnSend    = findViewById(R.id.btnSend);
        btnAttach  = findViewById(R.id.btnAttach);

        adapter = new MessageAdapter(new ArrayList<>(), uid, null);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        rvMessages.setAdapter(adapter);

        // Load avatar user từ Firebase
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("avatarUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                        userAvatarUrl = s.getValue(String.class);
                        adapter.setUserAvatarUrl(userAvatarUrl);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnAttach.setOnClickListener(v -> imagePicker.launch("image/*"));

        loadMessages();
    }

    private void setupProductCard() {
        Intent i = getIntent();
        String productName = i.getStringExtra("PRODUCT_NAME");
        if (productName == null) return; // không có sản phẩm → ẩn card

        View cardView = findViewById(R.id.includeProductCard);
        cardView.setVisibility(View.VISIBLE);

        TextView tvName  = cardView.findViewById(R.id.tvProductName);
        TextView tvPrice = cardView.findViewById(R.id.tvProductPrice);
        TextView tvOld   = cardView.findViewById(R.id.tvProductOldPrice);
        ImageView ivThumb = cardView.findViewById(R.id.ivProductThumb);

        tvName.setText(productName);

        double price = i.getDoubleExtra("PRODUCT_PRICE", 0);
        double oldPrice = i.getDoubleExtra("PRODUCT_OLD_PRICE", 0);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###đ");
        tvPrice.setText(df.format(price));
        if (oldPrice > 0 && oldPrice > price) {
            tvOld.setVisibility(View.VISIBLE);
            tvOld.setText(df.format(oldPrice));
            tvOld.setPaintFlags(tvOld.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvOld.setVisibility(View.GONE);
        }

        String imageUrl = i.getStringExtra("PRODUCT_IMAGE");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(ivThumb);
        }
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> msgs = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage m = ds.getValue(ChatMessage.class);
                    if (m != null) msgs.add(m);
                }
                adapter.setData(msgs);
                if (!msgs.isEmpty()) rvMessages.scrollToPosition(msgs.size() - 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        etMessage.setText("");
        pushMessage(text, "text");
    }

    private void sendImage(Uri uri) {
        new Thread(() -> {
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, IMAGE_SIZE, IMAGE_SIZE, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String b64 = "data:image/jpeg;base64," +
                        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                runOnUiThread(() -> pushMessage(b64, "image"));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Không thể gửi ảnh", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void pushMessage(String content, String type) {
        String key = chatRef.push().getKey();
        if (key == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", uid);
        msg.put("text", content);
        msg.put("type", type);
        msg.put("timestamp", System.currentTimeMillis());
        chatRef.child(key).setValue(msg);
    }

    // ===== Model =====
    public static class ChatMessage {
        public String senderId;
        public String text;
        public String type; // "text" | "image"
        public long timestamp;
        public ChatMessage() {}
    }

    // ===== Adapter =====
    static class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_USER_TEXT  = 0;
        private static final int TYPE_SHOP_TEXT  = 1;

        private List<ChatMessage> data;
        private final String myUid;
        private String userAvatarUrl;

        MessageAdapter(List<ChatMessage> data, String myUid, String userAvatarUrl) {
            this.data          = data;
            this.myUid         = myUid;
            this.userAvatarUrl = userAvatarUrl;
        }

        void setData(List<ChatMessage> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        void setUserAvatarUrl(String url) {
            this.userAvatarUrl = url;
            notifyDataSetChanged();
        }

        private boolean isUser(int position) {
            return myUid.equals(data.get(position).senderId);
        }

        // Avatar chỉ hiện ở tin cuối cùng của mỗi nhóm sender liên tiếp
        private boolean isLastInGroup(int position) {
            if (position == data.size() - 1) return true;
            return !data.get(position + 1).senderId.equals(data.get(position).senderId);
        }

        // Thêm margin top lớn hơn khi đổi sender
        private boolean isFirstInGroup(int position) {
            if (position == 0) return true;
            return !data.get(position - 1).senderId.equals(data.get(position).senderId);
        }

        @Override
        public int getItemViewType(int position) {
            return isUser(position) ? TYPE_USER_TEXT : TYPE_SHOP_TEXT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = (viewType == TYPE_USER_TEXT)
                    ? R.layout.item_message_user : R.layout.item_message_shop;
            View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new MsgVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage msg = data.get(position);
            MsgVH h = (MsgVH) holder;

            boolean isImg = "image".equals(msg.type);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            if (isImg && h.tvMessage != null) {
                // Hiển thị ảnh dạng thumbnail text tạm
                h.tvMessage.setText("[Hình ảnh]");
            } else if (h.tvMessage != null) {
                h.tvMessage.setText(msg.text);
            }

            if (h.tvTime != null) {
                h.tvTime.setText(sdf.format(new Date(msg.timestamp)));
            }

            // Avatar + Time: chỉ hiện ở tin cuối nhóm
            boolean showAvatar = isLastInGroup(position);
            if (h.cvAvatar != null) {
                h.cvAvatar.setVisibility(showAvatar ? View.VISIBLE : View.INVISIBLE);
            }
            if (h.tvTime != null) {
                h.tvTime.setVisibility(showAvatar ? View.VISIBLE : View.GONE);
            }

            // Load avatar user thật (nếu có)
            if (showAvatar && isUser(position) && h.ivAvatar != null) {
                if (userAvatarUrl != null && !userAvatarUrl.isEmpty()) {
                    EditProfileActivity.loadAvatarInto(userAvatarUrl, h.ivAvatar);
                }
            }

            // Margin top: lớn hơn khi bắt đầu nhóm mới
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            lp.topMargin = isFirstInGroup(position) ? 12 : 2;
            holder.itemView.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class MsgVH extends RecyclerView.ViewHolder {
            TextView tvMessage, tvTime;
            CardView cvAvatar;
            ImageView ivAvatar;

            MsgVH(View v) {
                super(v);
                tvMessage = v.findViewById(R.id.tvMessage);
                tvTime    = v.findViewById(R.id.tvTime);
                cvAvatar  = v.findViewById(R.id.cvAvatar);
                ivAvatar  = v.findViewById(R.id.ivAvatar);
            }
        }
    }
}
