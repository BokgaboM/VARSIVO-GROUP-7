package com.example.varsivo.util

import com.example.varsivo.api.Bursary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BursaryFilterTest {

    private val fromApi = listOf(
        Bursary(id = "1", name = "Funza Lushaka"),
        Bursary(id = "2", name = "Sasol Mining")
    )

    @Test fun defaultView_showsOnlyTheThreeWireframeBursaries() {
        val result = BursaryFilter.visibleBursaries(fromApi, showAll = false, query = "")
        assertEquals(BursaryFilter.wireframeBursaries, result)
    }

    @Test fun viewMore_addsTheApiBursaries() {
        val result = BursaryFilter.visibleBursaries(fromApi, showAll = true, query = "")
        assertEquals(3 + fromApi.size, result.size)
    }

    @Test fun search_findsApiBursariesEvenWithoutViewMore() {
        val result = BursaryFilter.visibleBursaries(fromApi, showAll = false, query = "funza")
        assertEquals(listOf("Funza Lushaka"), result.map { it.name })
    }

    @Test fun search_isCaseInsensitiveAndTrimmed() {
        val result = BursaryFilter.visibleBursaries(fromApi, showAll = false, query = "  NSFAS ")
        assertEquals(listOf("NSFAS"), result.map { it.name })
    }

    @Test fun search_withNoMatchReturnsEmptyList() {
        assertTrue(BursaryFilter.visibleBursaries(fromApi, showAll = true, query = "zzzz").isEmpty())
    }
}
