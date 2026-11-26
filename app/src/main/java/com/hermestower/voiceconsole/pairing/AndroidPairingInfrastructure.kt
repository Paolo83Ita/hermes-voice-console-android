package com.hermestower.voiceconsole.pairing

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ANDROID REPAIR CP-005 — Android-side pairing infrastructure (glue only; all
 * decision logic lives in the JVM-tested B-lane classes).
 *
 *  - [AndroidNetworkContext]: ConnectivityManager classification into the
 *    resolver's [NetworkContextType] vocabulary (rule 3 of the resolution
 *    contract);
 *  - [PrefsPairingSessionStore]: the non-secret resume store (stage, challenge
 *    id, expiry, phrase-confirmed flag — NO endpoints, NO tokens, NO codes);
 *  - [NsdTowerDiscovery]: the LAN_DISCOVERY method (mission section 4) —
 *    NsdManager service discovery of the Tower on the home Wi-Fi, producing
 *    LAN_IP endpoint candidates (NSD resolves the SERVICE to an address; the
 *    standard resolver cannot resolve .local hostnames — that is exactly why
 *    this surface exists).
 */

/** Rule 3: current network context classification. */
class AndroidNetworkContext(private val context: Context) {

    fun current(): NetworkContextType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkContextType.NONE
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetworkContextType.NONE
        val vpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val ethernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        return when {
            vpn -> NetworkContextType.VPN
            wifi || ethernet -> if (ethernet) NetworkContextType.ETHERNET else NetworkContextType.WIFI
            cellular -> NetworkContextType.CELLULAR
            else -> NetworkContextType.NONE
        }
    }
}

/** Non-secret resume store (SharedPreferences; all values are public/non-secret). */
class PrefsPairingSessionStore(context: Context) : PairingSessionStore {

    private val prefs: SharedPreferences = context.getSharedPreferences("tower_pairing_session", Context.MODE_PRIVATE)

    override fun save(session: PairingSessionStore.Session?) {
        if (session == null) {
            prefs.edit().clear().commit()
            return
        }
        prefs.edit()
            .putString("stage", session.stage)
            .putString("pairing_id", session.pairingId)
            .putLong("started_at", session.startedAtMs)
            .putLong("updated_at", session.updatedAtMs)
            .putLong("expires_at", session.expiresAtEpochSeconds)
            .putBoolean("phrase_confirmed", session.phraseConfirmed)
            .commit()
    }

    override fun load(): PairingSessionStore.Session? {
        if (!prefs.contains("pairing_id")) return null
        return PairingSessionStore.Session(
            stage = prefs.getString("stage", "") ?: "",
            pairingId = prefs.getString("pairing_id", "") ?: "",
            startedAtMs = prefs.getLong("started_at", 0L),
            updatedAtMs = prefs.getLong("updated_at", 0L),
            expiresAtEpochSeconds = prefs.getLong("expires_at", 0L),
            phraseConfirmed = prefs.getBoolean("phrase_confirmed", false),
        )
    }
}

/** A Tower discovered on the LAN (service name + resolved address + port). */
data class DiscoveredTower(
    val serviceName: String,
    val host: String,
    val port: Int,
) {
    /** The discovered Tower becomes a LAN_IP endpoint candidate for the resolver. */
    fun toCandidate(priority: Int = 15): EndpointCandidate = EndpointCandidate(
        transportClass = TransportClass.LAN_IP,
        urlOrHost = "https://$host:$port",
        priority = priority,
        networkRequirement = NetworkRequirement.LAN_SAME_SUBNET,
        tlsRequirement = TlsRequirement.LAN_SELF_SIGNED_EXPECTED,
        expiryEpochSeconds = System.currentTimeMillis() / 1000L + 600L,
        source = EndpointCandidate.CandidateSource.LAN_DISCOVERY,
        qualified = true,
    )
}

/**
 * LAN_DISCOVERY method: NSD discovery of `_hermes-tower._tcp.` on the current
 * network. Discovery is bounded (stop() always), evidence-only (no pairing
 * state touched), and the results are Candidates — the SAME resolver pipeline
 * validates them (no bypass).
 */
