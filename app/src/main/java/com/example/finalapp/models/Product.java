package com.example.finalapp.models;

import java.util.HashMap;
import java.util.Map;

public class Product {
    public String productId;
    public String categoryId;
    public String name;
    public String description;
    public double price;
    public String imageUrl;
    public int stock;
    public long timestamp;

    // Constructor trống cho Firebase
    public Product() {}

    public Product(String productId, String categoryId, String name, String description, double price, String imageUrl, int stock) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.timestamp = System.currentTimeMillis();
    }

    // Chuyển đối tượng sang Map để dễ dàng lưu trữ
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("categoryId", categoryId);
        result.put("name", name);
        result.put("description", description);
        result.put("price", price);
        result.put("imageUrl", imageUrl);
        result.put("stock", stock);
        result.put("timestamp", timestamp);
        return result;
    }
}