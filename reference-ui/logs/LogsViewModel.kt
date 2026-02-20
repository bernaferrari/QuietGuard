package com.bernaferrari.sdkmonitor.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.sdkmonitor.data.App
import com.bernaferrari.sdkmonitor.domain.model.AppFilter
import com.bernaferrari.sdkmonitor.domain.model.LogEntry
import com.bernaferrari.sdkmonitor.domain.repository.AppsRepository
import com.bernaferrari.sdkmonitor.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

sealed class LogsUiState {
    data object Loading : LogsUiState()

    data class Success(
        val logs: List<LogEntry>,
        val totalCount: Int,
    ) : LogsUiState()

    data class Error(
        val message: String,
    ) : LogsUiState()
}

@HiltViewModel
class LogsViewModel
    @Inject
    constructor(
        private val appsRepository: AppsRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<LogsUiState>(LogsUiState.Loading)
        val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

        // Expose apps data for filtering - use App instead of AppVersion
        private val _apps = MutableStateFlow<List<App>>(emptyList())
        val apps: StateFlow<List<App>> = _apps.asStateFlow()

        // Expose app filter
        private val _appFilter = MutableStateFlow(AppFilter.ALL_APPS)
        val appFilter: StateFlow<AppFilter> = _appFilter.asStateFlow()

        init {
            // Load apps data and set up preference observation
            viewModelScope.launch {
                // Load apps first
                loadAppsData()

                // Then observe preferences
                preferencesRepository.getUserPreferences().collect { preferences ->
                    _appFilter.value = preferences.appFilter
                    loadLogsWithFilter(preferences.appFilter)
                }
            }
        }

        private suspend fun loadAppsData() {
            try {
                val allApps = appsRepository.getAllApps()
                _apps.value = allApps
            } catch (e: Exception) {
                // Handle error if needed
            }
        }

        private fun loadLogsWithFilter(appFilter: AppFilter) {
            viewModelScope.launch {
                try {
                    _uiState.value = LogsUiState.Loading

                    val allVersions = appsRepository.getAllVersions()
                    val allApps = _apps.value

                    val appMap = allApps.associateBy { it.packageName }

                    // Filter apps based on the provided filter
                    val filteredApps =
                        when (appFilter) {
                            AppFilter.ALL_APPS -> allApps
                            AppFilter.USER_APPS -> allApps.filter { it.isFromPlayStore }
                            AppFilter.SYSTEM_APPS -> allApps.filter { !it.isFromPlayStore }
                        }

                    val filteredPackageNames = filteredApps.map { it.packageName }.toSet()

                    // Group versions by package and sort by timestamp to track changes
                    val versionsByPackage =
                        allVersions
                            .filter { version -> version.packageName in filteredPackageNames }
                            .groupBy { it.packageName }
                            .mapValues { (_, versions) ->
                                versions.sortedBy { it.lastUpdateTime }
                            }

                    val logEntries = mutableListOf<LogEntry>()

                    versionsByPackage.forEach { (packageName, versions) ->
                        val app = appMap[packageName] ?: return@forEach

                        versions.forEachIndexed { index, currentVersion ->
                            val previousVersion = if (index > 0) versions[index - 1] else null

                            // Only create log entry if there's actually a meaningful change
                            val hasVersionChange =
                                previousVersion != null &&
                                    previousVersion.versionName != currentVersion.versionName
                            val hasSdkChange =
                                previousVersion != null &&
                                    previousVersion.targetSdk != currentVersion.targetSdk

                            // Only add to logs if there's an actual difference AND we have a previous version to compare
                            if (previousVersion != null && (hasVersionChange || hasSdkChange)) {
                                logEntries.add(
                                    LogEntry(
                                        id = currentVersion.versionId.toLong(),
                                        packageName = currentVersion.packageName,
                                        appName = app.title,
                                        oldSdk = previousVersion.targetSdk,
                                        newSdk = currentVersion.targetSdk,
                                        oldVersion = previousVersion.versionName,
                                        newVersion = currentVersion.versionName,
                                        timestamp = currentVersion.lastUpdateTime,
                                    ),
                                )
                            }
                        }
                    }

                    _uiState.value =
                        LogsUiState.Success(
                            logs = logEntries.sortedByDescending { it.timestamp }, // Most recent first
                            totalCount = logEntries.size,
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        LogsUiState.Error(
                            e.message ?: "Failed to load logs",
                        )
                }
            }
        }

        fun updateAppFilter(filter: AppFilter) {
            viewModelScope.launch {
                preferencesRepository.updateAppFilter(filter)
            }
        }

        fun loadLogs() {
            viewModelScope.launch {
                val preferences = preferencesRepository.getUserPreferences().first()
                loadLogsWithFilter(preferences.appFilter)
            }
        }

        fun refreshLogs() {
            loadLogs()
        }

        fun getCurrentFilter(): AppFilter =
            try {
                runBlocking {
                    val preferences = preferencesRepository.getUserPreferences().first()
                    preferences.appFilter
                }
            } catch (e: Exception) {
                AppFilter.ALL_APPS
            }
    }
