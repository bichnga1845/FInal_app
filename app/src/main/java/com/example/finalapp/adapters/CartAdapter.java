package com.example.finalapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalapp.R;
import com.example.finalapp.models.CartItem;

import java.text.DecimalFormat;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> cartItems;
    private final OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onQuantityChange(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    public CartAdapter(List<CartItem> cartItems, OnCartChangeListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item.product != null) {
            holder.txtName.setText(item.product.getLocalizedName());
            
            // Hiển thị mô tả (loài hoặc tuổi cây)
            String desc = (item.product.species != null ? item.product.species : "") + 
                          (item.product.age != null ? ", " + item.product.age : "");
            holder.txtDesc.setText(desc.isEmpty() ? holder.itemView.getContext().getString(R.string.str_default_product_desc) : desc);
            
            DecimalFormat df = new DecimalFormat("#,###đ");
            holder.txtPrice.setText(df.format(item.product.getVndPrice()));
            
            Glide.with(holder.itemView.getContext())
                    .load(item.product.getImageUrl())
                    .placeholder(R.mipmap.ic_banner)
                    .into(holder.imgProduct);
        }

        holder.txtQuantity.setText(String.valueOf(item.quantity));

        if (listener != null) {
            holder.btnPlus.setOnClickListener(v -> listener.onQuantityChange(item, item.quantity + 1));
            holder.btnMinus.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    listener.onQuantityChange(item, item.quantity - 1);
                }
            });
            holder.btnRemove.setOnClickListener(v -> listener.onRemoveItem(item));
            holder.btnPlus.setVisibility(View.VISIBLE);
            holder.btnMinus.setVisibility(View.VISIBLE);
            holder.btnRemove.setVisibility(View.VISIBLE);
        } else {
            holder.btnPlus.setVisibility(View.GONE);
            holder.btnMinus.setVisibility(View.GONE);
            holder.btnRemove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtDesc, txtPrice, txtQuantity;
        ImageButton btnPlus, btnMinus, btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgCartProduct);
            txtName = itemView.findViewById(R.id.txtCartProductName);
            txtDesc = itemView.findViewById(R.id.txtCartProductDesc); // Đã thêm ánh xạ
            txtPrice = itemView.findViewById(R.id.txtCartProductPrice);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}