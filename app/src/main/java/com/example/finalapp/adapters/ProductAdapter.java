package com.example.finalapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.finalapp.R;
import com.example.finalapp.models.Product;

import java.text.DecimalFormat;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;
    private OnAddToCartClickListener addToCartListener;
    private boolean isHorizontal = false;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(Product product, View sourceView, ImageView productImage);
    }

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener, OnAddToCartClickListener addToCartListener) {
        this.productList = productList;
        this.listener = listener;
        this.addToCartListener = addToCartListener;
    }

    public ProductAdapter(List<Product> productList, boolean isHorizontal, OnProductClickListener listener) {
        this.productList = productList;
        this.isHorizontal = isHorizontal;
        this.listener = listener;
    }

    public ProductAdapter(List<Product> productList, boolean isHorizontal, OnProductClickListener listener, OnAddToCartClickListener addToCartListener) {
        this.productList = productList;
        this.isHorizontal = isHorizontal;
        this.listener = listener;
        this.addToCartListener = addToCartListener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_product_horizontal : R.layout.item_product;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.txtName.setText(product.getLocalizedName());
        
        DecimalFormat df = new DecimalFormat("#,###đ");
        holder.txtPrice.setText(df.format(product.getPrice()));
        
        if (holder.txtOldPrice != null) {
            holder.txtOldPrice.setText(df.format(product.getOldPrice()));
        }
        
        if (holder.txtSpecies != null) {
            holder.txtSpecies.setText(product.species != null ? product.species : holder.itemView.getContext().getString(R.string.str_species_default));
        }

        // Clear previous image to prevent showing same image
        Glide.with(holder.itemView.getContext()).clear(holder.imgProduct);

        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.mipmap.ic_banner)
                    .error(R.mipmap.ic_banner)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.mipmap.ic_banner);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && product != null) {
                listener.onProductClick(product);
            }
        });

        if (holder.btnAddCart != null) {
            holder.btnAddCart.setOnClickListener(v -> {
                if (addToCartListener != null && product != null) {
                    addToCartListener.onAddToCartClick(product, v, holder.imgProduct);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct, btnAddCart;
        TextView txtName, txtPrice, txtOldPrice, txtSpecies;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnAddCart = itemView.findViewById(R.id.btnAddCart);
            if (btnAddCart == null) {
                btnAddCart = itemView.findViewById(R.id.btnQuickAdd);
            }
            txtName = itemView.findViewById(R.id.txtProductName);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
            txtOldPrice = itemView.findViewById(R.id.txtProductOldPrice);
            txtSpecies = itemView.findViewById(R.id.txtProductSpecies);
        }
    }
}