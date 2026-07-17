package com.bernaferrari.quietguard.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        LogEntity::class,
        AccessEntity::class,
        DnsEntity::class,
        ForwardEntity::class,
        AppEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(NetGuardDatabaseConstructor::class)
abstract class NetGuardDatabase : RoomDatabase() {
    abstract fun netGuardDao(): NetGuardDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object NetGuardDatabaseConstructor : RoomDatabaseConstructor<NetGuardDatabase> {
    override fun initialize(): NetGuardDatabase
}