package com.bernaferrari.quietguard.data.db

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.mp.KoinPlatform

/**
 * Thin accessors for expect/actual platform code that cannot take constructor injection.
 * Database + DAO are owned by Koin ([com.bernaferrari.quietguard.di.PlatformDiModule]).
 */
internal object WasmNetGuardDatabase {
    private val seedMutex = Mutex()
    private var seeded: Boolean = false

    val instance: NetGuardDatabase
        get() = KoinPlatform.getKoin().get()

    val dao: NetGuardDao
        get() = KoinPlatform.getKoin().get()

    internal suspend fun ensureDemoDataSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            seedWebDemoDataIfEmpty(dao)
            seeded = true
        }
    }
}