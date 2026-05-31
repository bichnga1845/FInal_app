package com.example.finalapp.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Product {
    public String id;
    public String name;
    public double price;
    
    public double originalPrice;
    public double oldPrice;

    public String primaryImage;
    public String imageUrl;
    public String image_url;
    
    public String category;
    public String categoryId;
    public String category_id;

    public Product() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    public String getImageUrl() {
        // Check primaryImage first, then imageUrl, then image_url
        if (primaryImage != null && !primaryImage.isEmpty()) {
            return primaryImage.trim();
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl.trim();
        }
        if (image_url != null && !image_url.isEmpty()) {
            return image_url.trim();
        }
        return "";
    }

    public String getCategoryId() {
        String cid = null;
        // Check category (Firebase field name), then categoryId, then category_id
        if (category != null && !category.isEmpty()) {
            cid = category;
        } else if (categoryId != null && !categoryId.isEmpty()) {
            cid = categoryId;
        } else if (category_id != null && !category_id.isEmpty()) {
            cid = category_id;
        }
        return (cid != null) ? cid.trim() : "";
    }

    public double getOldPrice() {
        if (originalPrice != 0) return originalPrice;
        if (oldPrice != 0) return oldPrice;
        return 0;
    }
}