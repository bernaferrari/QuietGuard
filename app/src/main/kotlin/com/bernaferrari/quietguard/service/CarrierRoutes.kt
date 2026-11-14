package com.bernaferrari.quietguard.service

import android.content.res.Configuration
import com.bernaferrari.quietguard.IPUtil

/** Carrier ranges excluded from the VPN when subnet routing is enabled. */
internal fun carrierRouteExclusions(config: Configuration): List<IPUtil.CIDR> = buildList {
    if (
        config.mcc == 310 &&
            (config.mnc == 160 ||
                config.mnc == 200 ||
                config.mnc == 210 ||
                config.mnc == 220 ||
                config.mnc == 230 ||
                config.mnc == 240 ||
                config.mnc == 250 ||
                config.mnc == 260 ||
                config.mnc == 270 ||
                config.mnc == 310 ||
                config.mnc == 490 ||
                config.mnc == 660 ||
                config.mnc == 800)
    ) {
        add(IPUtil.CIDR("66.94.2.0", 24))
        add(IPUtil.CIDR("66.94.6.0", 23))
        add(IPUtil.CIDR("66.94.8.0", 22))
        add(IPUtil.CIDR("208.54.0.0", 16))
    }

    if (
        (config.mcc == 310 &&
            (config.mnc == 4 ||
                config.mnc == 5 ||
                config.mnc == 6 ||
                config.mnc == 10 ||
                config.mnc == 12 ||
                config.mnc == 13 ||
                config.mnc == 350 ||
                config.mnc == 590 ||
                config.mnc == 820 ||
                config.mnc == 890 ||
                config.mnc == 910)) ||
            (config.mcc == 311 &&
                (config.mnc == 12 ||
                    config.mnc == 110 ||
                    (config.mnc >= 270 && config.mnc <= 289) ||
                    config.mnc == 390 ||
                    (config.mnc >= 480 && config.mnc <= 489) ||
                    config.mnc == 590)) ||
            (config.mcc == 312 && config.mnc == 770)
    ) {
        add(IPUtil.CIDR("66.174.0.0", 16))
        add(IPUtil.CIDR("66.82.0.0", 15))
        add(IPUtil.CIDR("69.96.0.0", 13))
        add(IPUtil.CIDR("70.192.0.0", 11))
        add(IPUtil.CIDR("97.128.0.0", 9))
        add(IPUtil.CIDR("174.192.0.0", 9))
        add(IPUtil.CIDR("72.96.0.0", 9))
        add(IPUtil.CIDR("75.192.0.0", 9))
        add(IPUtil.CIDR("97.0.0.0", 10))
    }

    if (config.mnc == 10 && config.mcc == 208) {
        add(IPUtil.CIDR("10.151.0.0", 24))
    }
}
