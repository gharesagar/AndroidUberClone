package com.example.sagar.androiduberclone.common;

import com.example.sagar.androiduberclone.remote.IGoogleApi;
import com.example.sagar.androiduberclone.remote.RetrofitClient;


public class Common {
    public static final String baseUrl="https://maps.googleapis.com";
    public static IGoogleApi getGoogleApi(){
        return RetrofitClient.getClient(baseUrl).create(IGoogleApi.class);
    }
}