class NsdTowerDiscovery(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? android.net.nsd.NsdManager

    private val _towers = MutableStateFlow<List<DiscoveredTower>>(emptyList())

    /** Live discovery results (deduplicated by host:port). */
    val towers: StateFlow<List<DiscoveredTower>> = _towers

    private var listener: android.net.nsd.NsdManager.DiscoveryListener? = null

    val isAvailable: Boolean get() = nsdManager != null

    /** Starts discovery; returns false when NSD is unavailable on this device. */
    fun start(): Boolean {
        val manager = nsdManager ?: return false
        if (listener != null) return true
        val l = object : android.net.nsd.NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                listener = null
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                listener = null
            }
            override fun onDiscoveryStopped(serviceType: String) {
                listener = null
            }
            override fun onServiceFound(serviceInfo: android.net.nsd.NsdServiceInfo) {
                resolve(serviceInfo)
            }
            override fun onServiceLost(serviceInfo: android.net.nsd.NsdServiceInfo) {
                _towers.value = _towers.value.filterNot { it.serviceName == serviceInfo.serviceName }
            }
        }
        listener = l
        runCatching { manager.discoverServices(SERVICE_TYPE, android.net.nsd.NsdManager.PROTOCOL_DNS_SD, l) }
            .onFailure { listener = null }
        return listener != null
    }

    private fun resolve(serviceInfo: android.net.nsd.NsdServiceInfo) {
        val manager = nsdManager ?: return
        runCatching {
            manager.resolveService(
                serviceInfo,
                object : android.net.nsd.NsdManager.ResolveListener {
                    override fun onResolveFailed(info: android.net.nsd.NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(info: android.net.nsd.NsdServiceInfo) {
                        val host = info.host?.hostAddress ?: return
                        val found = DiscoveredTower(info.serviceName ?: "hermes-tower", host, info.port)
                        if (_towers.value.none { it.host == found.host && it.port == found.port }) {
                            _towers.value = _towers.value + found
                        }
                    }
                },
            )
        }
    }

    fun stop() {
        val l = listener ?: return
        listener = null
        runCatching { nsdManager?.stopServiceDiscovery(l) }
    }

    companion object {
        const val SERVICE_TYPE = "_hermes-tower._tcp."
    }
}

/**
 * Manual advanced endpoint parsing (the ADVANCED_MANUAL method): the Owner
 * enters a URL; it becomes a candidate through the SAME pipeline. Returns null
 * when the entry is not a usable https URL (the UI shows the typed reason).
 */
object ManualEndpointParser {

    data class Result(val candidate: EndpointCandidate?, val typedCode: TransportFailureCode?, val detail: String?)

    fun parse(raw: String): Result {
        val text = raw.trim()
        if (text.isEmpty()) return Result(null, null, "inserisci un indirizzo")
        val withScheme = if (text.startsWith("http://")) text else if (text.startsWith("https://")) text else "https://$text"
        if (!withScheme.startsWith("https://")) {
            return Result(null, TransportFailureCode.CLEARTEXT_BLOCKED, "solo https è consentito")
        }
        val host = withScheme.substringAfter("://").substringBefore('/').substringBefore(':')
        if (host.isBlank() || !host.contains('.')) {
            return Result(null, TransportFailureCode.HTTP_PROTOCOL_ERROR, "indirizzo non valido")
        }
        val cls = classifyLegacyEndpoint(withScheme)
        return Result(
            EndpointCandidate(
                transportClass = TransportClass.MANUAL_ADVANCED,
                urlOrHost = withScheme.trimEnd('/'),
                priority = 60,
                networkRequirement = NetworkRequirement.INTERNET_ANY,
                tlsRequirement = inferTlsRequirement(cls),
                expiryEpochSeconds = System.currentTimeMillis() / 1000L + 900L,
                source = EndpointCandidate.CandidateSource.MANUAL_ADVANCED,
                qualified = true,
            ),
            null,
            null,
        )
    }
}
