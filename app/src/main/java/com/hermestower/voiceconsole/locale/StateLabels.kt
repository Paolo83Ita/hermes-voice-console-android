package com.hermestower.voiceconsole.locale

/**
 * GRAPH 4.10 CP-G4.10-009 (§23 RAW ENUM LOCALIZATION) — the ONE label table
 * for product states (Owner finding P0-09: raw English values like "Invalid"
 * were visible in ordinary UI).
 *
 * Rule: never display a raw enum value in ordinary UI. Raw identifiers remain
 * available ONLY in Advanced Diagnostics / event log. Italian labels are the
 * Owner-mandated §23 table verbatim; the other locales carry the parity set.
 */
object StateLabels {

    private val table: Map<String, Map<String, String>> = mapOf(
        // ---- ConnectionConfigState / connection statuses (§23 table) ----
        "INVALID" to mapOf(
            "it" to "Non valida", "en" to "Invalid", "es" to "No válida", "fr" to "Non valide", "de" to "Ungültig",
        ),
        "VALIDATED" to mapOf(
            "it" to "Validata", "en" to "Validated", "es" to "Validada", "fr" to "Validée", "de" to "Validiert",
        ),
        "CONNECTED" to mapOf(
            "it" to "Connessa", "en" to "Connected", "es" to "Conectada", "fr" to "Connectée", "de" to "Verbunden",
        ),
        "DISCONNECTED" to mapOf(
            "it" to "Disconnessa", "en" to "Disconnected", "es" to "Desconectada", "fr" to "Déconnectée", "de" to "Getrennt",
        ),
        "DEGRADED" to mapOf(
            "it" to "Degradata", "en" to "Degraded", "es" to "Degradada", "fr" to "Dégradée", "de" to "Beeinträchtigt",
        ),
        "AUTHENTICATION_FAILED" to mapOf(
            "it" to "Autenticazione non riuscita", "en" to "Authentication failed",
            "es" to "Autenticación fallida", "fr" to "Échec de l’authentification", "de" to "Authentifizierung fehlgeschlagen",
        ),
        "PROTOCOL_MISMATCH" to mapOf(
            "it" to "Protocollo incompatibile", "en" to "Protocol mismatch",
            "es" to "Protocolo incompatible", "fr" to "Protocole incompatible", "de" to "Protokoll inkompatibel",
        ),
        "TIMEOUT" to mapOf(
            "it" to "Tempo scaduto", "en" to "Timed out", "es" to "Tiempo agotado", "fr" to "Délai dépassé", "de" to "Zeitüberschreitung",
        ),
        "PAIRING_REQUIRED" to mapOf(
            "it" to "Associazione richiesta", "en" to "Pairing required",
            "es" to "Emparejamiento requerido", "fr" to "Appairage requis", "de" to "Kopplung erforderlich",
        ),
        "REVOKED" to mapOf(
            "it" to "Accesso revocato", "en" to "Access revoked", "es" to "Acceso revocado", "fr" to "Accès révoqué", "de" to "Zugriff widerrufen",
        ),
        "EMPTY" to mapOf(
            "it" to "Nessuna configurazione", "en" to "No configuration", "es" to "Sin configuración", "fr" to "Aucune configuration", "de" to "Keine Konfiguration",
        ),
        "DRAFT" to mapOf(
            "it" to "Bozza", "en" to "Draft", "es" to "Borrador", "fr" to "Brouillon", "de" to "Entwurf",
        ),
        "PAIRED" to mapOf(
            "it" to "Associata", "en" to "Paired", "es" to "Emparejada", "fr" to "Appairée", "de" to "Gekoppelt",
        ),
        "CONNECTION_TEST_PENDING" to mapOf(
            "it" to "Verifica connessione in corso", "en" to "Connection test pending",
            "es" to "Prueba de conexión pendiente", "fr" to "Test de connexion en attente", "de" to "Verbindungstest ausstehend",
        ),
        "CERTIFICATE_ERROR" to mapOf(
            "it" to "Errore del certificato", "en" to "Certificate error",
            "es" to "Error de certificado", "fr" to "Erreur de certificat", "de" to "Zertifikatfehler",
        ),
        "ROLLBACK_AVAILABLE" to mapOf(
            "it" to "Configurazione precedente disponibile", "en" to "Previous configuration available",
            "es" to "Configuración anterior disponible", "fr" to "Configuration précédente disponible", "de" to "Vorherige Konfiguration verfügbar",
        ),

        // ---- TowerConnectionTester.Outcome ----
        "VALIDATION_PASSED" to mapOf(
            "it" to "Validazione superata", "en" to "Validation passed", "es" to "Validación superada", "fr" to "Validation réussie", "de" to "Validierung bestanden",
        ),
        "CONNECTION_PASSED" to mapOf(
            "it" to "Connessione riuscita", "en" to "Connection passed", "es" to "Conexión correcta", "fr" to "Connexion réussie", "de" to "Verbindung erfolgreich",
        ),
        "HOST_UNREACHABLE" to mapOf(
            "it" to "Host non raggiungibile", "en" to "Host unreachable", "es" to "Host inalcanzable", "fr" to "Hôte injoignable", "de" to "Host nicht erreichbar",
        ),
        "TLS_FAILED" to mapOf(
            "it" to "TLS non riuscito", "en" to "TLS failed", "es" to "TLS fallido", "fr" to "Échec TLS", "de" to "TLS fehlgeschlagen",
        ),
        "TOWER_REJECTED" to mapOf(
            "it" to "Tower ha rifiutato", "en" to "Tower rejected", "es" to "Tower rechazó", "fr" to "Tower a refusé", "de" to "Tower abgelehnt",
        ),
        "STREAMING_UNAVAILABLE" to mapOf(
            "it" to "Streaming non disponibile", "en" to "Streaming unavailable",
            "es" to "Streaming no disponible", "fr" to "Streaming indisponible", "de" to "Streaming nicht verfügbar",
        ),
        "CONNECTION_TIMEOUT" to mapOf(
            "it" to "Connessione scaduta", "en" to "Connection timeout", "es" to "Tiempo de conexión agotado", "fr" to "Délai de connexion dépassé", "de" to "Verbindungs-Timeout",
        ),

        // ---- TransportMode ----
        "MOCK_SIMULATOR" to mapOf(
            "it" to "Simulatore Mock", "en" to "Mock simulator", "es" to "Simulador Mock", "fr" to "Simulateur Mock", "de" to "Mock-Simulator",
        ),
        "HTTP_SECURE" to mapOf(
            "it" to "HTTPS sicuro", "en" to "Secure HTTPS", "es" to "HTTPS seguro", "fr" to "HTTPS sécurisé", "de" to "Sicheres HTTPS",
        ),
        "SECURE_WEBSOCKET" to mapOf(
            "it" to "WebSocket sicuro", "en" to "Secure WebSocket", "es" to "WebSocket seguro", "fr" to "WebSocket sécurisé", "de" to "Sicheres WebSocket",
        ),
        "HTTPS_PLUS_SECURE_WEBSOCKET" to mapOf(
            "it" to "HTTPS + WebSocket sicuro", "en" to "HTTPS + secure WebSocket",
            "es" to "HTTPS + WebSocket seguro", "fr" to "HTTPS + WebSocket sécurisé", "de" to "HTTPS + sicheres WebSocket",
        ),
        "TAILSCALE_PRIVATE" to mapOf(
            "it" to "Indirizzo privato Tailscale", "en" to "Tailscale private address",
            "es" to "Dirección privada de Tailscale", "fr" to "Adresse privée Tailscale", "de" to "Private Tailscale-Adresse",
        ),
    )

    /** The localized label for a state identifier; falls back to the identifier itself. */
    fun label(stateName: String, localeTag: String): String {
        val locale = ResponseLocaleResolver.normalize(localeTag) ?: "en"
        return table[stateName]?.get(locale) ?: table[stateName]?.get("en") ?: stateName
    }

    fun hasLabel(stateName: String): Boolean = table.containsKey(stateName)
}
