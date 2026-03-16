package ru.aokruan.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.AppShapes

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.ScreenCard,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    backgroundBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF7F0FA),
            Color(0xFFF3EDF8),
            Color(0xFFF7F2F5)
        )
    ),
    borderColor: Color = AppColors.CardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x22000000)
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .then(clickableModifier),
        content = content
    )
}