package com.example.varsivo.api

import retrofit2.http.GET
import retrofit2.http.Query

interface UniversityApi {

    @GET("search")
    suspend fun getUniversities(
        @Query("country") country: String
    ): List<University>
}