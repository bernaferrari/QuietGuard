package com.bernaferrari.sdkmonitor.ui.settings

import com.bernaferrari.sdkmonitor.domain.model.AppFilter
import com.bernaferrari.sdkmonitor.domain.model.AppVersion
import com.bernaferrari.sdkmonitor.domain.model.ThemeMode

data class SdkDistribution(
    val sdkVersion: Int,
    val appCount: Int,
    val percentage: Float,
)

/**
 * UI state for Settings screen with granular control
 * Each preference can be updated independently while maintaining overall state consistency
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: SettingsPreferences = SettingsPreferences(),
    val errorMessage: String? = null,
    val sdkDistribution: List<SdkDistribution> = emptyList(),
    val totalApps: Int = 0,
    val allAppsForSdk: List<AppVersion> = emptyList(),
    val isAnalyticsLoading: Boolean = false,
) {
    val hasError: Boolean get() = errorMessage != null
}

data class SettingsPreferences(
    val themeMode: ThemeMode = ThemeMode.MATERIAL_YOU,
    val appFilter: AppFilter = AppFilter.ALL_APPS,
    val backgroundSync: Boolean = false,
    val orderBySdk: Boolean = false,
    val syncInterval: String = "30",
    val syncLocalTimeUnit: LocalTimeUnit = LocalTimeUnit.MINUTES,
)

enum class LocalTimeUnit(
    val code: Int,
) {
    MINUTES(0),
    HOURS(1),
    DAYS(2),
}
