package com.bernaferrari.quietguard.service

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.bernaferrari.quietguard.LegacyTelephony
import com.bernaferrari.quietguard.Util

/** Owns API-level differences and registration state for telephony-driven VPN reloads. */
internal class SinkholeTelephonyMonitor(
    private val context: Context,
    private val isFirewallEnabled: () -> Boolean,
    private val usesMeteredGenerationRules: () -> Boolean,
    private val onReload: (reason: String) -> Unit,
    private val onStart: () -> Unit,
    private val onTemporaryStop: () -> Unit,
) {
    private var legacyCallStateToken: Any? = null
    private var legacyDataConnectionToken: Any? = null
    private var callStateCallback: TelephonyCallback? = null
    private var dataConnectionCallback: TelephonyCallback? = null
    private var lastGeneration: String? = null

    fun setCallStateListening(telephonyManager: TelephonyManager, enabled: Boolean) {
        if (enabled) registerCallStateListener(telephonyManager) else unregisterCallStateListener(telephonyManager)
    }

    fun startDataConnectionListener(telephonyManager: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dataConnectionCallback == null) {
                val callback = object : TelephonyCallback(), TelephonyCallback.DataConnectionStateListener {
                    override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                        handleDataConnectionStateChanged(state)
                    }
                }
                telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
                dataConnectionCallback = callback
            }
        } else if (legacyDataConnectionToken == null) {
            legacyDataConnectionToken = LegacyTelephony.registerDataConnection(telephonyManager) {
                handleDataConnectionStateChanged(it)
            }
        }
    }

    fun stop(telephonyManager: TelephonyManager) {
        unregisterCallStateListener(telephonyManager)
        unregisterDataConnectionListener(telephonyManager)
    }

    private fun handleDataConnectionStateChanged(state: Int) {
        if (state != TelephonyManager.DATA_CONNECTED) return

        val currentGeneration = Util.getNetworkGeneration(context)
        Log.i(TAG, "Data connected generation=$currentGeneration")
        if (lastGeneration == currentGeneration) return

        Log.i(TAG, "New network generation=$currentGeneration")
        lastGeneration = currentGeneration
        if (usesMeteredGenerationRules()) onReload("data connection state changed")
    }

    private fun handleCallStateChanged(state: Int) {
        Log.i(TAG, "New call state=$state")
        if (!isFirewallEnabled()) return
        if (state == TelephonyManager.CALL_STATE_IDLE) onStart() else onTemporaryStop()
    }

    private fun registerCallStateListener(telephonyManager: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (callStateCallback == null) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) = handleCallStateChanged(state)
                }
                telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
                callStateCallback = callback
            }
        } else if (legacyCallStateToken == null && Util.hasPhoneStatePermission(context)) {
            legacyCallStateToken = LegacyTelephony.registerCallState(telephonyManager) {
                handleCallStateChanged(it)
            }
        }
    }

    private fun unregisterCallStateListener(telephonyManager: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateCallback?.let(telephonyManager::unregisterTelephonyCallback)
            callStateCallback = null
        } else if (legacyCallStateToken != null) {
            LegacyTelephony.unregisterCallState(telephonyManager, legacyCallStateToken)
            legacyCallStateToken = null
        }
    }

    private fun unregisterDataConnectionListener(telephonyManager: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dataConnectionCallback?.let(telephonyManager::unregisterTelephonyCallback)
            dataConnectionCallback = null
        } else if (legacyDataConnectionToken != null) {
            LegacyTelephony.unregisterDataConnection(telephonyManager, legacyDataConnectionToken)
            legacyDataConnectionToken = null
        }
    }

    private companion object {
        const val TAG = "NetGuard.Service"
    }
}
