package com.example.varsivo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationStatusTest {

    @Test fun next_cyclesPendingSubmittedOfferAndBack() {
        assertEquals(ApplicationStatus.SUBMITTED, ApplicationStatus.PENDING.next())
        assertEquals(ApplicationStatus.OFFER, ApplicationStatus.SUBMITTED.next())
        assertEquals(ApplicationStatus.PENDING, ApplicationStatus.OFFER.next())
    }
}
