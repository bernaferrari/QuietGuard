package eu.faircode.netguard

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun updateAll(context: Context) {
        scope.launch {
            FirewallWidget().updateAll(context)
            LockdownWidget().updateAll(context)
        }
    }

    fun updateFirewall(context: Context) {
        scope.launch {
            FirewallWidget().updateAll(context)
        }
    }

    fun updateLockdown(context: Context) {
        scope.launch {
            LockdownWidget().updateAll(context)
        }
    }

    fun themeColorRes(enabled: Boolean): Int {
        val theme = eu.faircode.netguard.data.Prefs.getString("theme", "teal")
        return when (theme) {
            "blue" -> if (enabled) R.color.colorBlueOn else R.color.colorBlueOff
            "purple" -> if (enabled) R.color.colorPurpleOn else R.color.colorPurpleOff
            "amber" -> if (enabled) R.color.colorAmberOn else R.color.colorAmberOff
            "orange" -> if (enabled) R.color.colorOrangeOn else R.color.colorOrangeOff
            "green" -> if (enabled) R.color.colorGreenOn else R.color.colorGreenOff
            else -> if (enabled) R.color.colorTealOn else R.color.colorTealOff
        }
    }
}
