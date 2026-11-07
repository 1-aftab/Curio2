package com.aftab.curioalarm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Warm metallic gradient used for small decorative accents. */
val AccentBrush: Brush = Brush.horizontalGradient(
    listOf(Color(0xFFF2DCA8), Color(0xFFE3C27E), Color(0xFFB99A5B), Color(0xFFE8D09A))
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
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Accent,
            contentColor = Palette.Ink,
            disabledContainerColor = Palette.SurfaceHigh,
            disabledContentColor = Palette.Muted
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Palette.Outline2),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Palette.Text,
            disabledContentColor = Palette.Muted
        )
    ) {
        Text(text, fontSize = 13.sp, letterSpacing = 0.3.sp)
    }
}

/** Small rounded pill used for secondary metadata (challenge, kiosk, difficulty…). */
@Composable
fun MetaChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(shape = CircleShape, color = Palette.SurfaceHigh, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(11.dp))
            }
            Text(
                text,
                color = Palette.Muted,
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Circular tinted badge holding a typographic glyph (♫, ×, ◆ …). */
@Composable
fun GlyphBadge(glyph: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(38.dp).clip(CircleShape).background(Palette.AccentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = Palette.Accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun curioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Accent,
    unfocusedBorderColor = Palette.Outline2,
    cursorColor = Palette.Accent,
    focusedLabelColor = Palette.Accent,
    unfocusedLabelColor = Palette.Muted,
    focusedTextColor = Palette.Text,
    unfocusedTextColor = Palette.Text
)

@Composable
fun curioChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = Palette.Muted,
    selectedContainerColor = Palette.Accent,
    selectedLabelColor = Palette.Ink
)

@Composable
fun curioSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Palette.Ink,
    checkedTrackColor = Palette.Accent,
    checkedBorderColor = Palette.Accent,
    uncheckedThumbColor = Palette.Muted,
    uncheckedTrackColor = Palette.SurfaceHigh,
    uncheckedBorderColor = Palette.Outline2
)

/** "made with curiosity by Aftab": small muted lead-in, warm gold name. */
@Composable
fun CuriosityFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "made with curiosity by ",
            color = Palette.Muted,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 0.15.sp,
            modifier = Modifier.alignByBaseline()
        )
        Text(
            text = "Aftab",
            style = TextStyle(
                color = Palette.AccentDim,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp
            ),
            modifier = Modifier.alignByBaseline()
        )
    }
}
