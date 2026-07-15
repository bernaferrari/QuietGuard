package com.bernaferari.renetguard.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

internal fun createNetGuardDatabase(context: Context): NetGuardDatabase {
    val appContext = context.applicationContext
    val dbPath = appContext.getDatabasePath("netguard.db").absolutePath
    return Room.databaseBuilder<NetGuardDatabase>(
        context = appContext,
        name = dbPath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}