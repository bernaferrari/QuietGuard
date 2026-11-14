package com.bernaferrari.quietguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bernaferrari.quietguard.data.preferences

open class ReceiverAutostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        val action = intent?.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            try {
                if (context.preferences().getBoolean("enabled", false)) {
                    ServiceSinkhole.start("receiver", context)
                } else if (context.preferences().getBoolean("show_stats", false)) {
                    ServiceSinkhole.run("receiver", context)
                }

                if (Util.isInteractive(context)) {
                    ServiceSinkhole.reloadStats("receiver", context)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }
    }

    companion object {
        private const val TAG = "NetGuard.Receiver"
    }
}
