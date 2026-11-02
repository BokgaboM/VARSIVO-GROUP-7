package com.example.varsivo.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Creates the Retrofit REST clients used by the app.
 *
 *  - MockAPI (hosted by us): "users", "applications" and "bursaries" resources
 *  - Hipo Labs Universities API (public): list of South African universities
 *  - OpenStreetMap Nominatim (public): turns a university name into map coordinates
 *
 * Reference: Square (2024) *Retrofit* https://square.github.io/retrofit/
 */
object RetrofitClient {

    /** Our hosted REST API + database (MockAPI project). */
    private const val MOCK_API_BASE_URL =
        "https://6aa34e30e7ae868cdf7aceb1.mockapi.io/"

    private val mockApi: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MOCK_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** GET/POST/PUT users — registration, login and settings. */
    val userApi: UserApi by lazy { mockApi.create(UserApi::class.java) }

    /** POST applications — university and bursary applications. */
    val applicationApi: ApplicationApi by lazy { mockApi.create(ApplicationApi::class.java) }

    /** GET bursaries. */
    val bursaryApi: BursaryApiService by lazy { mockApi.create(BursaryApiService::class.java) }

    /** GET universities for South Africa (public API). */
    val universityApi: UniversityApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://universities.hipolabs.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UniversityApi::class.java)
    }

    /** Place name -> coordinates (public API). */
    val geocodingApi: GeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)
    }
}
