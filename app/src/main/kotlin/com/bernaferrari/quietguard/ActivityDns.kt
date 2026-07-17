package com.bernaferrari.quietguard

import android.app.Activity
import android.os.Bundle
import com.bernaferrari.quietguard.ui.Dns

class ActivityDns : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(ActivityMain.createRouteIntent(this, Dns.route))
        finish()
    }
}
