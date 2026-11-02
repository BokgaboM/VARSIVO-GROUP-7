package com.example.varsivo.data

import com.example.varsivo.api.RemoteUser
import com.example.varsivo.api.UserApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Tests registration, login, password change and profile update using in-memory fakes, so no
 * phone or internet connection is needed (this is what GitHub Actions runs on every push).
 */
class AuthRepositoryTest {

    private lateinit var store: FakeAccountStore
    private lateinit var api: FakeUserApi
    private lateinit var repo: AuthRepository

    @Before fun setUp() {
        store = FakeAccountStore()
        api = FakeUserApi()
        repo = AuthRepository(store, api)
    }

    // ---------- registration ----------

    @Test fun register_hashesThePasswordBeforeSavingLocallyAndOnline() = runTest {
        val outcome = repo.register("Thabo Mokoena", "Rosebank High", "thabo01", "Password1")

        assertEquals(RegisterOutcome.SAVED_AND_SYNCED, outcome)
        val local = store.getCredentials()
        assertNotNull(local)
        assertNotEquals("Password1", local!!.passwordHash)          // never stored in plain text
        assertEquals(64, local.passwordHash.length)                  // 256-bit PBKDF2 hash as hex
        assertEquals(1, api.users.size)
        assertEquals(local.passwordHash, api.users[0].passwordHash)  // the same hash goes to the API
        assertEquals(local.salt, api.users[0].salt)
        assertEquals("1", store.remoteId)
    }

    @Test fun register_keepsTheAccountOnThePhoneWhenOffline() = runTest {
        api.offline = true

        val outcome = repo.register("Thabo Mokoena", "", "thabo01", "Password1")

        assertEquals(RegisterOutcome.SAVED_OFFLINE, outcome)
        assertNotNull(store.getCredentials())
        assertTrue(api.users.isEmpty())
        assertNull(store.remoteId)
    }

    @Test fun register_refusesAStudentIdThatAlreadyExistsOnline() = runTest {
        api.users.add(RemoteUser("9", "thabo01", "Someone", "School", "00", "11"))

        val outcome = repo.register("Thabo Mokoena", "", "THABO01", "Password1")

        assertEquals(RegisterOutcome.ALREADY_EXISTS, outcome)
        assertNull(store.getCredentials())
    }

    // ---------- login ----------

    @Test fun login_succeedsWithTheCorrectPassword() = runTest {
        repo.register("Thabo", "", "thabo01", "Password1")

        assertEquals(LoginOutcome.SUCCESS, repo.login("thabo01", "Password1"))
        assertTrue(store.isLoggedIn)
    }

    @Test fun login_isNotCaseSensitiveOnTheStudentId() = runTest {
        repo.register("Thabo", "", "Thabo01", "Password1")

        assertEquals(LoginOutcome.SUCCESS, repo.login("  thabo01 ", "Password1"))
    }

