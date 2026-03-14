package ru.aokruan.hmlkbi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.aokruan.network.ApiOperation
import ru.aokruan.service.domain.GetMassagesPageUseCase

class ServiceListViewModel(
    private val getMassagesPage: GetMassagesPageUseCase,
    private val pageSize: Int = 7
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceListState())
    val state: StateFlow<ServiceListState> = _state

    fun loadFirstPage() {
        if (_state.value.page != 0) return
        loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading) return
        val nextPage = s.page + 1

        val total = s.totalPages
        if (total != null && nextPage > total) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val res = getMassagesPage(nextPage, pageSize)) {
                is ApiOperation.Success -> {
                    val page = res.data
                    _state.update {
                        it.copy(
                            items = it.items + page.results,
                            page = nextPage,
                            totalPages = page.info.pages,
                            isLoading = false
                        )
                    }
                }
                is ApiOperation.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = res.error.toString()
                        )
                    }
                }
            }
        }
    }
}