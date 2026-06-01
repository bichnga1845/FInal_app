package com.example.finalapp.models;

import java.util.List;

public class Order {
    public String orderId;
    public String userId;
    public List<CartItem> items;
    public double subtotal;
    public double shippingFee;
    public double totalAmount;
    public String addressId;
    public String addressDetail;
    public String paymentMethod;
    public String status; // e.g., "Pending", "Processing", "Shipped", "Delivered"
    public long timestamp;

    public Order() {
        // Required for Firebase
    }

    public Order(String userId, List<CartItem> items, double subtotal, double shippingFee, double totalAmount, String addressId, String addressDetail, String paymentMethod) {
        this.userId = userId;
        this.items = items;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.addressId = addressId;
        this.addressDetail = addressDetail;
        this.paymentMethod = paymentMethod;
        this.status = "Pending";
        this.timestamp = System.currentTimeMillis();
    }
}