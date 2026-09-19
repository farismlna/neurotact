// network/ApiClient.kt

package com.neurotact.coach.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Ganti dengan IP laptop kamu saat testing
    // Kalau test di emulator pakai 10.0.2.2
    // Kalau test di HP fisik pakai IP laptop di jaringan yang sama
    private const val BASE_URL = "http://192.168.100.9:8000/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}