package com.example.varsivo.util

import com.example.varsivo.api.Bursary

/**
 * Decides which bursaries the Bursaries screen lists. Pure Kotlin so it can be unit tested.
 */
object BursaryFilter {

    /** The three bursaries shown by default, matching the wireframe. */
    val wireframeBursaries = listOf(
        Bursary(id = "wf1", name = "Sasol Bursary", closingDate = "2 Days left"),
        Bursary(id = "wf2", name = "NSFAS", closingDate = "31 May"),
        Bursary(id = "wf3", name = "Deloitte", closingDate = "30 June")
    )

    /**
     * - Default view: only the wireframe bursaries.
     * - "View more" or an active search: wireframe + everything the API returned, so a search
     *   for something that only exists in the API list never looks broken.
     * Then filter by [query] (case-insensitive match on the name).
     */
    fun visibleBursaries(
        apiBursaries: List<Bursary>,
        showAll: Boolean,
        query: String
    ): List<Bursary> {
        val searching = query.isNotBlank()
        val pool = if (showAll || searching) wireframeBursaries + apiBursaries else wireframeBursaries
        return pool.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
}
