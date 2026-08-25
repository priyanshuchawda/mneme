package com.mneme.app.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifTextTriageTest {
    @Test
    fun onlyWhatsAppPackages() {
        assertTrue(NotifTextTriage.isWhatsApp("com.whatsapp"))
        assertTrue(NotifTextTriage.isWhatsApp("com.whatsapp.w4b"))
        assertFalse(NotifTextTriage.isWhatsApp("com.instagram.android"))
        assertFalse(NotifTextTriage.isWhatsApp(null))
    }

    @Test
    fun dropsSystemNoiseAndOk() {
        assertNull(NotifTextTriage.candidate("WhatsApp", "Checking for new messages"))
        assertNull(NotifTextTriage.candidate("Sam", "ok"))
        assertNull(NotifTextTriage.candidate("Sam", "📷"))
    }

    @Test
    fun keepsDeadlineMessage() {
        val c = NotifTextTriage.candidate(
            "Class Group",
            "Reminder: submit the assignment by tomorrow for the lab exam",
        )
        assertNotNull(c)
        assertTrue(c!!.contains("Class Group"))
    }
}

class NotifQueueLogicTest {
    @Test
    fun offerDrainRoundTrip() {
        var json = "[]"
        json = NotifQueueLogic.offer(json, "a")
        json = NotifQueueLogic.offer(json, "b")
        json = NotifQueueLogic.offer(json, "b") // dedupe tail
        assertEquals(2, NotifQueueLogic.size(json))
        val (batch, rest) = NotifQueueLogic.drain(json, 1)
        assertEquals(listOf("a"), batch)
        assertEquals(1, NotifQueueLogic.size(rest))
        val (batch2, rest2) = NotifQueueLogic.drain(rest, 5)
        assertEquals(listOf("b"), batch2)
        assertEquals(0, NotifQueueLogic.size(rest2))
    }

    @Test
    fun capsMax() {
        var json = "[]"
        for (i in 1..20) json = NotifQueueLogic.offer(json, "m$i", max = 3)
        assertEquals(3, NotifQueueLogic.size(json))
        val (batch, _) = NotifQueueLogic.drain(json, 10)
        assertEquals(listOf("m18", "m19", "m20"), batch)
    }
}
