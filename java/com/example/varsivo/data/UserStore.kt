package com.example.varsivo.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/** What is needed to check a login: the student ID plus the salted password hash. */
data class Credentials(val studentId: String, val passwordHash: String, val salt: String)

/**
 * Local storage for the signed-in student: profile, settings, session flag and the salted
 * password hash. Backed by SharedPreferences (file "VarsivoUser").
 *
 * The password itself is never stored here — only [Credentials.passwordHash] and the salt.
 */
class UserStore(context: Context) : AccountStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Account ----------

    /** Replaces the stored account (the app keeps one local account at a time). */
    override fun saveUser(
        name: String,
        schoolName: String,
        studentId: String,
        passwordHash: String,
        salt: String,
        remoteId: String?
    ) {
        Log.i(TAG, "Saving local account for studentId=$studentId")
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_SCHOOL, schoolName)
            .putString(KEY_STUDENT_ID, studentId)
            .putString(KEY_HASH, passwordHash)
            .putString(KEY_SALT, salt)
            .putString(KEY_REMOTE_ID, remoteId)
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }

    override fun getCredentials(): Credentials? {
        val id = this.studentId
        val hash = prefs.getString(KEY_HASH, null)
        val salt = prefs.getString(KEY_SALT, null)
        if (id.isEmpty() || hash.isNullOrEmpty() || salt.isNullOrEmpty()) return null
        return Credentials(id, hash, salt)
    }

    override fun updatePassword(passwordHash: String, salt: String) {
        prefs.edit().putString(KEY_HASH, passwordHash).putString(KEY_SALT, salt).apply()
    }

    // ---------- Profile ----------

    /** Student ID. Older builds saved it under the key "email", so fall back to that. */
    override val studentId: String
        get() = prefs.getString(KEY_STUDENT_ID, null)
            ?: prefs.getString(LEGACY_KEY_EMAIL, "")
            ?: ""

    override var name: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    override var schoolName: String
        get() = prefs.getString(KEY_SCHOOL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SCHOOL, value).apply()

    override var remoteId: String?
        get() = prefs.getString(KEY_REMOTE_ID, null)
        set(value) = prefs.edit().putString(KEY_REMOTE_ID, value).apply()

    // ---------- Settings ----------

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    // ---------- Session ----------

    override var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false) && getCredentials() != null
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    companion object {
        private const val TAG = "UserStore"
        private const val PREFS_NAME = "VarsivoUser"

        private const val KEY_NAME = "name"
        private const val KEY_SCHOOL = "schoolName"
        private const val KEY_STUDENT_ID = "studentId"
        private const val LEGACY_KEY_EMAIL = "email"
        private const val KEY_HASH = "passwordHash"
        private const val KEY_SALT = "salt"
        private const val KEY_REMOTE_ID = "remoteId"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS = "notificationsEnabled"
        private const val KEY_LOGGED_IN = "loggedIn"

        const val DEFAULT_LANGUAGE = "English"
        val SUPPORTED_LANGUAGES = listOf("English", "isiZulu", "Afrikaans")
    }
}
