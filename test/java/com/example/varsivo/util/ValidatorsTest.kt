package com.example.varsivo.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Input validation is what stops bad data reaching the API, so it is tested thoroughly. */
class ValidatorsTest {

    // ----- name -----
    @Test fun name_acceptsNormalNames() {
        assertTrue(Validators.isValidName("Thabo Mokoena"))
        assertTrue(Validators.isValidName("  Li  "))
    }

    @Test fun name_rejectsBlankShortOrNumericOnly() {
        assertFalse(Validators.isValidName(""))
        assertFalse(Validators.isValidName("   "))
        assertFalse(Validators.isValidName("A"))
        assertFalse(Validators.isValidName("12"))
    }

    // ----- student id -----
    @Test fun studentId_acceptsLettersDigitsDotUnderscoreDash() {
        assertTrue(Validators.isValidStudentId("stu1234"))
        assertTrue(Validators.isValidStudentId("  Thabo_01  "))
        assertTrue(Validators.isValidStudentId("a.b-c1"))
    }

    @Test fun studentId_rejectsTooShortTooLongOrSpaces() {
        assertFalse(Validators.isValidStudentId("abc"))
        assertFalse(Validators.isValidStudentId("a".repeat(21)))
        assertFalse(Validators.isValidStudentId("stu 1234"))
        assertFalse(Validators.isValidStudentId("stu@1234"))
        assertFalse(Validators.isValidStudentId(""))
    }

    // ----- password -----
    @Test fun password_needsEightCharsLetterAndDigit() {
        assertTrue(Validators.isStrongPassword("Password1"))
        assertTrue(Validators.isStrongPassword("12345678a"))
    }

    @Test fun password_rejectsWeakOnes() {
        assertFalse(Validators.isStrongPassword("Pass1"))          // too short
        assertFalse(Validators.isStrongPassword("passwordonly"))   // no digit
        assertFalse(Validators.isStrongPassword("1234567890"))     // no letter
        assertFalse(Validators.isStrongPassword(""))
    }

    // ----- email -----
    @Test fun email_acceptsValidAddresses() {
        assertTrue(Validators.isValidEmail("student@gmail.com"))
        assertTrue(Validators.isValidEmail(" first.last+tag@school.co.za "))
    }

    @Test fun email_rejectsInvalidAddresses() {
        assertFalse(Validators.isValidEmail("student"))
        assertFalse(Validators.isValidEmail("student@"))
        assertFalse(Validators.isValidEmail("@gmail.com"))
        assertFalse(Validators.isValidEmail("student@gmail"))
        assertFalse(Validators.isValidEmail(""))
    }

    // ----- marks -----
    @Test fun mark_mustBeBetween0And100() {
        assertTrue(Validators.isValidMark(0))
        assertTrue(Validators.isValidMark(100))
        assertFalse(Validators.isValidMark(-1))
        assertFalse(Validators.isValidMark(101))
    }
}
