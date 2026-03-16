package ru.aokruan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.AppShapes
import ru.aokruan.designsystem.components.DecorativeBlobs
import ru.aokruan.designsystem.components.GlassCard
import ru.aokruan.designsystem.components.GlassChip
import ru.aokruan.service.model.ServiceItem
import ru.aokruan.ui.mapper.formatDuration
import ru.aokruan.ui.mapper.formatPrice
import ru.aokruan.ui.vm.ServiceListViewModel

@Composable
fun ServiceListScreen(
    vm: ServiceListViewModel,
    onItemClick: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.loadFirstPage() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= state.items.size - 3) {
                    vm.loadNextPage()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
    ) {
        if (state.error != null) {
            Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                ServiceListCard(
                    item = item,
                    onClick = { onItemClick(item.id) }
                )
            }

            item {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceListCard(
    item: ServiceItem,
    onClick: () -> Unit,
) {
    GlassCard(
        minHeight = 208.dp,
        shape = AppShapes.ScreenCard,
        onClick = onClick
    ) {
        DecorativeBlobs()

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(126.dp)
                .background(
                    color = Color.White.copy(alpha = 0.16f),
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
                .fillMaxSize()
                .padding(start = 20.dp, top = 22.dp, end = 132.dp, bottom = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = item.title,
                color = AppColors.Title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassChip(
                text = formatDuration(item.durationMinutes),
                background = Color.White.copy(alpha = 0.45f),
                textColor = AppColors.Body,
                horizontalPadding = 14.dp,
                verticalPadding = 8.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.description,
                color = AppColors.Body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}