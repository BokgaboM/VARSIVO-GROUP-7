package com.example.varsivo.api

import retrofit2.http.Body
import retrofit2.http.POST

/** A submitted university / bursary application, as sent to the hosted "applications" table. */
data class RemoteApplication(
    val id: String? = null,
    val studentId: String,
    val type: String,
    val institution: String,
    val programme: String,
    val status: String,
    val applicantName: String,
    val email: String,
    val createdAt: Long
)

/** REST endpoint of the hosted database (MockAPI resource "applications"). */
interface ApplicationApi {

    @POST("applications")
    suspend fun createApplication(@Body application: RemoteApplication): RemoteApplication
}
