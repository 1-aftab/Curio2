package com.aftab.curioalarm

import com.aftab.curioalarm.alarm.RingGuard
import com.aftab.curioalarm.data.AlertMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertModeTest {

    @Test
    fun parseRoundTripsEveryMode() {
        AlertMode.entries.forEach { mode ->
            assertEquals(mode, AlertMode.parse(mode.name))
        }
    }

    @Test
    fun parseFallsBackToSoundAndVibration() {
        assertEquals(AlertMode.SOUND_AND_VIBRATION, AlertMode.parse(null))
        assertEquals(AlertMode.SOUND_AND_VIBRATION, AlertMode.parse(""))
        assertEquals(AlertMode.SOUND_AND_VIBRATION, AlertMode.parse("silent"))
    }

    @Test
    fun legacyVibrateFlagMapsOntoTheNewChoice() {
        assertEquals(
            AlertMode.SOUND_AND_VIBRATION,
            AlertMode.fromStored(alertModeName = null, hasAlertMode = false, legacyVibrate = true)
        )
        assertEquals(
            AlertMode.SOUND,
            AlertMode.fromStored(alertModeName = null, hasAlertMode = false, legacyVibrate = false)
        )
    }

    @Test
    fun storedModeWinsOverTheLegacyFlag() {
        assertEquals(
            AlertMode.VIBRATION,
            AlertMode.fromStored("VIBRATION", hasAlertMode = true, legacyVibrate = true)
        )
    }

    @Test
    fun vibrationOnlyDoesNotPlaySound() {
        assertFalse(AlertMode.VIBRATION.playsSound)
        assertTrue(AlertMode.VIBRATION.vibrates)
        assertTrue(AlertMode.SOUND.playsSound)
        assertFalse(AlertMode.SOUND.vibrates)
        assertTrue(AlertMode.SOUND_AND_VIBRATION.playsSound)
        assertTrue(AlertMode.SOUND_AND_VIBRATION.vibrates)
    }
}

class RingGuardTest {

    @Test
    fun reviveWhenSessionIsOwedAndServiceIsNotPlayingIt() {
        assertTrue(RingGuard.needsRevive(sessionId = 4, serviceRingingId = null))
        assertTrue(RingGuard.needsRevive(sessionId = 4, serviceRingingId = 9))
        // Timer rings use id 0. A dead service must still count as needing a revive.
        assertTrue(RingGuard.needsRevive(sessionId = 0, serviceRingingId = null))
    }

    @Test
    fun noReviveWhenNothingIsOwedOrTheSameAlarmIsPlaying() {
        assertFalse(RingGuard.needsRevive(sessionId = null, serviceRingingId = null))
        assertFalse(RingGuard.needsRevive(sessionId = null, serviceRingingId = 4))
        assertFalse(RingGuard.needsRevive(sessionId = 4, serviceRingingId = 4))
        assertFalse(RingGuard.needsRevive(sessionId = 0, serviceRingingId = 0))
    }
}
