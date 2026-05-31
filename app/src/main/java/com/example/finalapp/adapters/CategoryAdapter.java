package com.example.finalapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.R;
import com.example.finalapp.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.txtName.setText(category.getName());

        // Dynamic icon loading
        String contextPackage = holder.itemView.getContext().getPackageName();
        String iconName;
        
        if ("all".equalsIgnoreCase(category.getId())) {
            iconName = "ic_category_all";
        } else {
            String categoryName = (category.getName() != null) ? category.getName().toLowerCase().trim() : "all";
            // Chuyển ký tự đặc biệt và khoảng trắng thành "_" (ví dụ: "Books & Guides" -> "books_guides")
            String sanitizedName = categoryName.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("__+", "_");
            if (sanitizedName.endsWith("_")) sanitizedName = sanitizedName.substring(0, sanitizedName.length() - 1);
            iconName = "ic_category_" + sanitizedName;
        }
        
        int resID = holder.itemView.getContext().getResources().getIdentifier(iconName, "mipmap", contextPackage);
        if (resID != 0) {
            holder.imgIcon.setImageResource(resID);
        } else {
            // Fallback to default icon if not found
            holder.imgIcon.setImageResource(R.drawable.ic_leaf);
        }

        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.txtName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            holder.imgIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.txtName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_description));
            holder.imgIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.tab_inactive));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgCategory);
            txtName = itemView.findViewById(R.id.txtCategoryName);
        }
    }
}