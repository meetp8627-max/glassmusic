package com.meet.glassmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Liquid-glass surface: dark frosted base, white sheen, specular rim light. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF1B1B22).copy(alpha = 0.72f))
            .background(tint)
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.05f))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(0.75f), Color.White.copy(0.06f), Color.White.copy(0.35f))),
                shape
            ),
        content = content
    )
}
