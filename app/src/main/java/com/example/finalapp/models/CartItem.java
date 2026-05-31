package com.example.finalapp.models;

public class CartItem {
    public String productId;
    public int quantity;
    public Product product; // Dữ liệu sản phẩm đi kèm để hiển thị

    public CartItem() {}

    public CartItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}