package com.aftab.curioalarm.alarm

/**
 * Pure decisions for the ringing watchdog. Kept free of Android types so unit tests can cover
 * the "power button killed the process" case without a device.
 */
internal object RingGuard {
    /**
     * A session id is still owed, but the service is not currently playing that alarm.
     * Timer rings use id 0, so a missing service id must not be confused with "timer is playing".
     */
    fun needsRevive(sessionId: Int?, serviceRingingId: Int?): Boolean =
        sessionId != null && serviceRingingId != sessionId
}
