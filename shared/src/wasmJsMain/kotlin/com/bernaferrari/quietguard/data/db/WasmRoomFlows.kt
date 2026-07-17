package com.bernaferrari.quietguard.data.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Subscribes to a Room [Flow] invalidation signal and invokes [onChanged] on each emission after the first
 * (first emission is the current snapshot, not a change).
 */
internal fun observeRoomInvalidations(
    flow: Flow<*>,
    onChanged: () -> Unit,
): () -> Unit {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val job: Job =
        scope.launch {
            WasmNetGuardDatabase.ensureDemoDataSeeded()
            flow
                .distinctUntilChanged()
                .drop(1)
                .collect { onChanged() }
        }
    return {
        job.cancel()
        scope.cancel()
    }
}
