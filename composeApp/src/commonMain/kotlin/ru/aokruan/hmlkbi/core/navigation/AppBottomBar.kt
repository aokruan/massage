package ru.aokruan.hmlkbi.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.AppShapes
import ru.aokruan.designsystem.components.GlassCard

@Composable
fun AppBottomBar(
    selectedTab: AppTab?,
    onTabClick: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        GlassCard(
            shape = AppShapes.ScreenCard,
            backgroundBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.86f),
                    Color(0xFFF4EDF8),
                    Color.White.copy(alpha = 0.80f),
                )
            ),
            borderColor = AppColors.CardBorder,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTab.entries.forEach { tab ->
                    BottomBarItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabClick(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val itemBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                AppColors.AccentLavender.copy(alpha = 0.62f),
                AppColors.AccentPink.copy(alpha = 0.34f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
            )
        )
    }

    val borderColor = if (selected) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color.Transparent
    }

    val contentColor = if (selected) {
        AppColors.Title
    } else {
        AppColors.Body
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 64.dp)
            .clip(AppShapes.Pill)
            .background(
                brush = itemBrush,
                shape = AppShapes.Pill,
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = AppShapes.Pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )

            Text(
                text = tab.title,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
                    fontSize = 9.sp
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}