package com.example.finalapp.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    public String productId;
    public int quantity;
    public Product product; // Dữ liệu sản phẩm đi kèm để hiển thị

    public CartItem() {}

    public CartItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}