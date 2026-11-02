package com.example.varsivo.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class GeocodeResult(
    val lat: String,
    val lon: String,
    val display_name: String
)

/**
 * OpenStreetMap's free Nominatim geocoding service — turns a place name
 * into real coordinates. No API key needed, which is why it's a common
 * pairing with MapLibre for prototypes like this one. Nominatim's usage
 * policy asks for a descriptive User-Agent, which RetrofitClient sets.
 */
interface GeocodingApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = "VarsivoApp/1.0"
    ): List<GeocodeResult>
}
