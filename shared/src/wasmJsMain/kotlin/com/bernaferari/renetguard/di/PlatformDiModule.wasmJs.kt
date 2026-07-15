package com.bernaferari.renetguard.di

import com.bernaferari.renetguard.data.db.NetGuardDao
import com.bernaferari.renetguard.data.db.NetGuardDatabase
import com.bernaferari.renetguard.data.db.createNetGuardDatabase
import com.bernaferari.renetguard.domain.RulesRepository
import com.bernaferari.renetguard.domain.WasmRulesRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformDiModule {
    @Single
    fun rulesRepository(repository: WasmRulesRepository): RulesRepository = repository

    /** Room + WebWorkerSQLiteDriver (OPFS). Created once when Koin starts. */
    @Single
    fun netGuardDatabase(): NetGuardDatabase = createNetGuardDatabase()

    @Single
    fun netGuardDao(database: NetGuardDatabase): NetGuardDao = database.netGuardDao()
}