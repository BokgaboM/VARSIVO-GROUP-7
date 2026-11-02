package com.example.varsivo.api

import retrofit2.http.GET

interface BursaryApiService {

    @GET("bursaries")
    suspend fun getBursaries(): List<Bursary>
}