package com.aftab.curioalarm.challenge

import android.content.Context
import com.aftab.curioalarm.data.ChallengeType
import com.aftab.curioalarm.data.Difficulty
import kotlin.random.Random

sealed interface Puzzle {
    data class Math(val question: String, val answer: Int) : Puzzle
    data class Memory(
        val sequence: List<String>,
        val alphabet: List<String>,
        val showMillis: Long,
        val attempt: Int
    ) : Puzzle
    data class Phrase(val text: String) : Puzzle
}

object ChallengeEngine {

    private val symbols = listOf("▲", "●", "■", "◆", "★", "✚", "♠", "♥")
    private val digits = (0..9).map { it.toString() }

    private val easyPhrases = listOf(
        "the quiet morning light",
        "coffee tastes better awake",
        "one small step forward",
        "open the window wide",
        "today is a fresh page",
        "stand up and stretch"
    )
    private val mediumPhrases = listOf(
        "a calm mind makes better choices",
        "the early bird is already moving",
        "water the plants before breakfast",
        "keep going because progress is quiet",
        "silver clouds hide the rising sun",
        "wake up and take a deep breath"
    )
    private val hardPhrases = listOf(
        "discipline is choosing what you want most over what you want now",
        "a steady routine builds the freedom that motivation never could",
        "somewhere a kettle is already humming while the city slowly wakes",
        "curiosity keeps the mind young long after the alarm has stopped",
        "every sunrise hands you an unwritten page and a sharpened pencil",
        "the best ideas arrive when you are fully awake and walking outside"
    )

    /** One enabled type = that type; several = random choice per occurrence. */
    fun pick(types: Set<ChallengeType>): ChallengeType =
        types.ifEmpty { setOf(ChallengeType.MATH) }.toList().random()

    fun generate(context: Context, type: ChallengeType, difficulty: Difficulty, attempt: Int): Puzzle =
        when (type) {
            ChallengeType.MATH -> math(difficulty)
            ChallengeType.MEMORY -> memory(difficulty, attempt)
            ChallengeType.PHRASE -> phrase(context, difficulty)
        }

    private fun math(d: Difficulty): Puzzle.Math {
        val r = Random.Default
        return when (d) {
            Difficulty.EASY ->
                if (r.nextBoolean()) {
                    val a = r.nextInt(12, 70)
                    val b = r.nextInt(11, 40)
                    Puzzle.Math("$a + $b", a + b)
                } else {
                    val a = r.nextInt(30, 100)
                    val b = r.nextInt(11, a)
                    Puzzle.Math("$a − $b", a - b)
                }
            Difficulty.MEDIUM -> when (r.nextInt(3)) {
                0 -> {
                    val a = r.nextInt(3, 13)
                    val b = r.nextInt(6, 16)
                    Puzzle.Math("$a × $b", a * b)
                }
                1 -> {
                    val a = r.nextInt(12, 60)
                    val b = r.nextInt(12, 60)
                    val c = r.nextInt(5, 30)
                    Puzzle.Math("$a + $b − $c", a + b - c)
                }
                else -> {
                    val a = r.nextInt(3, 10)
                    val b = r.nextInt(11, 25)
                    val c = r.nextInt(10, 60)
                    Puzzle.Math("$a × $b + $c", a * b + c)
                }
            }
            Difficulty.HARD -> when (r.nextInt(3)) {
                0 -> {
                    val a = r.nextInt(13, 30)
                    val b = r.nextInt(8, 20)
                    val c = r.nextInt(20, 100)
                    Puzzle.Math("$a × $b − $c", a * b - c)
                }
                1 -> {
                    val a = r.nextInt(11, 46)
                    val b = r.nextInt(11, 46)
                    val c = r.nextInt(3, 10)
                    Puzzle.Math("($a + $b) × $c", (a + b) * c)
                }
                else -> {
                    val a = r.nextInt(6, 16)
                    val b = r.nextInt(6, 16)
                    val c = r.nextInt(3, 13)
                    val e = r.nextInt(3, 13)
                    Puzzle.Math("$a × $b + $c × $e", a * b + c * e)
                }
            }
        }
    }

    private fun memory(d: Difficulty, attempt: Int): Puzzle.Memory {
        val length = when (d) {
            Difficulty.EASY -> 4
            Difficulty.MEDIUM -> 6
            Difficulty.HARD -> 8
        }
        val alphabet = if (Random.nextBoolean()) digits else symbols
        val sequence = List(length) { alphabet.random() }
        return Puzzle.Memory(sequence, alphabet, 1000L * length + 1500L, attempt)
    }

    private fun phrase(context: Context, d: Difficulty): Puzzle.Phrase {
        val pool = when (d) {
            Difficulty.EASY -> easyPhrases
            Difficulty.MEDIUM -> mediumPhrases
            Difficulty.HARD -> hardPhrases
        }
        val prefs = context.applicationContext.getSharedPreferences("curio_challenge", Context.MODE_PRIVATE)
        val last = prefs.getString("last_phrase", null)
        val choice = pool.filter { it != last }.random()   // never the same phrase twice in a row
        prefs.edit().putString("last_phrase", choice).apply()
        return Puzzle.Phrase(choice)
    }

    fun normalize(s: String): String = s.trim().replace(Regex("\\s+"), " ").lowercase()
}
