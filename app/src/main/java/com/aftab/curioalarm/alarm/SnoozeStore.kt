package com.aftab.curioalarm.alarm

import android.content.Context

/**
 * How many snoozes this occurrence has used, and when the pending one should fire.
 * The occurrence key is the alarm's lastFiredAt, so a later morning starts the count over.
 */
object SnoozeStore {
    private const val PREFS = "curio_snooze"

    data class Pending(val alarmId: Int, val atMillis: Long)

    fun used(context: Context, alarmId: Int, occurrence: Long): Int {
        val p = prefs(context)
        if (p.getLong(occKey(alarmId), Long.MIN_VALUE) != occurrence) return 0
        return p.getInt(usedKey(alarmId), 0)
    }

    fun noteUsed(context: Context, alarmId: Int, occurrence: Long): Int {
        val next = used(context, alarmId, occurrence) + 1
        prefs(context).edit()
            .putLong(occKey(alarmId), occurrence)
            .putInt(usedKey(alarmId), next)
            .commit()
        return next
    }

    fun setPending(context: Context, alarmId: Int, atMillis: Long) {
        prefs(context).edit().putLong(pendingKey(alarmId), atMillis).commit()
    }

    fun pendingAt(context: Context, alarmId: Int): Long =
        prefs(context).getLong(pendingKey(alarmId), 0L)

    fun clearPending(context: Context, alarmId: Int) {
        prefs(context).edit().remove(pendingKey(alarmId)).commit()
    }

    /** Dismiss, delete, or turning the alarm off. Drops the count and the pending fire. */
    fun clear(context: Context, alarmId: Int) {
        prefs(context).edit()
            .remove(occKey(alarmId))
            .remove(usedKey(alarmId))
            .remove(pendingKey(alarmId))
            .commit()
    }

    fun allPending(context: Context): List<Pending> {
        val out = mutableListOf<Pending>()
        for ((key, value) in prefs(context).all) {
            if (!key.startsWith("pending_") || value !is Long || value <= 0L) continue
            val id = key.removePrefix("pending_").toIntOrNull() ?: continue
            out.add(Pending(id, value))
        }
        return out
    }

    fun soonest(context: Context, now: Long = System.currentTimeMillis()): Pending? =
        allPending(context).filter { it.atMillis > now }.minByOrNull { it.atMillis }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun occKey(id: Int) = "occ_$id"
    private fun usedKey(id: Int) = "used_$id"
    private fun pendingKey(id: Int) = "pending_$id"
}
