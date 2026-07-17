package com.bernaferrari.quietguard.ui.screens.vm

import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.platform.observeOnChanges
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficScreenStateTest {
    @Test
    fun dnsFiltersSeparateActiveAndExpiredRecords() {
        val active = dnsEntry(time = 8_000, ttl = 5_000)
        val expired = dnsEntry(time = 1_000, ttl = 5_000)
        val entries = UiAsyncState(data = listOf(active, expired), hasReceived = true)

        val activeState = DnsScreenState(entries = entries, filter = DnsListFilter.Active, nowMs = 10_000)
        val expiredState = DnsScreenState(entries = entries, filter = DnsListFilter.Expired, nowMs = 10_000)

        assertEquals(listOf(active), activeState.filtered)
        assertEquals(listOf(expired), expiredState.filtered)
        assertEquals(1, activeState.expiredCount)
    }

    @Test
    fun forwardingFiltersUseProtocolNumbers() {
        val udp = ForwardingEntry(protocol = 17, dport = 53, raddr = "1.1.1.1", rport = 53, ruid = 0)
        val tcp = ForwardingEntry(protocol = 6, dport = 443, raddr = "1.1.1.1", rport = 443, ruid = 0)
        val entries = UiAsyncState(data = listOf(udp, tcp), hasReceived = true)

        assertEquals(
            listOf(udp),
            ForwardingScreenState(entries = entries, protocolFilter = ForwardingListFilter.Udp).filtered,
        )
        assertEquals(
            listOf(tcp),
            ForwardingScreenState(entries = entries, protocolFilter = ForwardingListFilter.Tcp).filtered,
        )
    }

    @Test
    fun observerReloadsOnItsOwnChangeEventAndUnregisters() =
        runBlocking {
            var value = 0
            val listeners = mutableSetOf<() -> Unit>()
            val emissions = mutableListOf<Int>()

            val job = launch {
                observeOnChanges(
                    register = { listener ->
                        listeners += listener
                        val unregister: () -> Unit = { listeners -= listener }
                        unregister
                    },
                ) { value }
                    .take(2)
                    .toList(emissions)
            }

            withTimeout(2_000) { while (listeners.isEmpty()) kotlinx.coroutines.yield() }
            withTimeout(2_000) { while (emissions.isEmpty()) kotlinx.coroutines.yield() }
            value = 1
            listeners.single().invoke()
            job.join()

            assertEquals(listOf(0, 1), emissions)
            assertEquals(emptySet<() -> Unit>(), listeners)
        }

    private fun dnsEntry(time: Long, ttl: Int) =
        DnsEntry(time = time, qname = "example.com", aname = "example.com", resource = "1.1.1.1", ttl = ttl, uid = 1000)
}
