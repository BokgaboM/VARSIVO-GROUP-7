package com.example.varsivo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApsCalculatorTest {

    @Test fun markToPoint_usesTheSevenPointScale() {
        assertEquals(7, ApsCalculator.markToPoint(100))
        assertEquals(7, ApsCalculator.markToPoint(80))
        assertEquals(6, ApsCalculator.markToPoint(79))
        assertEquals(6, ApsCalculator.markToPoint(70))
        assertEquals(5, ApsCalculator.markToPoint(60))
        assertEquals(4, ApsCalculator.markToPoint(50))
        assertEquals(3, ApsCalculator.markToPoint(40))
        assertEquals(2, ApsCalculator.markToPoint(30))
        assertEquals(1, ApsCalculator.markToPoint(29))
        assertEquals(1, ApsCalculator.markToPoint(0))
    }

    @Test fun calculate_sixSubjectsAddsThePoints() {
        // 80,70,60,50,40,30 -> 7+6+5+4+3+2 = 27
        assertEquals(27, ApsCalculator.calculate(listOf(80, 70, 60, 50, 40, 30)))
    }

    @Test fun calculate_scalesFewerSubjectsToSix() {
        // Three subjects, all 7 points -> average 7 * 6 = 42
        assertEquals(42, ApsCalculator.calculate(listOf(90, 85, 80)))
    }

    @Test fun calculate_returnsNullWhenNoMarksEntered() {
        assertNull(ApsCalculator.calculate(emptyList()))
    }

    @Test fun isEligible_comparesAgainstTheMinimum() {
        assertTrue(ApsCalculator.isEligible(30, 30))
        assertTrue(ApsCalculator.isEligible(35, 30))
        assertFalse(ApsCalculator.isEligible(29, 30))
    }

    @Test fun exploreData_isNotEmpty() {
        assertTrue(ExploreData.institutions.isNotEmpty())
        assertTrue(ExploreData.careers.isNotEmpty())
    }
}
