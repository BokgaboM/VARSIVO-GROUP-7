package com.example.varsivo.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PartOfDayTest {

    @Test fun morning_isMidnightUpToEleven() {
        assertEquals(PartOfDay.MORNING, PartOfDay.fromHour(0))
        assertEquals(PartOfDay.MORNING, PartOfDay.fromHour(11))
    }

    @Test fun afternoon_isNoonUpToFive() {
        assertEquals(PartOfDay.AFTERNOON, PartOfDay.fromHour(12))
        assertEquals(PartOfDay.AFTERNOON, PartOfDay.fromHour(17))
    }

    @Test fun evening_isSixOnwards() {
        assertEquals(PartOfDay.EVENING, PartOfDay.fromHour(18))
        assertEquals(PartOfDay.EVENING, PartOfDay.fromHour(23))
    }
}
