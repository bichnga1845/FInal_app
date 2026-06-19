package com.example.finalapp.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VietnamAddressApi {

    public static class Item {
        public final int code;
        public final String name;
        public Item(int code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return name; }
    }

    public interface Callback { void onResult(List<Item> items); }

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String BASE = "https://provinces.open-api.vn/api";

    public static void getProvinces(Callback callback) {
        fetch(BASE + "/p/", response -> {
            try {
                JSONArray arr = new JSONArray(response);
                List<Item> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    list.add(new Item(p.getInt("code"), p.getString("name")));
                }
                mainHandler.post(() -> callback.onResult(list));
            } catch (Exception e) { mainHandler.post(() -> callback.onResult(new ArrayList<>())); }
        });
    }

    public static void getDistricts(int provinceCode, Callback callback) {
        fetch(BASE + "/p/" + provinceCode + "?depth=2", response -> {
            try {
                JSONObject obj = new JSONObject(response);
                JSONArray arr = obj.getJSONArray("districts");
                List<Item> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject d = arr.getJSONObject(i);
                    list.add(new Item(d.getInt("code"), d.getString("name")));
                }
                mainHandler.post(() -> callback.onResult(list));
            } catch (Exception e) { mainHandler.post(() -> callback.onResult(new ArrayList<>())); }
        });
    }

    public static void getWards(int districtCode, Callback callback) {
        fetch(BASE + "/d/" + districtCode + "?depth=2", response -> {
            try {
                JSONObject obj = new JSONObject(response);
                JSONArray arr = obj.getJSONArray("wards");
                List<Item> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject w = arr.getJSONObject(i);
                    list.add(new Item(w.getInt("code"), w.getString("name")));
                }
                mainHandler.post(() -> callback.onResult(list));
            } catch (Exception e) { mainHandler.post(() -> callback.onResult(new ArrayList<>())); }
        });
    }

    // fetch trả raw string, dùng cho provinces và overloaded district/ward
    private static void fetch(String urlStr, RawCallback callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                callback.onResponse(sb.toString());
            } catch (Exception e) { callback.onResponse("[]"); }
        });
    }

    private interface RawCallback { void onResponse(String response); }
}
