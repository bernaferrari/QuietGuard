package com.bernaferrari.quietguard

import android.app.Activity
import android.os.Bundle
import com.bernaferrari.quietguard.ui.Settings

class ActivitySettings : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(ActivityMain.createRouteIntent(this, Settings.route))
        finish()
    }
}
