package com.example.varsivo.data

/**
 * What [AuthRepository] needs from local storage. [UserStore] (SharedPreferences) is the real
 * implementation; the unit tests use a simple in-memory fake, so login / registration logic can be
 * tested on the JVM without an Android device.
 */
interface AccountStore {
    val studentId: String
    var name: String
    var schoolName: String
    var remoteId: String?
    var isLoggedIn: Boolean

    fun saveUser(
        name: String,
        schoolName: String,
        studentId: String,
        passwordHash: String,
        salt: String,
        remoteId: String?
    )

    fun getCredentials(): Credentials?

    fun updatePassword(passwordHash: String, salt: String)
}
