package com.philem.philem.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 에뮬레이터에서 로컬 서버(localhost:10000) 접근용 주소
    // 10.0.2.2는 Android 에뮬레이터에서 호스트 머신의 localhost를 가리킴
    private const val BASE_URL = "http://10.0.2.2:10000/"
    private const val REGION_BASE_URL = "http://10.0.2.2:10001/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val regionRetrofit = Retrofit.Builder()
        .baseUrl(REGION_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val pricingApi: PricingApiService = retrofit.create(PricingApiService::class.java)
    val regionApi: RegionApiService = regionRetrofit.create(RegionApiService::class.java)
}
