package com.mneme.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIntentRouterTest {
    @Test
    fun routesRemember() {
        val i = VoiceIntentRouter.route("remember: buy milk tomorrow")
        assertTrue(i is VoiceIntent.Remember)
        assertEquals("buy milk tomorrow", (i as VoiceIntent.Remember).text)
    }

    @Test
    fun routesAlarm() {
        val i = VoiceIntentRouter.route("set an alarm at 7:30 am")
        assertTrue(i is VoiceIntent.SetAlarm)
        val a = i as VoiceIntent.SetAlarm
        assertEquals(7, a.hour)
        assertEquals(30, a.minute)
    }

    @Test
    fun routesQuery() {
        val i = VoiceIntentRouter.route("when is the lab due")
        assertTrue(i is VoiceIntent.Query)
    }
}
