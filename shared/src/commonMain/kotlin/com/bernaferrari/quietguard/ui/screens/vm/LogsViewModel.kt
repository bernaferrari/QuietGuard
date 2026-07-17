package com.bernaferrari.quietguard.ui.screens.vm

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.data.TrafficRepository
import com.bernaferrari.quietguard.platform.AppDisplayInfo
import com.bernaferrari.quietguard.platform.LogEntry
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.PlatformContext
import com.bernaferrari.quietguard.ui.screens.LOGS_UI_MAX_ROWS
import com.bernaferrari.quietguard.ui.screens.LogsGroupMode
import com.bernaferrari.quietguard.ui.screens.LogsOutcomeFilter
import com.bernaferrari.quietguard.ui.screens.LogsProtocolFilter
import com.bernaferrari.quietguard.ui.screens.buildLogQueryFlags
import com.bernaferrari.quietguard.ui.screens.defaultOutcomeFilterFromPrefs
import com.bernaferrari.quietguard.ui.screens.defaultProtocolFilterFromPrefs
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import com.bernaferrari.quietguard.ui.util.asUiAsyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class LogsScreenState(
    val outcomeFilter: LogsOutcomeFilter,
    val protocolFilter: LogsProtocolFilter,
    val groupMode: LogsGroupMode = LogsGroupMode.Timeline,
    val filtersExpanded: Boolean = false,
    val selectedAppUid: Int? = null,
    val logs: UiAsyncState<List<LogEntry>> = UiAsyncState(emptyList(), isLoading = true),
    val loggingEnabled: Boolean = false,
    val filteringEnabled: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class LogsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val trafficRepository: TrafficRepository,
) : ViewModel() {
    private val outcomeFilter = MutableStateFlow(defaultOutcomeFilterFromPrefs(preferencesRepository))
    private val protocolFilter = MutableStateFlow(defaultProtocolFilterFromPrefs(preferencesRepository))
    private val groupMode = MutableStateFlow(LogsGroupMode.Timeline)
    private val filtersExpanded = MutableStateFlow(false)
    private val selectedAppUid = MutableStateFlow<Int?>(null)
    private val isDemoMode = PlatformContext.isDemoMode()

    private val prefsFlags: StateFlow<Pair<Boolean, Boolean>> =
        preferencesRepository.data
            .map { prefs ->
                val logOn = (prefs[booleanPreferencesKey("log")] ?: false) || isDemoMode
                val filterOn = (prefs[booleanPreferencesKey("filter")] ?: false) || isDemoMode
                logOn to filterOn
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false to isDemoMode)

    private val logsState: StateFlow<UiAsyncState<List<LogEntry>>> =
        combine(outcomeFilter, protocolFilter) { outcome, protocol ->
            buildLogQueryFlags(protocol, outcome)
        }
            .flatMapLatest { flags ->
                trafficRepository.observeLogs(
                    udp = flags.udp,
                    tcp = flags.tcp,
                    other = flags.other,
                    allowed = flags.allowed,
                    blocked = flags.blocked,
                    limit = LOGS_UI_MAX_ROWS,
                )
            }
            .asUiAsyncState(viewModelScope, emptyList())

    private val filtersState =
        combine(outcomeFilter, protocolFilter, groupMode, filtersExpanded, selectedAppUid) {
                outcome,
                protocol,
                group,
                expanded,
                selected,
            ->
            FiltersSlice(outcome, protocol, group, expanded, selected)
        }

    val uiState: StateFlow<LogsScreenState> =
        combine(filtersState, logsState, prefsFlags) { filters, logs, flags ->
            LogsScreenState(
                outcomeFilter = filters.outcome,
                protocolFilter = filters.protocol,
                groupMode = filters.group,
                filtersExpanded = filters.expanded,
                selectedAppUid = filters.selected,
                logs = logs,
                loggingEnabled = flags.first,
                filteringEnabled = flags.second,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LogsScreenState(
                outcomeFilter = outcomeFilter.value,
                protocolFilter = protocolFilter.value,
                logs = logsState.value,
                loggingEnabled = prefsFlags.value.first,
                filteringEnabled = prefsFlags.value.second,
            ),
        )

    fun setOutcomeFilter(filter: LogsOutcomeFilter) {
        outcomeFilter.value = filter
    }

    fun setProtocolFilter(filter: LogsProtocolFilter) {
        protocolFilter.value = filter
    }

    fun setGroupMode(mode: LogsGroupMode) {
        groupMode.value = mode
        if (mode != LogsGroupMode.ByApp) selectedAppUid.value = null
    }

    fun setFiltersExpanded(expanded: Boolean) {
        filtersExpanded.value = expanded
    }

    fun selectAppUid(uid: Int?) {
        selectedAppUid.value = uid
    }

    fun enableFiltering() = preferencesRepository.putBoolean("filter", true)

    fun enableLogging() = preferencesRepository.putBoolean("log", true)

    fun clearLogs() {
        viewModelScope.launch { trafficRepository.clearAllLogs() }
    }

    fun appDisplay(uid: Int, fallbackLabel: String): AppDisplayInfo =
        trafficRepository.appDisplay(uid, fallbackLabel)

    fun hasLogPro(): Boolean =
        NetGuardPlatform.proFeatures.isPurchased(NetGuardPlatform.proFeatures.logSku)

    private data class FiltersSlice(
        val outcome: LogsOutcomeFilter,
        val protocol: LogsProtocolFilter,
        val group: LogsGroupMode,
        val expanded: Boolean,
        val selected: Int?,
    )
}
