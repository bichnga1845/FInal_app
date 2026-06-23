package com.example.finalapp.models;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// Model dia chi giao hang - map data tu Firebase node /addresses/{uid}/{addressId}
public class Address implements Serializable {

    @Exclude
    public String addressId;

    public String receiverName;
    public String phone;
    public String detail;
    public boolean isDefault;
    public long createdAt;

    public Address() {}

    public Address(String receiverName, String phone, String detail, boolean isDefault) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.detail = detail;
        this.isDefault = isDefault;
        this.createdAt = System.currentTimeMillis();
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("receiverName", receiverName);
        result.put("phone", phone);
        result.put("detail", detail);
        result.put("isDefault", isDefault);
        result.put("createdAt", createdAt);
        return result;
    }
}
