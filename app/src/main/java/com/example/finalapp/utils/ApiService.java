package com.example.finalapp.utils;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("p/")
    Call<List<VietnamAddressApi.Item>> getProvinces();

    @GET("p/{code}")
    Call<VietnamAddressApi.DistrictResponse> getDistricts(@Path("code") int provinceCode, @Query("depth") int depth);

    @GET("d/{code}")
    Call<VietnamAddressApi.WardResponse> getWards(@Path("code") int districtCode, @Query("depth") int depth);
}
