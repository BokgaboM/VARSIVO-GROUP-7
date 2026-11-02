package com.example.varsivo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun correctPassword_verifiesSuccessfully() {

        val salt = SecurityUtils.generateSalt()

        val hash = SecurityUtils.hashPassword(
            "Password123",
            salt
        )

        assertTrue(
            SecurityUtils.verifyPassword(
                "Password123",
                hash,
                salt
            )
        )
    }

    @Test
    fun incorrectPassword_doesNotVerify() {

        val salt = SecurityUtils.generateSalt()

        val hash = SecurityUtils.hashPassword(
            "Password123",
            salt
        )

        assertFalse(
            SecurityUtils.verifyPassword(
                "WrongPassword",
                hash,
                salt
            )
        )
    }

    @Test
    fun differentSalts_produceDifferentHashes() {

        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(
            "Password123",
            salt1
        )

        val hash2 = SecurityUtils.hashPassword(
            "Password123",
            salt2
        )

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun samePasswordAndSalt_produceSameHash() {

        val salt = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(
            "Password123",
            salt
        )

        val hash2 = SecurityUtils.hashPassword(
            "Password123",
            salt
        )

        assertTrue(hash1 == hash2)
    }

    @Test
    fun hash_isNeverThePlainPassword_andIs256BitsAsHex() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword("Password123", salt)

        assertNotEquals("Password123", hash)
        assertTrue(hash.length == 64)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun salt_is128BitsAsHex_andDifferentEachTime() {
        val a = SecurityUtils.generateSalt()
        val b = SecurityUtils.generateSalt()

        assertTrue(a.length == 32)
        assertNotEquals(a, b)
    }
}
