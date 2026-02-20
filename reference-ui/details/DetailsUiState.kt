package com.bernaferrari.sdkmonitor.ui.details

import android.content.Context
import com.bernaferrari.sdkmonitor.data.Version
import com.bernaferrari.sdkmonitor.domain.model.AppDetails
import com.bernaferrari.sdkmonitor.domain.model.AppVersion
import com.bernaferrari.sdkmonitor.extensions.convertTimestampToDate

/**
 * Sealed class representing the UI state for the Details screen
 */
sealed class DetailsUiState {
    data object Loading : DetailsUiState()

    data class Success(
        val appDetails: AppDetails,
        val versions: List<AppVersion> = emptyList(),
    ) : DetailsUiState()

    data class Error(
        val message: String,
    ) : DetailsUiState()
}

/**
 * Data class representing version information for the app
 */
data class VersionInfo(
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val timestamp: Long,
    val changes: List<String> = emptyList(),
)

/**
 * Extension function to convert Version to AppVersion
 */
fun Version.toAppVersion(
    appDetails: AppDetails,
    context: Context,
) = AppVersion(
    packageName = this.packageName,
    title = appDetails.title,
    sdkVersion = this.targetSdk,
    versionName = this.versionName,
    versionCode = this.version,
    lastUpdateTime = this.lastUpdateTime.convertTimestampToDate(context),
)
