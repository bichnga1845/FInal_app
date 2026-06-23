package com.example.finalapp.utils;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VietnamAddressApi {

    public static class Item {
        @SerializedName("code") public int code;
        @SerializedName("name") public String name;

        public Item() {} // Required for GSON
        public Item(int code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return name; }
    }

    // Wrap models for complex JSON structures
    public static class DistrictResponse {
        @SerializedName("districts") public List<Item> districts;
    }

    public static class WardResponse {
        @SerializedName("wards") public List<Item> wards;
    }

    public interface Callback { void onResult(List<Item> items); }

    private static final String BASE_URL = "https://provinces.open-api.vn/api/";
    private static ApiService apiService;

    private static ApiService getApiService() {
        if (apiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    public static void getProvinces(Callback callback) {
        getApiService().getProvinces().enqueue(new retrofit2.Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body());
                } else {
                    callback.onResult(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                callback.onResult(new ArrayList<>());
            }
        });
    }

    public static void getDistricts(int provinceCode, Callback callback) {
        getApiService().getDistricts(provinceCode, 2).enqueue(new retrofit2.Callback<DistrictResponse>() {
            @Override
            public void onResponse(Call<DistrictResponse> call, Response<DistrictResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().districts != null) {
                    callback.onResult(response.body().districts);
                } else {
                    callback.onResult(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<DistrictResponse> call, Throwable t) {
                callback.onResult(new ArrayList<>());
            }
        });
    }

    public static void getWards(int districtCode, Callback callback) {
        getApiService().getWards(districtCode, 2).enqueue(new retrofit2.Callback<WardResponse>() {
            @Override
            public void onResponse(Call<WardResponse> call, Response<WardResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().wards != null) {
                    callback.onResult(response.body().wards);
                } else {
                    callback.onResult(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<WardResponse> call, Throwable t) {
                callback.onResult(new ArrayList<>());
            }
        });
    }
}
