package com.bernaferari.renetguard.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Simple async list/data state for screen ViewModels backed by cold Flows. */
data class UiAsyncState<T>(
    val data: T,
    val isLoading: Boolean = false,
    val hasReceived: Boolean = false,
) {
    val isReady: Boolean get() = hasReceived && !isLoading
}

fun <T> Flow<T>.asUiAsyncState(
    scope: CoroutineScope,
    initialData: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
): StateFlow<UiAsyncState<T>> =
    this
        .map { value -> UiAsyncState(data = value, isLoading = false, hasReceived = true) }
        .onStart { emit(UiAsyncState(data = initialData, isLoading = true, hasReceived = false)) }
        .stateIn(
            scope = scope,
            started = started,
            initialValue = UiAsyncState(data = initialData, isLoading = true, hasReceived = false),
        )
