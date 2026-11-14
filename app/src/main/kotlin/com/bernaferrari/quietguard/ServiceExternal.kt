package com.bernaferrari.quietguard

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.system.Os
import android.util.Log
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.shared.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.text.DateFormat
import java.util.Date
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceExternal : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ServiceSinkhole.NOTIFY_EXTERNAL, getForegroundNotification(this))
        val previous = job
        previous?.cancel()
        job = scope.launch {
            try {
                previous?.join()
                if (intent?.action == ACTION_DOWNLOAD_HOSTS_FILE) downloadHosts()
            } finally {
                withContext(Dispatchers.Main.immediate + NonCancellable) {
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun downloadHosts() {
        val preferences = applicationContext.preferences()
        val configuredUrl = preferences.getString("hosts_url", null)
        val source = configuredUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.HOSTS_FILE_URI
        val hosts = File(filesDir, "hosts.txt")
        val temporary = File.createTempFile("hosts-", ".tmp", filesDir)
        try {
            val url = URI(source).toURL()
            require(url.protocol == "https" || url.protocol == "http") {
                "Hosts URL must use HTTP or HTTPS"
            }
            val connection =
                (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Hosts download returned HTTP ${connection.responseCode}")
                }
                connection.inputStream.use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val bytes = input.read(buffer)
                            if (bytes == -1) break
                            output.write(buffer, 0, bytes)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            coroutineContext.ensureActive()
            Os.rename(temporary.absolutePath, hosts.absolutePath)
            preferences.putString(
                "hosts_last_download",
                DateFormat.getDateTimeInstance().format(Date()),
            )
            ServiceSinkhole.reload("hosts file download", this, false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.e(TAG, "Hosts download failed", failure)
        } finally {
            temporary.delete()
        }
    }

    companion object {
        private const val TAG = "NetGuard.External"
        const val ACTION_DOWNLOAD_HOSTS_FILE = "com.bernaferrari.quietguard.DOWNLOAD_HOSTS_FILE"

        private fun getForegroundNotification(context: Context): Notification {
            val builder = Notification.Builder(context, Notifications.CHANNEL_FOREGROUND)
            builder.setSmallIcon(context.hourglassIcon())
            builder.setCategory(Notification.CATEGORY_STATUS)
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
            builder.setContentTitle(context.getString(R.string.app_name))
            return builder.build()
        }
    }
}
