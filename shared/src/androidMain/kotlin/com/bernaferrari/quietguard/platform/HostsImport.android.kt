package com.bernaferrari.quietguard.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Same file ServiceExternal downloads into and the sinkhole reads on reload.
private const val HOSTS_FILE = "hosts.txt"

@Composable
actual fun rememberHostsFileImporter(onComplete: (success: Boolean) -> Unit): ((append: Boolean) -> Unit)? {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val currentOnComplete by rememberUpdatedState(onComplete)
    var appendMode by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching { copyHostsFile(context, uri, appendMode) }.isSuccess
            }
            if (success) NetGuardPlatform.firewall.reload("hosts file import", false)
            currentOnComplete(success)
        }
    }
    return remember(launcher) {
        { append ->
            appendMode = append
            launcher.launch(arrayOf("text/*", "application/octet-stream"))
        }
    }
}

private fun copyHostsFile(context: Context, uri: Uri, append: Boolean) {
    val hosts = File(context.filesDir, HOSTS_FILE)
    val target = if (append) hosts else File(context.filesDir, "hosts.import.tmp")
    val input = context.contentResolver.openInputStream(uri) ?: error("Unable to open $uri")
    input.use { source ->
        FileOutputStream(target, append).use { sink ->
            if (append && hosts.length() > 0) sink.write('\n'.code)
            source.copyTo(sink)
        }
    }
    if (!append) {
        if (hosts.exists()) hosts.delete()
        check(target.renameTo(hosts)) { "Unable to replace hosts file" }
    }
}
