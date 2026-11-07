package com.aftab.curioalarm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.challenge.ChallengeEngine
import com.aftab.curioalarm.challenge.Puzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Horizontal shake used as gentle feedback for a wrong answer. */
private class Shake(private val anim: Animatable<Float, AnimationVector1D>, private val scope: CoroutineScope) {
    val offset: Int get() = anim.value.roundToInt()
    fun trigger() {
        scope.launch {
            repeat(3) {
                anim.animateTo(14f, tween(45))
                anim.animateTo(-14f, tween(45))
            }
            anim.animateTo(0f, tween(45))
        }
    }
}

@Composable
private fun rememberShake(): Shake {
    val scope = rememberCoroutineScope()
    val anim = remember { Animatable(0f) }
    return remember { Shake(anim, scope) }
}

// ------------------------------------------------------------------ math

@Composable
fun MathChallenge(puzzle: Puzzle.Math, onSolved: () -> Unit) {
    var input by remember(puzzle) { mutableStateOf("") }
    var wrong by remember(puzzle) { mutableStateOf(false) }
    val shake = rememberShake()

    fun submit() {
        if (input.trim().toIntOrNull() == puzzle.answer) {
            onSolved()
        } else {
            wrong = true
            input = ""
            shake.trigger()
        }
    }

    Column(
        Modifier.fillMaxWidth().offset { IntOffset(shake.offset, 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionLabel("Solve to dismiss")
        Text(
            puzzle.question,
            color = Palette.Text,
            fontSize = 40.sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )
        OutlinedTextField(
            value = input,
            onValueChange = { v ->
                input = v.filterIndexed { i, c -> c.isDigit() || (c == '-' && i == 0) }.take(7)
                wrong = false
            },
            placeholder = { Text("Your answer", color = Palette.Muted) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            shape = RoundedCornerShape(18.dp),
            colors = curioFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (wrong) Text("Incorrect. The alarm keeps ringing.", color = Palette.Danger, fontSize = 13.sp)
        PrimaryButton("Confirm", onClick = { submit() }, modifier = Modifier.fillMaxWidth(), enabled = input.isNotBlank())
    }
}

// ------------------------------------------------------------------ phrase

@Composable
fun PhraseChallenge(puzzle: Puzzle.Phrase, onSolved: () -> Unit) {
    var input by remember(puzzle) { mutableStateOf("") }
    var wrong by remember(puzzle) { mutableStateOf(false) }
    val shake = rememberShake()

    fun submit() {
        if (ChallengeEngine.normalize(input) == ChallengeEngine.normalize(puzzle.text)) {
            onSolved()
        } else {
            wrong = true
            shake.trigger()
        }
    }

    Column(
        Modifier.fillMaxWidth().offset { IntOffset(shake.offset, 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionLabel("Type this phrase")
        Text(
            puzzle.text,
            color = Palette.Text,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; wrong = false },
            placeholder = { Text("Type it here", color = Palette.Muted) },
            shape = RoundedCornerShape(18.dp),
            colors = curioFieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (wrong) Text("That doesn't match. Check every word.", color = Palette.Danger, fontSize = 13.sp)
        PrimaryButton("Confirm", onClick = { submit() }, modifier = Modifier.fillMaxWidth(), enabled = input.isNotBlank())
    }
}

// ------------------------------------------------------------------ memory

@Composable
fun MemoryChallenge(puzzle: Puzzle.Memory, onSolved: () -> Unit, onWrong: () -> Unit) {
    var showing by remember(puzzle) { mutableStateOf(true) }
    var entered by remember(puzzle) { mutableStateOf(listOf<String>()) }
    val progress = remember(puzzle) { Animatable(1f) }
    val shake = rememberShake()
    val n = puzzle.sequence.size

    LaunchedEffect(puzzle) {
        progress.animateTo(0f, tween(puzzle.showMillis.toInt(), easing = LinearEasing))
        showing = false
    }

    Column(
        Modifier.fillMaxWidth().offset { IntOffset(shake.offset, 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (showing) {
            SectionLabel("Memorize the sequence")
            if (puzzle.attempt > 0) {
                Text("Incorrect. Here is a new sequence.", color = Palette.Danger, fontSize = 13.sp)
            }
            Text(
                puzzle.sequence.joinToString("  "),
                color = Palette.Text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                lineHeight = 46.sp
            )
            Box(
                Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Palette.SurfaceHigh)
            ) {
                Box(
                    Modifier.fillMaxWidth(progress.value.coerceIn(0f, 1f)).height(3.dp).background(Palette.Accent)
                )
            }
            GhostButton("I'm ready", onClick = { showing = false }, modifier = Modifier.fillMaxWidth())
        } else {
            SectionLabel("Repeat the sequence")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until n) {
                    Text(
                        entered.getOrNull(i) ?: "–",
                        color = if (i < entered.size) Palette.Text else Palette.Outline2,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.size(width = 30.dp, height = 36.dp)
                    )
                }
            }
            val perRow = if (puzzle.alphabet.size > 8) 5 else 4
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                puzzle.alphabet.chunked(perRow).forEach { rowKeys ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowKeys.forEach { key ->
                            PadKey(key, Modifier.weight(1f)) {
                                if (entered.size < n) {
                                    val next = entered + key
                                    entered = next
                                    if (next.size == n) {
                                        if (next == puzzle.sequence) {
                                            onSolved()
                                        } else {
                                            shake.trigger()
                                            onWrong()
                                        }
                                    }
                                }
                            }
                        }
                        repeat(perRow - rowKeys.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            GhostButton(
                "Delete last",
                onClick = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PadKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.SurfaceHigh)
            .border(1.dp, Palette.Outline2, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Palette.Text, fontSize = 22.sp)
    }
}
