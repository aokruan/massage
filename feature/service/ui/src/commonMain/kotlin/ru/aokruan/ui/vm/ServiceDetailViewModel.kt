package ru.aokruan.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.aokruan.network.ApiOperation
import ru.aokruan.service.domain.GetMassageByIdUseCase
import ru.aokruan.service.model.ServiceItem

data class ServiceDetailState(
    val isLoading: Boolean = false,
    val item: ServiceItem? = null,
    val error: String? = null
)

class ServiceDetailViewModel(
    private val id: String,
    private val getMassageById: GetMassageByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceDetailState())
    val state: StateFlow<ServiceDetailState> = _state

    fun load() {
        if (_state.value.isLoading || _state.value.item != null) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val res = getMassageById(id)) {
                is ApiOperation.Success -> _state.update { it.copy(isLoading = false, item = res.data) }
                is ApiOperation.Failure -> _state.update { it.copy(isLoading = false, error = res.error.toString()) }
            }
        }
    }
}