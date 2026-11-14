package com.bernaferrari.quietguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first

private var appContext: Context? = null

fun initPreferencesDataStore(context: Context) {
    appContext = context.applicationContext
    createPreferencesDataStore()
}

actual fun createPlatformDataStore(): DataStore<Preferences> {
    val context =
        checkNotNull(appContext) {
            "Call initPreferencesDataStore(context) before accessing preferences on Android."
        }
    return PreferenceDataStoreFactory.create(
        produceFile = { context.filesDir.resolve(dataStoreFileName) }
    )
}

internal actual fun readInitialPreferences(dataStore: DataStore<Preferences>): Preferences? =
    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        dataStore.data.first()
    }
