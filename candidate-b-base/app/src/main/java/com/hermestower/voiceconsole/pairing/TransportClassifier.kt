package com.hermestower.voiceconsole.pairing

import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.CertificateException

/**
 * ANDROID REPAIR CP-005 — transport exception classifier (mission section 3).
 *
 * Distinguishes the TLS/network failure classes with REAL exception types —
 * no assumptions: DNS vs mDNS vs TCP vs TLS(CA/hostname/expiry/handshake) vs
 * cleartext vs HTTP protocol vs route. The classifier is the ONLY place a raw
 * exception becomes a typed code; user-facing layers never see stack traces.
 *
 * TRUST INVARIANT (PAIRING_TRUST_POLICY TRUST-1): classification NEVER
 * weakens TLS — it only names the failure.
 */
object TransportClassifier {

    fun classifyHost(host: String): (Exception) -> TransportFailureCode = { e -> classify(e, host) }

    fun classify(e: Exception, host: String = ""): TransportFailureCode {
        val h = host.ifBlank { hostOf(e) }
        return when (e) {
            is UnknownHostException ->
                if (h.endsWith(".local", true)) TransportFailureCode.MDNS_UNAVAILABLE
                else TransportFailureCode.DNS_RESOLUTION_FAILED
            is ConnectException -> TransportFailureCode.TCP_CONNECTION_FAILED
            is SocketTimeoutException -> TransportFailureCode.TCP_CONNECTION_FAILED
            is SSLPeerUnverifiedException -> TransportFailureCode.TLS_HOSTNAME_MISMATCH
            is CertificateExpiredException -> TransportFailureCode.TLS_CERTIFICATE_EXPIRED
            is CertificateNotYetValidException -> TransportFailureCode.TLS_CERTIFICATE_EXPIRED
            is SSLHandshakeException -> handshakeClass(e)
            is SSLProtocolException -> TransportFailureCode.TLS_HANDSHAKE_FAILED
            is SSLException -> TransportFailureCode.TLS_HANDSHAKE_FAILED
            is MalformedURLException -> TransportFailureCode.HTTP_PROTOCOL_ERROR
            else -> when {
                // HttpURLConnection cleartext policy (network_security_config: loopback only)
                e.message?.contains("Cleartext HTTP traffic", ignoreCase = true) == true ->
                    TransportFailureCode.CLEARTEXT_BLOCKED
                else -> TransportFailureCode.TCP_CONNECTION_FAILED
            }
        }
    }

    /** CA-untrusted vs generic handshake: a CertificateException cause means the trust chain failed. */
    private fun handshakeClass(e: SSLHandshakeException): TransportFailureCode =
        if (e.cause is CertificateException) TransportFailureCode.TLS_CA_UNTRUSTED
        else if (e.message?.contains("Certificate expired", ignoreCase = true) == true) TransportFailureCode.TLS_CERTIFICATE_EXPIRED
        else TransportFailureCode.TLS_HANDSHAKE_FAILED

    /** Best-effort host extraction from common exception messages (for the .local check). */
    private fun hostOf(e: Exception): String = e.message?.let { m ->
        Regex("(?:https?://)?([A-Za-z0-9._-]+)").find(m)?.groupValues?.get(1)
    } ?: ""

    /** Human Italian explanation per code (PAIRING_TYPED_FAILURE_CATALOG, no secrets). */
    fun explain(code: TransportFailureCode): String = when (code) {
        TransportFailureCode.OFFLINE_NO_NETWORK -> "Nessuna rete attiva. Connettiti al Wi-Fi o ai dati e riprova."
        TransportFailureCode.NO_ELIGIBLE_ENDPOINT -> "Nessun endpoint utilizzabile nel contesto di rete attuale. Il QR espone solo endpoint raggiungibili dalla rete di casa; sei su rete cellulare. Rientra nel Wi-Fi di casa oppure usa la configurazione manuale avanzata con l'indirizzo pubblico della Tower."
        TransportFailureCode.DNS_RESOLUTION_FAILED -> "Nome host non risolvibile su questa rete."
        TransportFailureCode.MDNS_UNAVAILABLE -> "Nome .local non risolvibile dal telefono: usa la scoperta LAN o l'indirizzo IP."
        TransportFailureCode.LAN_ROUTE_UNAVAILABLE -> "Rete di casa non raggiungibile da qui (endpoint solo-LAN)."
        TransportFailureCode.CELLULAR_CANNOT_REACH_LAN_ENDPOINT -> "Sei su rete cellulare e questo endpoint vive solo nella rete di casa."
        TransportFailureCode.TCP_CONNECTION_FAILED -> "Connessione TCP rifiutata o scaduta verso l'host."
        TransportFailureCode.TLS_CA_UNTRUSTED -> "Certificato della Tower non riconosciuto (autosfirmato sulla LAN). Fiducia manuale richiesta."
        TransportFailureCode.TLS_HOSTNAME_MISMATCH -> "Il certificato non copre il nome con cui lo stai contattando."
        TransportFailureCode.TLS_CERTIFICATE_EXPIRED -> "Certificato della Tower scaduto."
        TransportFailureCode.TLS_HANDSHAKE_FAILED -> "Negoziazione TLS fallita (protocollo/cifrari)."
        TransportFailureCode.CLEARTEXT_BLOCKED -> "Endpoint http:// non consentito: serve https."
        TransportFailureCode.HTTP_PROTOCOL_ERROR -> "Risposta non-HTTP o malformata dall'endpoint."
        TransportFailureCode.PAIRING_ROUTE_MISSING -> "La rotta di abbinamento non esiste su questa versione della Tower (aggiornamento server richiesto)."
        TransportFailureCode.PAIRING_ROUTE_REJECTED -> "Richiesta rifiutata dalla Tower."
        TransportFailureCode.BUDGET_EXHAUSTED -> "Tempo di ricerca endpoint esaurito."
    }
}
