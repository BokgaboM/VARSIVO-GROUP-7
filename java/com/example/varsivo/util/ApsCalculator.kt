package com.example.varsivo.util

import kotlin.math.roundToInt

/**
 * Admission Point Score (APS) maths for the Explore screen.
 *
 * Converts NSC subject percentages to the standard 7-point scale used by South African
 * universities (80–100% = 7 … 0–29% = 1). Scores are always reported on the 0–42 scale
 * (7 points x 6 subjects) so they can be compared with institutions' minimum APS even when
 * a learner entered fewer than six subjects.
 */
object ApsCalculator {

    const val MAX_SUBJECTS = 6

    fun markToPoint(mark: Int): Int = when {
        mark >= 80 -> 7
        mark >= 70 -> 6
        mark >= 60 -> 5
        mark >= 50 -> 4
        mark >= 40 -> 3
        mark >= 30 -> 2
        else -> 1
    }

    /**
     * Average the points of the entered [marks] and scale to six subjects.
     * Returns null when no marks were entered.
     */
    fun calculate(marks: List<Int>): Int? {
        if (marks.isEmpty()) return null
        val average = marks.map { markToPoint(it) }.average()
        return (average * MAX_SUBJECTS).roundToInt()
    }

    fun isEligible(apsScore: Int, minimumAps: Int): Boolean = apsScore >= minimumAps
}

/** An institution with the minimum APS it asks for. */
data class MatchedInstitution(val name: String, val minAps: Int)

/** Prototype data for the Explore screen (a real build would load this from the API). */
object ExploreData {
    val institutions = listOf(
        MatchedInstitution("University of Pretoria", 30),
        MatchedInstitution("Wits University", 32),
        MatchedInstitution("University of Johannesburg", 26),
        MatchedInstitution("Stellenbosch University", 28),
        MatchedInstitution("UNISA", 22)
    )

    val careers = listOf(
        "Software Developer",
        "Computer Science",
        "Economics",
        "Mechanical Engineering",
        "Nursing"
    )
}
