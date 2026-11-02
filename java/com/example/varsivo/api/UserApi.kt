package com.example.varsivo.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * A Varsivo account as stored in the hosted "users" table.
 * NOTE: there is no plain-text password here — only the random salt and the PBKDF2 hash.
 */
data class RemoteUser(
    val id: String? = null,
    val studentId: String,
    val name: String,
    val schoolName: String,
    val salt: String,
    val passwordHash: String
)

/** REST endpoints of the hosted database (MockAPI resource "users"). */
interface UserApi {

    @POST("users")
    suspend fun createUser(@Body user: RemoteUser): RemoteUser

    /** MockAPI filters with a "contains" match, so callers must still compare studentId exactly. */
    @GET("users")
    suspend fun findUser(@Query("studentId") studentId: String): List<RemoteUser>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: RemoteUser): RemoteUser
}
