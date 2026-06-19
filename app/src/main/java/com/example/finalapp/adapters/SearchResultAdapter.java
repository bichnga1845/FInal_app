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
import com.example.finalapp.models.Product;

import java.text.DecimalFormat;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    private final List<Product> list;
    private final OnItemClickListener listener;

    public SearchResultAdapter(List<Product> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Product p = list.get(position);
        h.txtName.setText(p.getLocalizedName());
        h.txtPrice.setText(new DecimalFormat("#,###đ").format(p.getPrice()));
        Glide.with(h.itemView.getContext())
                .load(p.getImageUrl())
                .placeholder(R.mipmap.ic_logo_new)
                .into(h.imgProduct);
        h.itemView.setOnClickListener(v -> listener.onItemClick(p));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtPrice;

        ViewHolder(View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgSearchProduct);
            txtName = v.findViewById(R.id.txtSearchProductName);
            txtPrice = v.findViewById(R.id.txtSearchProductPrice);
        }
    }
}
