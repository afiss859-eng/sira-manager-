package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Small design primitives used to keep SIRA surfaces consistent across screens. */
@Composable
fun SiraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    varPressedCard(onClick)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(1.dp)) {
            content()
        }
    }
}

@Composable
private fun varPressedCard(onClick: (() -> Unit)?) {
    // Deliberately tiny no-op hook: keeps the primitive binary-compatible while
    // allowing call sites to opt into click semantics through the helper below.
    if (onClick != null) Unit
}

fun Modifier.siraInteractive(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(enabled = enabled, onClick = onClick)

@Composable
fun SiraSoftBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        content()
    }
}

val SiraHairline = 1.dp
val SiraScreenPadding = 20.dp
val SiraSectionGap = 16.dp
val SiraControlHeight = 48.dp

fun siraAccentAlpha(isDark: Boolean): Float = if (isDark) 0.18f else 0.08f
