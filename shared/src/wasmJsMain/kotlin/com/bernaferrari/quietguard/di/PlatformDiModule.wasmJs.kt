package com.bernaferrari.quietguard.di

import com.bernaferrari.quietguard.data.db.NetGuardDao
import com.bernaferrari.quietguard.data.db.NetGuardDatabase
import com.bernaferrari.quietguard.data.db.createNetGuardDatabase
import com.bernaferrari.quietguard.domain.RulesRepository
import com.bernaferrari.quietguard.domain.WasmRulesRepository
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