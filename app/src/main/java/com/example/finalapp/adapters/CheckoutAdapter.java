package com.example.finalapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalapp.R;
import com.example.finalapp.models.CartItem;

import java.text.DecimalFormat;
import java.util.List;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {

    private final List<CartItem> cartItems;

    public CheckoutAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkout_product, parent, false);
        return new CheckoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item.product != null) {
            holder.txtName.setText(item.product.getName());
            
            String desc = (item.product.species != null ? item.product.species : "Bonsai") + 
                          " | " + (item.product.age != null ? item.product.age : "Nghệ thuật");
            holder.txtDesc.setText(desc);
            
            DecimalFormat df = new DecimalFormat("#,###đ");
            holder.txtPrice.setText(df.format(item.product.getPrice()));
            
            Glide.with(holder.itemView.getContext())
                    .load(item.product.getImageUrl())
                    .placeholder(R.mipmap.ic_banner)
                    .into(holder.imgProduct);
        }
        holder.txtQuantity.setText("x" + item.quantity);
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class CheckoutViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtDesc, txtPrice, txtQuantity;

        public CheckoutViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtDesc = itemView.findViewById(R.id.txtProductDesc);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
        }
    }
}