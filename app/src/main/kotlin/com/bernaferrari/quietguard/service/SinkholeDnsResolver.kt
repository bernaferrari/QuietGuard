package com.bernaferrari.quietguard.service

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import com.bernaferrari.quietguard.Util
import com.bernaferrari.quietguard.data.preferences
import java.math.BigInteger
import java.net.Inet4Address
import java.net.InetAddress

/** Resolves the DNS servers used by the VPN tunnel from preferences and network defaults. */
internal object SinkholeDnsResolver {
    private const val TAG = "NetGuard.Service"

    fun resolve(context: Context): List<InetAddress> {
        val prefs = context.preferences()
        val listDns = ArrayList<InetAddress>()
        val sysDns = Util.getDefaultDNS(context)

        val ip6 = prefs.getBoolean("ip6", true)
        val filter = prefs.getBoolean("filter", false)
        val vpnDns1 = prefs.getString("dns", null)
        val vpnDns2 = prefs.getString("dns2", null)
        Log.i(TAG, "DNS system=" + TextUtils.join(",", sysDns) + " config=$vpnDns1,$vpnDns2")

        if (vpnDns1 != null) {
            try {
                val dns = InetAddress.getByName(vpnDns1)
                if (!(dns.isLoopbackAddress || dns.isAnyLocalAddress) && (ip6 || dns is Inet4Address)) {
                    listDns.add(dns)
                }
            } catch (_: Throwable) {
            }
        }

        if (vpnDns2 != null) {
            try {
                val dns = InetAddress.getByName(vpnDns2)
                if (!(dns.isLoopbackAddress || dns.isAnyLocalAddress) && (ip6 || dns is Inet4Address)) {
                    listDns.add(dns)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        if (listDns.size == 2) {
            return listDns
        }

        for (defDns in sysDns) {
            try {
                val ddns = InetAddress.getByName(defDns)
                if (!listDns.contains(ddns) &&
                    !(ddns.isLoopbackAddress || ddns.isAnyLocalAddress) &&
                    (ip6 || ddns is Inet4Address)
                ) {
                    listDns.add(ddns)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        val count = listDns.size
        val lan = prefs.getBoolean("lan", false)
        val useHosts = prefs.getBoolean("use_hosts", false)
        if (lan && useHosts && filter) {
            try {
                val subnets = ArrayList<Pair<InetAddress, Int>>()
                subnets.add(Pair(InetAddress.getByName("10.0.0.0"), 8))
                subnets.add(Pair(InetAddress.getByName("172.16.0.0"), 12))
                subnets.add(Pair(InetAddress.getByName("192.168.0.0"), 16))

                for (subnet in subnets) {
                    val hostAddress = subnet.first
                    val host = BigInteger(1, hostAddress.address)

                    val prefix = subnet.second
                    val mask =
                        BigInteger.valueOf(-1)
                            .shiftLeft(hostAddress.address.size * 8 - prefix)

                    for (dns in ArrayList(listDns)) {
                        if (hostAddress.address.size == dns.address.size) {
                            val ip = BigInteger(1, dns.address)

                            if (host.and(mask) == ip.and(mask)) {
                                Log.i(
                                    TAG,
                                    "Local DNS server host=$hostAddress/$prefix dns=$dns"
                                )
                                listDns.remove(dns)
                            }
                        }
                    }
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        if (listDns.isEmpty() || listDns.size < count) {
            try {
                listDns.add(InetAddress.getByName("8.8.8.8"))
                listDns.add(InetAddress.getByName("8.8.4.4"))
                if (ip6) {
                    listDns.add(InetAddress.getByName("2001:4860:4860::8888"))
                    listDns.add(InetAddress.getByName("2001:4860:4860::8844"))
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        Log.i(TAG, "Get DNS=" + TextUtils.join(",", listDns))

        return listDns
    }
}