    @Test fun login_failsWithTheWrongPassword() = runTest {
        repo.register("Thabo", "", "thabo01", "Password1")

        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("thabo01", "WrongPass9"))
        assertFalse(store.isLoggedIn)
    }

    @Test fun login_failsForAnUnknownStudentId() = runTest {
        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("nobody1", "Password1"))
        assertFalse(store.isLoggedIn)
    }

    @Test fun login_worksOnAnotherPhoneUsingTheHostedAccount() = runTest {
        repo.register("Thabo", "Rosebank High", "thabo01", "Password1")

        // A second phone: empty local storage, same hosted database.
        val otherStore = FakeAccountStore()
        val otherRepo = AuthRepository(otherStore, api)

        assertEquals(LoginOutcome.SUCCESS, otherRepo.login("thabo01", "Password1"))
        assertEquals("Thabo", otherStore.name)
        assertTrue(otherStore.isLoggedIn)
    }

    @Test fun logout_endsTheSessionButKeepsTheAccount() = runTest {
        repo.register("Thabo", "", "thabo01", "Password1")
        repo.login("thabo01", "Password1")

        repo.logout()

        assertFalse(store.isLoggedIn)
        assertNotNull(store.getCredentials())
    }

    // ---------- change password ----------

    @Test fun changePassword_rejectsAWrongCurrentPassword() = runTest {
        repo.register("Thabo", "", "thabo01", "Password1")

        assertEquals(ChangePasswordOutcome.WRONG_CURRENT_PASSWORD, repo.changePassword("nope", "NewPass22"))
    }

    @Test fun changePassword_updatesLocalAndHostedHashAndTheNewPasswordWorks() = runTest {
        repo.register("Thabo", "", "thabo01", "Password1")

        assertEquals(ChangePasswordOutcome.SUCCESS, repo.changePassword("Password1", "NewPass22"))

        assertEquals(store.getCredentials()!!.passwordHash, api.users[0].passwordHash)
        assertEquals(LoginOutcome.SUCCESS, repo.login("thabo01", "NewPass22"))
        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("thabo01", "Password1"))
    }

    @Test fun changePassword_withoutAnAccountReportsNoAccount() = runTest {
        assertEquals(ChangePasswordOutcome.NO_ACCOUNT, repo.changePassword("a", "b"))
    }

    // ---------- profile (Settings screen) ----------

    @Test fun updateProfile_savesLocallyAndOnline() = runTest {
        repo.register("Thabo", "Old School", "thabo01", "Password1")

        val synced = repo.updateProfile("  Thabo M  ", "New School")

        assertTrue(synced)
        assertEquals("Thabo M", store.name)
        assertEquals("New School", store.schoolName)
        assertEquals("Thabo M", api.users[0].name)
        assertEquals("New School", api.users[0].schoolName)
    }

    @Test fun updateProfile_stillSavesLocallyWhenOffline() = runTest {
        repo.register("Thabo", "Old School", "thabo01", "Password1")
        api.offline = true

        val synced = repo.updateProfile("Thabo M", "New School")

        assertFalse(synced)
        assertEquals("Thabo M", store.name)
    }
}

/** In-memory stand-in for [UserStore] (which needs an Android Context). */
class FakeAccountStore : AccountStore {
    private var hash: String? = null
    private var salt: String? = null
    private var id: String = ""

    override val studentId: String get() = id
    override var name: String = ""
    override var schoolName: String = ""
    override var remoteId: String? = null
    override var isLoggedIn: Boolean = false

    override fun saveUser(
        name: String,
        schoolName: String,
        studentId: String,
        passwordHash: String,
        salt: String,
        remoteId: String?
    ) {
        this.name = name
        this.schoolName = schoolName
        this.id = studentId
        this.hash = passwordHash
        this.salt = salt
        this.remoteId = remoteId
        this.isLoggedIn = false
    }

    override fun getCredentials(): Credentials? {
        val h = hash
        val s = salt
        return if (id.isEmpty() || h.isNullOrEmpty() || s.isNullOrEmpty()) null else Credentials(id, h, s)
    }

    override fun updatePassword(passwordHash: String, salt: String) {
        this.hash = passwordHash
        this.salt = salt
    }
}

/** In-memory stand-in for the hosted MockAPI "users" resource. */
class FakeUserApi : UserApi {
    val users = mutableListOf<RemoteUser>()
    var offline = false

    override suspend fun createUser(user: RemoteUser): RemoteUser {
        if (offline) throw IOException("offline")
        val saved = user.copy(id = (users.size + 1).toString())
        users.add(saved)
        return saved
    }

    // Like MockAPI, this is a "contains" search.
    override suspend fun findUser(studentId: String): List<RemoteUser> {
        if (offline) throw IOException("offline")
        return users.filter { it.studentId.contains(studentId, ignoreCase = true) }
    }

    override suspend fun updateUser(id: String, user: RemoteUser): RemoteUser {
        if (offline) throw IOException("offline")
        val index = users.indexOfFirst { it.id == id }
        if (index < 0) throw IOException("404")
        users[index] = user
        return user
    }
}
