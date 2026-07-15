package com.bernaferari.renetguard.ui.screens.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferari.renetguard.data.TrafficRepository
import com.bernaferari.renetguard.platform.ForwardingEntry
import com.bernaferari.renetguard.ui.util.UiAsyncState
import com.bernaferari.renetguard.ui.util.asUiAsyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

enum class ForwardingListFilter {
    All,
    Udp,
    Tcp,
}

data class ForwardingScreenState(
    val entries: UiAsyncState<List<ForwardingEntry>> = UiAsyncState(emptyList(), isLoading = true),
    val protocolFilter: ForwardingListFilter = ForwardingListFilter.All,
    val showAddDialog: Boolean = false,
) {
    val filtered: List<ForwardingEntry>
        get() =
            entries.data.filter { entry ->
                when (protocolFilter) {
                    ForwardingListFilter.All -> true
                    ForwardingListFilter.Udp -> entry.protocol == 17
                    ForwardingListFilter.Tcp -> entry.protocol == 6
                }
            }
}

@KoinViewModel
class ForwardingViewModel(
    private val trafficRepository: TrafficRepository,
) : ViewModel() {
    private val protocolFilter = MutableStateFlow(ForwardingListFilter.All)
    private val showAddDialog = MutableStateFlow(false)
    private val entriesState = trafficRepository.observeForwarding().asUiAsyncState(viewModelScope, emptyList())

    val uiState: StateFlow<ForwardingScreenState> =
        combine(entriesState, protocolFilter, showAddDialog) { entries, filter, showDialog ->
            ForwardingScreenState(entries = entries, protocolFilter = filter, showAddDialog = showDialog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForwardingScreenState())

    fun setProtocolFilter(filter: ForwardingListFilter) {
        protocolFilter.value = filter
    }

    fun setShowAddDialog(show: Boolean) {
        showAddDialog.value = show
    }

    fun addForward(protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) {
        viewModelScope.launch { trafficRepository.addForward(protocol, dport, raddr, rport, ruid) }
        showAddDialog.value = false
    }

    fun deleteForward(entry: ForwardingEntry) {
        viewModelScope.launch { trafficRepository.deleteForward(entry) }
    }
}
