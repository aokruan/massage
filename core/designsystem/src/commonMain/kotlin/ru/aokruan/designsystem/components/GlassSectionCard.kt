package ru.aokruan.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.AppShapes

@Composable
fun GlassSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        backgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.52f),
                Color(0xFFF5EFF8)
            )
        ),
        borderColor = AppColors.CardBorder,
        shape = AppShapes.SectionCardShape
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                color = AppColors.Title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = Color.White.copy(alpha = 0.65f)
            )

            content()
        }
    }
}