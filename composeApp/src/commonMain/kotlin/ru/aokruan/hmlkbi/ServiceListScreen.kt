package ru.aokruan.hmlkbi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import ru.aokruan.service.model.ServiceItem

@Composable
fun ServiceListScreen(vm: ServiceListViewModel) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.loadFirstPage() }

    // автопагинация
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

    Column(Modifier.fillMaxSize()) {
        if (state.error != null) {
            Text(
                text = state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                ServiceRow(item)
            }

            item {
                if (state.isLoading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(item: ServiceItem) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text("${item.durationMinutes} мин • ${item.priceMinor} (minor)", style = MaterialTheme.typography.bodyMedium)
            Text(item.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}