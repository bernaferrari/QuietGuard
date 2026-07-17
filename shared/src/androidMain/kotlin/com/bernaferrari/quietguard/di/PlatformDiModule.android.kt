package com.bernaferrari.quietguard.di

import android.content.Context
import com.bernaferrari.quietguard.domain.AndroidRulesRepository
import com.bernaferrari.quietguard.domain.RulesRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Module
actual class PlatformDiModule {
    @Single
    fun rulesRepository(
        repository: AndroidRulesRepository,
    ): RulesRepository = repository

    @Single
    fun androidRulesRepository(
        @Provided context: Context,
        preferencesRepository: com.bernaferrari.quietguard.data.PreferencesRepository,
    ): AndroidRulesRepository = AndroidRulesRepository(preferencesRepository, context)
}