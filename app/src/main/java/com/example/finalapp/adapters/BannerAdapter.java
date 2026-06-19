package com.example.finalapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.R;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    public static class BannerItem {
        public final int imageRes;
        public final String tag;
        public final String title;
        public final String desc;

        public BannerItem(int imageRes, String tag, String title, String desc) {
            this.imageRes = imageRes;
            this.tag = tag;
            this.title = title;
            this.desc = desc;
        }
    }

    private final List<BannerItem> items;

    public BannerAdapter(List<BannerItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannerItem item = items.get(position);
        holder.imgBanner.setImageResource(item.imageRes);
        holder.txtTag.setText(item.tag);
        holder.txtTitle.setText(item.title);
        holder.txtDesc.setText(item.desc);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;
        TextView txtTag, txtTitle, txtDesc;

        ViewHolder(View v) {
            super(v);
            imgBanner = v.findViewById(R.id.imgBanner);
            txtTag = v.findViewById(R.id.txtBannerTag);
            txtTitle = v.findViewById(R.id.txtBannerTitle);
            txtDesc = v.findViewById(R.id.txtBannerDesc);
        }
    }
}
