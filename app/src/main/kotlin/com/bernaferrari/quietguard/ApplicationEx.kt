package com.bernaferrari.quietguard

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.bernaferrari.quietguard.data.initPreferencesDataStore
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.platform.installNetGuardPlatformBindings
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
open class ApplicationEx : Application() {
    override fun onCreate() {
        super.onCreate()

        initPreferencesDataStore(this)
        startKoin<ApplicationEx> {
            androidContext(this@ApplicationEx)
        }
        installNetGuardPlatformBindings()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context).build()
        }

        Log.i(
            TAG,
            "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this),
        )
        preferences()
        WorkScheduler.scheduleHousekeeping(this)

        Notifications.ensureChannels(this)
    }

    companion object {
        private const val TAG = "NetGuard.App"
    }
}
