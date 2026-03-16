package ru.aokruan.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.aokruan.designsystem.AppColors

@Composable
fun BoxScope.DecorativeBlobs() {
    Box(
        modifier = Modifier
            .offset(x = (-18).dp, y = (-10).dp)
            .size(150.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.AccentCream.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                )
            )
    )

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 26.dp, y = (-8).dp)
            .size(170.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.AccentLavender.copy(alpha = 0.95f),
                        Color.Transparent
                    )
                )
            )
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = 8.dp, y = 24.dp)
            .size(130.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.AccentPink.copy(alpha = 0.70f),
                        Color.Transparent
                    )
                )
            )
    )

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = 30.dp, y = 8.dp)
            .size(110.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.AccentMint.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                )
            )
    )
}