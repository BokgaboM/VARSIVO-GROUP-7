package com.example.varsivo.data

import android.util.Log
import com.example.varsivo.api.ApplicationApi
import com.example.varsivo.api.RemoteApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline-first applications: every submission is saved to the local SQLite table first and then
 * pushed to the hosted REST API. If the push fails (no network, API down) the row stays flagged
 * "Offline will sync..." and is retried the next time the tracker opens ([syncPending]).
 */
class ApplicationRepository(
    private val db: ApplicationsDb,
    private val api: ApplicationApi,
    private val userStore: UserStore,
) {

    suspend fun getAll(): List<TrackedApplication> = withContext(Dispatchers.IO) { db.getAll() }

    /** Saves a new application locally, then tries to sync it. Returns true when it synced. */
    suspend fun submit(
        type: ApplicationType,
        institution: String,
        programme: String,
        applicantName: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        val app = TrackedApplication(
            type = type,
            institution = institution,
            programme = programme,
            status = ApplicationStatus.SUBMITTED,
            synced = false,
            applicantName = applicantName,
            email = email,
            createdAt = System.currentTimeMillis()
        )
        val id = db.insert(app)
        push(app.copy(id = id))
    }

    /** Tries to upload every row that has not been synced yet. */
    suspend fun syncPending() = withContext(Dispatchers.IO) {
        val pending = db.getUnsynced()
        if (pending.isEmpty()) return@withContext
        Log.i(TAG, "Syncing ${pending.size} pending application(s)")
        pending.forEach { push(it) }
    }

    suspend fun cycleStatus(app: TrackedApplication) = withContext(Dispatchers.IO) {
        db.updateStatus(app.id, app.status.next())
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { db.delete(id) }

    suspend fun clearAll() = withContext(Dispatchers.IO) { db.clear() }

    private suspend fun push(app: TrackedApplication): Boolean = withContext(Dispatchers.IO) {
        try {
            api.createApplication(
                RemoteApplication(
                    studentId = userStore.studentId,
                    type = app.type.name,
                    institution = app.institution,
                    programme = app.programme,
                    status = app.status.name,
                    applicantName = app.applicantName,
                    email = app.email,
                    createdAt = app.createdAt
                )
            )
            db.markSynced(app.id)
            Log.i(TAG, "Application ${app.id} synced to the REST API")
            true
        } catch (e: Exception) {
            // Offline or the hosted "applications" resource is not set up yet: keep it local.
            Log.w(TAG, "Could not sync application ${app.id}: ${e.message}")
            false
        }
    }

    private companion object {
        const val TAG = "ApplicationRepository"
    }
}
