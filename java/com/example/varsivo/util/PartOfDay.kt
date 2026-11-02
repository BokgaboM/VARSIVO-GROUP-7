package com.example.varsivo.util

/** Used for the Home greeting ("Good Morning" / "Good Afternoon" / "Good Evening"). */
enum class PartOfDay {
    MORNING, AFTERNOON, EVENING;

    companion object {
        /** @param hour hour of day, 0–23 */
        fun fromHour(hour: Int): PartOfDay = when (hour) {
            in 0..11 -> MORNING
            in 12..17 -> AFTERNOON
            else -> EVENING
        }
    }
}
