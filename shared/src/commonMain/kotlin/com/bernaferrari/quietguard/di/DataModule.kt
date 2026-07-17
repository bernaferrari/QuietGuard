package com.bernaferrari.quietguard.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.data.createPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class DataModule {
    @Single
    fun dataStore(): DataStore<Preferences> = createPreferencesDataStore()

    @Single
    fun preferencesRepository(dataStore: DataStore<Preferences>): PreferencesRepository =
        PreferencesRepository(dataStore, Dispatchers.Default)
}