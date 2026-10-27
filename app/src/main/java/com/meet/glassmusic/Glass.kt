package com.meet.glassmusic

import android.os.Build
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** Frosted fallback (also used where there is no backdrop to sample). */
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

/** Real liquid glass (blur + lens refraction) on Android 13+, frosted fallback on older phones. */
@Composable
fun GlassSurface(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT >= 33) {
        Box(
            modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(refractionHeight = 14.dp.toPx(), refractionAmount = 28.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.10f)) }
            ),
            content = content
        )
    } else {
        Glass(modifier, shape, content = content)
    }
}
