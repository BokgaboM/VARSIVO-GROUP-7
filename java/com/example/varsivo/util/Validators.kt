package com.example.varsivo.util

/**
 * Pure input-validation rules, kept free of Android classes so they can be unit tested
 * on the JVM (see ValidatorsTest). The UI layer decides which message to show.
 */
object Validators {

    private val STUDENT_ID_REGEX = Regex("^[A-Za-z0-9._-]{4,20}$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 8

    /** A name needs at least two characters including one letter. */
    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length >= 2 && trimmed.any { it.isLetter() }
    }

    /** Student ID: 4–20 letters, digits, dot, underscore or dash (no spaces). */
    fun isValidStudentId(studentId: String): Boolean =
        STUDENT_ID_REGEX.matches(studentId.trim())

    /** Password: at least 8 characters, with at least one letter and one digit. */
    fun isStrongPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /** NSC subject marks are percentages between 0 and 100. */
    fun isValidMark(mark: Int): Boolean = mark in 0..100
}
