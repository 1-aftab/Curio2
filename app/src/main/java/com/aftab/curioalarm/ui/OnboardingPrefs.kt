package com.aftab.curioalarm.ui

import android.content.Context

/** First-run gate. Consent is recorded separately so Settings can say so honestly. */
object OnboardingPrefs {
    private const val PREFS = "curio_ui"
    private const val KEY_DONE = "onboarding_done"
    private const val KEY_CONSENT = "consent_accepted"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun hasConsent(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CONSENT, false)

    fun acceptConsent(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CONSENT, true).apply()
    }

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DONE, true).apply()
    }
}
