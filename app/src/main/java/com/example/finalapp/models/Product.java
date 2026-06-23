package com.example.finalapp.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.Locale;

@IgnoreExtraProperties
public class Product implements Serializable {
    public String id;
    public String name;
    public String nameVi;
    public String description;
    public String descriptionVi;
    public double price;
    
    public double originalPrice;
    public double oldPrice;

    public double getVndPrice() {
        return price;
    }

    public double getVndOldPrice() {
        return getOldPrice();
    }

    public String primaryImage;
    public String imageUrl;
    public String image_url;
    
    public String category;
    public String categoryId;
    public String category_id;
    
    // Additional fields for detail screen
    public String species;
    public String age;
    public String height;
    public String careLevel;
    public String waterInfo;
    public String lightInfo;
    public String size; // kích thước chậu (VD: "5cm", "15 x 10cm")

    public boolean isPot() {
        String cat = getCategoryId().toLowerCase();
        String n = (name != null ? name : "").toLowerCase();
        return cat.contains("pot") || cat.contains("chậu") || cat.contains("dish") || cat.contains("bowl")
            || n.contains(" pot ") || n.contains(" pot\n") || n.endsWith(" pot")
            || n.contains("dish") || n.contains("stoneware") || n.contains("ceramic pot")
            || n.contains("bonsai pot") || n.contains("cube pot");
    }

    public Product() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    public String getLocalizedName() {
        if (isVietnamese() && nameVi != null && !nameVi.isEmpty()) return nameVi;
        return name != null ? name : "";
    }

    public String getLocalizedDescription() {
        if (isVietnamese() && descriptionVi != null && !descriptionVi.isEmpty()) return descriptionVi;
        return description != null ? description : "";
    }

    private static boolean isVietnamese() {
        return Locale.getDefault().getLanguage().equals("vi");
    }

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