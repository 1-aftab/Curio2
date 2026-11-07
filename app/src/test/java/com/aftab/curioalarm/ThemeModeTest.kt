package com.aftab.curioalarm

import com.aftab.curioalarm.ui.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun parseRoundTripsEveryMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.parse(mode.name))
        }
    }

    @Test
    fun parseFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("gold"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("dark"))   // case-sensitive on purpose
    }
}
