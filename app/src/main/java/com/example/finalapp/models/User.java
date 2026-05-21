package com.example.finalapp.models;

import java.util.HashMap;
import java.util.Map;

// Model user - map data tu Firebase Realtime DB node /users/{uid}
public class User {
    public String uid;
    public String name;
    public String email;
    public String phone;
    public String address;
    public String role;
    public String avatar;
    public long createdAt;

    public User() {}

    public User(String uid, String name, String email, String phone, String address, String role, String avatar) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.avatar = avatar;
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("email", email);
        result.put("phone", phone);
        result.put("address", address);
        result.put("role", role);
        result.put("avatar", avatar);
        result.put("createdAt", createdAt);
        return result;
    }
}
