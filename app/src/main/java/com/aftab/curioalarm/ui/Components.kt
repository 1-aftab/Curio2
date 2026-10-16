package com.aftab.curioalarm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SilverBrush: Brush = Brush.horizontalGradient(
    listOf(Color(0xFFF4F5F8), Color(0xFFB9BCC6), Color(0xFF8E919C), Color(0xFFD8DAE2))
)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Palette.Muted,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
fun SilverButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Silver,
            contentColor = Palette.Black,
            disabledContainerColor = Palette.SurfaceHigh,
            disabledContentColor = Palette.Muted
        )
    ) {
        Text(text, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.Outline2),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Text)
    ) {
        Text(text, letterSpacing = 0.3.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun curioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Silver,
    unfocusedBorderColor = Palette.Outline2,
    cursorColor = Palette.Silver,
    focusedLabelColor = Palette.Silver,
    unfocusedLabelColor = Palette.Muted
)

@Composable
fun curioChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = Palette.Muted,
    selectedContainerColor = Palette.Silver,
    selectedLabelColor = Palette.Black
)

@Composable
fun curioSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Palette.Black,
    checkedTrackColor = Palette.Silver,
    checkedBorderColor = Palette.Silver,
    uncheckedThumbColor = Palette.Muted,
    uncheckedTrackColor = Palette.SurfaceHigh,
    uncheckedBorderColor = Palette.Outline2
)

/** "made with curiosity by Aftab": small muted lead-in, slightly larger metallic-silver name. */
@Composable
fun CuriosityFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "made with curiosity by ",
            color = Palette.Muted,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 0.15.sp,
            modifier = Modifier.alignByBaseline()
        )
        Text(
            text = "Aftab",
            style = TextStyle(
                color = Color(0xFFC6C6CC),
                fontSize = 12.5.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp
            ),
            modifier = Modifier.alignByBaseline()
        )
    }
}
