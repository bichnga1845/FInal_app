package com.example.finalapp.models;

import java.io.Serializable;

public class Notification implements Serializable {
    public String id;
    public String title;
    public String message;
    public String type; // e.g., "order", "promo", "system"
    public long timestamp;
    public boolean isRead;
    public String targetId; // e.g., orderId or productId if applicable

    public Notification() {
        // Required for Firebase
    }

    public Notification(String id, String title, String message, String type, long timestamp, String targetId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
        this.targetId = targetId;
    }
}
