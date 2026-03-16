package ru.aokruan.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
object AppShapes {
    val ScreenCard: Shape = RoundedCornerShape(30.dp)
    val HeroCard: Shape = RoundedCornerShape(34.dp)
    val SectionCardShape: Shape = RoundedCornerShape(28.dp)
    val InnerGlass: Shape = RoundedCornerShape(22.dp)
    val Pill: Shape = RoundedCornerShape(999.dp)
}