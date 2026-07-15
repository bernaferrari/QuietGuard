package com.bernaferari.renetguard.data.db

import androidx.room3.Room
import com.bernaferari.renetguard.worker.createSQLiteWasmWorker
import kotlinx.coroutines.Dispatchers

internal fun createNetGuardDatabase(): NetGuardDatabase =
    Room.databaseBuilder<NetGuardDatabase>(name = "netguard.db")
        .setDriver(createSQLiteWasmWorker())
        // Web/demo only: wipe and recreate when schema version changes.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()