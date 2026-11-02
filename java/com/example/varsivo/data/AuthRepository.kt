package com.example.varsivo.data

import android.util.Log
import com.example.varsivo.SecurityUtils
import com.example.varsivo.api.RemoteUser
import com.example.varsivo.api.UserApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RegisterOutcome { SAVED_AND_SYNCED, SAVED_OFFLINE, ALREADY_EXISTS }

enum class LoginOutcome { SUCCESS, INVALID_CREDENTIALS }

enum class ChangePasswordOutcome { SUCCESS, WRONG_CURRENT_PASSWORD, NO_ACCOUNT }

/**
 * Registration, login and password change.
 *
 * - Passwords are hashed with PBKDF2 ([SecurityUtils]) before they are saved or sent anywhere.
 * - The account is kept on the device ([UserStore]) so the app works offline, and copied to the
 *   hosted "users" table through the REST API so it can also be viewed/used from another device.
 *   If the hosted API is not reachable the account is simply kept locally.
 */
class AuthRepository(
    private val userStore: AccountStore,
    private val userApi: UserApi
) {

    suspend fun register(
        name: String,
        schoolName: String,
        studentId: String,
        password: String
    ): RegisterOutcome = withContext(Dispatchers.IO) {
        val id = studentId.trim()

        // 1) Is this Student ID already taken online? (best effort)
        if (findRemote(id) != null) {
            Log.w(TAG, "Registration refused: $id already exists online")
            return@withContext RegisterOutcome.ALREADY_EXISTS
        }

        // 2) Hash the password (CPU heavy, so off the main thread)
        val salt = SecurityUtils.generateSalt()
        val hash = withContext(Dispatchers.Default) { SecurityUtils.hashPassword(password, salt) }

        // 3) Save locally (always) ...
        userStore.saveUser(name.trim(), schoolName.trim(), id, hash, salt, remoteId = null)

        // 4) ... and try to save on the hosted database
        try {
            val created = userApi.createUser(
                RemoteUser(
                    studentId = id,
                    name = name.trim(),
                    schoolName = schoolName.trim(),
                    salt = salt,
                    passwordHash = hash
                )
            )
            userStore.remoteId = created.id
            Log.i(TAG, "Account $id created on the hosted database (remote id ${created.id})")
            RegisterOutcome.SAVED_AND_SYNCED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Account $id saved locally only: ${e.message}")
            RegisterOutcome.SAVED_OFFLINE
        }
    }

    suspend fun login(studentId: String, password: String): LoginOutcome = withContext(Dispatchers.IO) {
        val id = studentId.trim()
        val local = userStore.getCredentials()

        if (local != null && local.studentId.equals(id, ignoreCase = true)) {
            val ok = withContext(Dispatchers.Default) {
                SecurityUtils.verifyPassword(password, local.passwordHash, local.salt)
            }
            return@withContext finishLogin(ok, id)
        }

        // Not the account stored on this phone: look it up on the hosted database.
        val remote = findRemote(id)
        if (remote != null) {
            val ok = withContext(Dispatchers.Default) {
                SecurityUtils.verifyPassword(password, remote.passwordHash, remote.salt)
            }
            if (ok) {
                userStore.saveUser(
                    name = remote.name,
                    schoolName = remote.schoolName,
                    studentId = remote.studentId,
                    passwordHash = remote.passwordHash,
                    salt = remote.salt,
                    remoteId = remote.id
                )
            }
            return@withContext finishLogin(ok, id)
        }

        Log.w(TAG, "Login failed: unknown student ID $id")
        LoginOutcome.INVALID_CREDENTIALS
    }

    suspend fun changePassword(current: String, new: String): ChangePasswordOutcome = withContext(Dispatchers.IO) {
        val local = userStore.getCredentials() ?: return@withContext ChangePasswordOutcome.NO_ACCOUNT

        val ok = withContext(Dispatchers.Default) {
            SecurityUtils.verifyPassword(current, local.passwordHash, local.salt)
        }
        if (!ok) return@withContext ChangePasswordOutcome.WRONG_CURRENT_PASSWORD

        val salt = SecurityUtils.generateSalt()
        val hash = withContext(Dispatchers.Default) { SecurityUtils.hashPassword(new, salt) }
        userStore.updatePassword(hash, salt)
        Log.i(TAG, "Password changed for ${local.studentId}")

        // Best-effort update of the hosted record
        val remoteId = userStore.remoteId
        if (remoteId != null) {
            try {
                userApi.updateUser(
                    remoteId,
                    RemoteUser(
                        id = remoteId,
                        studentId = local.studentId,
                        name = userStore.name,
                        schoolName = userStore.schoolName,
                        salt = salt,
                        passwordHash = hash
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Hosted password update failed: ${e.message}")
            }
        }
        ChangePasswordOutcome.SUCCESS
    }

    /**
     * Settings -> "Save changes": stores the new profile on the phone and, when this account also
     * exists on the hosted database, updates it there too (PUT /users/{id}).
     * Returns true when the hosted copy was updated.
     */
    suspend fun updateProfile(name: String, schoolName: String): Boolean = withContext(Dispatchers.IO) {
        userStore.name = name.trim()
        userStore.schoolName = schoolName.trim()
        Log.i(TAG, "Profile saved locally for ${userStore.studentId}")

        val local = userStore.getCredentials() ?: return@withContext false
        val remoteId = userStore.remoteId ?: return@withContext false
        try {
            userApi.updateUser(
                remoteId,
                RemoteUser(
                    id = remoteId,
                    studentId = local.studentId,
                    name = userStore.name,
                    schoolName = userStore.schoolName,
                    salt = local.salt,
                    passwordHash = local.passwordHash
                )
            )
            Log.i(TAG, "Profile updated on the hosted database")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Hosted profile update failed: ${e.message}")
            false
        }
    }

    /** Ends the session. The account stays on the phone so the student can log in again. */
    fun logout() {
        userStore.isLoggedIn = false
        Log.i(TAG, "Logged out")
    }

    private fun finishLogin(ok: Boolean, studentId: String): LoginOutcome {
        return if (ok) {
            userStore.isLoggedIn = true
            Log.i(TAG, "Login success for $studentId")
            LoginOutcome.SUCCESS
        } else {
            Log.w(TAG, "Login failed: wrong password for $studentId")
            LoginOutcome.INVALID_CREDENTIALS
        }
    }

    /** Looks the Student ID up on the hosted database. Null when not found or unreachable. */
    private suspend fun findRemote(studentId: String): RemoteUser? {
        return try {
            userApi.findUser(studentId).firstOrNull { it.studentId.equals(studentId, ignoreCase = true) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // MockAPI answers 404 for "no matches" and a network error means offline: both = null.
            Log.d(TAG, "Remote lookup for $studentId returned nothing: ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
