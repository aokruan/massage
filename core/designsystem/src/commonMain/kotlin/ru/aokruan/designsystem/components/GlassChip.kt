package ru.aokruan.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.aokruan.designsystem.AppShapes

@Composable
fun GlassChip(
    text: String,
    background: Color,
    textColor: Color,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Pill)
            .background(background)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.40f),
                shape = AppShapes.Pill
            )
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
            .widthIn(min = 72.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}