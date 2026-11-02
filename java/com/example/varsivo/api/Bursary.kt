package com.example.varsivo.api

/**
 * A bursary as returned by the hosted REST API (GET /bursaries).
 * Gson bypasses Kotlin's null-safety, so a field the API sends as `null` can end up null even though
 * it is declared non-null; [sanitised] turns those into empty strings before the UI touches them.
 */
data class Bursary(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val provider: String = "",
    val eligibility: String = "",
    val closingDate: String = ""
) {
    fun sanitised(): Bursary = Bursary(
        id = orEmpty(id),
        name = orEmpty(name),
        description = orEmpty(description),
        provider = orEmpty(provider),
        eligibility = orEmpty(eligibility),
        closingDate = orEmpty(closingDate)
    )

    private companion object {
        // Takes String? so the null check is real (a plain `x ?: ""` on a String can be optimised away).
        fun orEmpty(value: String?): String = value ?: ""
    }
}
