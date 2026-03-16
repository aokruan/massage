package ru.aokruan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.AppShapes
import ru.aokruan.designsystem.components.DecorativeBlobs
import ru.aokruan.designsystem.components.GlassCard
import ru.aokruan.designsystem.components.GlassChip
import ru.aokruan.designsystem.components.GlassSectionCard
import ru.aokruan.designsystem.components.PremiumTopBar
import ru.aokruan.service.domain.GetMassageByIdUseCase
import ru.aokruan.service.model.ServiceItem
import ru.aokruan.ui.mapper.formatDuration
import ru.aokruan.ui.mapper.formatPrice
import ru.aokruan.ui.mapper.title
import ru.aokruan.ui.vm.ServiceDetailViewModel

@Composable
fun ServiceDetailScreen(
    id: String,
    getMassageById: GetMassageByIdUseCase,
    onBack: () -> Unit,
) {
    val vm = remember(id) { ServiceDetailViewModel(id, getMassageById) }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
    ) {
        PremiumTopBar(
            title = "Детали услуги",
            onBack = onBack
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.ScreenBg)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    DetailErrorState(
                        message = state.error.orEmpty()
                    )
                }

                state.item != null -> {
                    DetailContent(item = state.item!!)
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    item: ServiceItem,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        HeroDetailCard(item = item)

        GlassSectionCard(title = "Описание") {
            Text(
                text = item.description,
                color = AppColors.Body,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            )
        }

        if (item.tags.isNotEmpty()) {
            TagsSection(tags = item.tags)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroDetailCard(
    item: ServiceItem,
) {
    GlassCard(
        shape = AppShapes.HeroCard,
        minHeight = 260.dp
    ) {
        DecorativeBlobs()

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(170.dp)
                .background(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = AppShapes.InnerGlass
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp)
        ) {
            GlassChip(
                text = formatPrice(item.priceMinor),
                background = AppColors.ChipBg.copy(alpha = 0.92f),
                textColor = AppColors.ChipText,
                horizontalPadding = 18.dp,
                verticalPadding = 10.dp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(42.dp))

            Text(
                text = item.title,
                color = AppColors.Title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassChip(
                    text = formatDuration(item.durationMinutes),
                    background = Color.White.copy(alpha = 0.50f),
                    textColor = AppColors.Body,
                    horizontalPadding = 14.dp,
                    verticalPadding = 8.dp
                )

                if (item.category.title.isNotBlank()) {
                    GlassChip(
                        text = item.category.title,
                        background = Color.White.copy(alpha = 0.38f),
                        textColor = AppColors.Body,
                        horizontalPadding = 14.dp,
                        verticalPadding = 8.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = item.description,
                color = AppColors.Body.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
) {
    GlassSectionCard(title = "Теги") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tags.forEach { tag ->
                GlassChip(
                    text = tag,
                    background = Color.White.copy(alpha = 0.55f),
                    textColor = AppColors.Title,
                    horizontalPadding = 14.dp,
                    verticalPadding = 8.dp
                )
            }
        }
    }
}

@Composable
private fun DetailErrorState(
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassSectionCard(title = "Ошибка загрузки") {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}