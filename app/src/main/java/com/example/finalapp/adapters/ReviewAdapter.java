package com.example.finalapp.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.R;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    public static class ReviewItem {
        public String name;
        public final int stars;
        public final String content;
        public final String date;
        public final int avatarColor;

        public ReviewItem(String name, int stars, String content, String date, int avatarColor) {
            this.name = name;
            this.stars = stars;
            this.content = content;
            this.date = date;
            this.avatarColor = avatarColor;
        }
    }

    private final List<ReviewItem> items;

    public ReviewAdapter(List<ReviewItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReviewItem item = items.get(position);

        // Avatar circle with first letter
        String initial = item.name.isEmpty() ? "?" : String.valueOf(item.name.charAt(0)).toUpperCase();
        holder.txtAvatar.setText(initial);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(item.avatarColor);
        holder.txtAvatar.setBackground(circle);

        holder.txtName.setText(item.name);

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < item.stars; i++) stars.append("★");
        for (int i = item.stars; i < 5; i++) stars.append("☆");
        holder.txtStars.setText(stars.toString());

        holder.txtContent.setText(item.content);
        holder.txtDate.setText(item.date);

        // Reset scale (scale được set từ scroll listener)
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAvatar, txtName, txtStars, txtContent, txtDate;

        ViewHolder(View v) {
            super(v);
            txtAvatar = v.findViewById(R.id.txtReviewAvatar);
            txtName = v.findViewById(R.id.txtReviewName);
            txtStars = v.findViewById(R.id.txtReviewStars);
            txtContent = v.findViewById(R.id.txtReviewContent);
            txtDate = v.findViewById(R.id.txtReviewDate);
        }
    }
}
