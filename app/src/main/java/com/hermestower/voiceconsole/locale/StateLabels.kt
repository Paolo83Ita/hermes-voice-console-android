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

        // GRAPH 4.12 §21: ordinary-UI enum localization (raw value only in
        // Advanced Diagnostics). Model install states surfaced on cards:
        "DISCOVERED" to mapOf(
            "it" to "Scoperto", "en" to "Discovered", "es" to "Descubierto", "fr" to "Découvert", "de" to "Erkannt",
        ),
        "COMPATIBILITY_PENDING" to mapOf(
            "it" to "Compatibilità in attesa", "en" to "Compatibility pending", "es" to "Compatibilidad pendiente", "fr" to "Compatibilité en attente", "de" to "Kompatibilität offen",
        ),
        "INCOMPATIBLE" to mapOf(
            "it" to "Non compatibile", "en" to "Incompatible", "es" to "Incompatible", "fr" to "Incompatible", "de" to "Inkompatibel",
        ),
        "LICENSE_PENDING" to mapOf(
            "it" to "Licenza da verificare", "en" to "License review pending", "es" to "Licencia pendiente", "fr" to "Licence à vérifier", "de" to "Lizenzprüfung offen",
        ),
        "DOWNLOAD_APPROVAL_REQUIRED" to mapOf(
            "it" to "Approvazione download richiesta", "en" to "Download approval required", "es" to "Se requiere aprobación de descarga", "fr" to "Approbation du téléchargement requise", "de" to "Download-Freigabe erforderlich",
        ),
        "DOWNLOADING" to mapOf(
            "it" to "Download in corso", "en" to "Downloading", "es" to "Descargando", "fr" to "Téléchargement", "de" to "Wird geladen",
        ),
        "DOWNLOAD_PAUSED" to mapOf(
            "it" to "Download in pausa", "en" to "Download paused", "es" to "Descarga en pausa", "fr" to "Téléchargement en pause", "de" to "Download pausiert",
        ),
        "DOWNLOADED" to mapOf(
            "it" to "Scaricato", "en" to "Downloaded", "es" to "Descargado", "fr" to "Téléchargé", "de" to "Geladen",
        ),
        "HASH_VERIFICATION_PENDING" to mapOf(
            "it" to "Verifica hash in attesa", "en" to "Hash verification pending", "es" to "Verificación de hash pendiente", "fr" to "Vérification du hachage en attente", "de" to "Hash-Prüfung offen",
        ),
        "HASH_VERIFIED" to mapOf(
            "it" to "Hash verificato", "en" to "Hash verified", "es" to "Hash verificado", "fr" to "Hachage vérifié", "de" to "Hash geprüft",
        ),
        "INSTALLING" to mapOf(
            "it" to "Installazione in corso", "en" to "Installing", "es" to "Instalando", "fr" to "Installation", "de" to "Wird installiert",
        ),
        "INSTALLED" to mapOf(
            "it" to "Installato", "en" to "Installed", "es" to "Instalado", "fr" to "Installé", "de" to "Installiert",
        ),
        "QUALIFICATION_PENDING" to mapOf(
            "it" to "Qualifica in attesa", "en" to "Qualification pending", "es" to "Cualificación pendiente", "fr" to "Qualification en attente", "de" to "Qualifizierung offen",
        ),
        "QUALIFIED" to mapOf(
            "it" to "Qualificato", "en" to "Qualified", "es" to "Cualificado", "fr" to "Qualifié", "de" to "Qualifiziert",
        ),
        "ACTIVE" to mapOf(
            "it" to "Attivo", "en" to "Active", "es" to "Activo", "fr" to "Actif", "de" to "Aktiv",
        ),
        "FAILED" to mapOf(
            "it" to "Non riuscito", "en" to "Failed", "es" to "Fallido", "fr" to "Échec", "de" to "Fehlgeschlagen",
        ),
        "REMOVED" to mapOf(
            "it" to "Rimosso", "en" to "Removed", "es" to "Eliminado", "fr" to "Supprimé", "de" to "Entfernt",
        ),
        // Local LLM runtime truth (§11):
        "LOCAL_LLM_RUNTIME_UNAVAILABLE" to mapOf(
            "it" to "Motore locale non disponibile", "en" to "Local engine unavailable", "es" to "Motor local no disponible", "fr" to "Moteur local indisponible", "de" to "Lokale Engine nicht verfügbar",
        ),
        "RUNTIME_MISSING" to mapOf(
            "it" to "Runtime assente", "en" to "Runtime missing", "es" to "Runtime ausente", "fr" to "Runtime absent", "de" to "Runtime fehlt",
        ),
        "MODEL_READY" to mapOf(
            "it" to "Modello pronto", "en" to "Model ready", "es" to "Modelo listo", "fr" to "Modèle prêt", "de" to "Modell bereit",
        ),
        // History source / delivery enums:
        "LOCAL" to mapOf(
            "it" to "Locale", "en" to "Local", "es" to "Local", "fr" to "Local", "de" to "Lokal",
        ),
        "TOWER" to mapOf(
            "it" to "Torre", "en" to "Tower", "es" to "Torre", "fr" to "Tour", "de" to "Turm",
        ),
        "USER" to mapOf(
            "it" to "Utente", "en" to "User", "es" to "Usuario", "fr" to "Utilisateur", "de" to "Nutzer",
        ),
        "DELIVERED" to mapOf(
            "it" to "Consegnato", "en" to "Delivered", "es" to "Entregado", "fr" to "Livré", "de" to "Zugestellt",
        ),
        "PENDING" to mapOf(
            "it" to "In attesa", "en" to "Pending", "es" to "Pendiente", "fr" to "En attente", "de" to "Ausstehend",
        ),
        "QUEUED" to mapOf(
            "it" to "In coda", "en" to "Queued", "es" to "En cola", "fr" to "En file", "de" to "In Warteschlange",
        ),

        // ---- GRAPH 4.13 §18: raw enums never render in ordinary UI (§26 pin) ----
        "NOT_DOWNLOADED" to mapOf(
            "it" to "Non scaricato", "en" to "Not downloaded", "es" to "No descargado", "fr" to "Non téléchargé", "de" to "Nicht heruntergeladen",
        ),
        "VERIFIED_DOWNLOADABLE" to mapOf(
            "it" to "Download verificato", "en" to "Download verified", "es" to "Descarga verificada", "fr" to "Téléchargement vérifié", "de" to "Download verifiziert",
        ),
        "RUNTIME_AVAILABLE" to mapOf(
            "it" to "Runtime pronto", "en" to "Runtime ready", "es" to "Runtime listo", "fr" to "Runtime prêt", "de" to "Laufzeit bereit",
        ),
        // ---- GRAPH 4.14 §7: completed capability-ladder states ----
        "NOT_AVAILABLE" to mapOf(
            "it" to "Non disponibile", "en" to "Not available", "es" to "No disponible", "fr" to "Indisponible", "de" to "Nicht verfügbar",
        ),
        "DOWNLOAD_ALLOWED" to mapOf(
            "it" to "Download consentito", "en" to "Download allowed", "es" to "Descarga permitida", "fr" to "Téléchargement autorisé", "de" to "Download erlaubt",
        ),
        "RUNTIME_RESOLVED" to mapOf(
            "it" to "Runtime verificato", "en" to "Runtime resolved", "es" to "Runtime resuelto", "fr" to "Runtime résolu", "de" to "Laufzeit gebunden",
        ),
        "ACTIVE_READBACK_VERIFIED" to mapOf(
            "it" to "Attivo e verificato", "en" to "Active and verified", "es" to "Activo y verificado", "fr" to "Actif et vérifié", "de" to "Aktiv und verifiziert",
        ),
        "FUNCTIONAL_TEST_PASSED" to mapOf(
            "it" to "Test funzionale superato", "en" to "Functional test passed", "es" to "Prueba funcional superada", "fr" to "Test fonctionnel réussi", "de" to "Funktionstest bestanden",
        ),
        "PHYSICAL_QUALIFICATION_PENDING" to mapOf(
            "it" to "Richiesto test sul dispositivo", "en" to "Device test required", "es" to "Requerida prueba en el dispositivo", "fr" to "Test sur l'appareil requis", "de" to "Gerätetest erforderlich",
        ),
        "QUARANTINED" to mapOf(
            "it" to "In quarantena", "en" to "Quarantined", "es" to "En cuarentena", "fr" to "En quarantaine", "de" to "Quarantäne",
        ),
        "LOADED" to mapOf(
            "it" to "Caricato", "en" to "Loaded", "es" to "Cargado", "fr" to "Chargé", "de" to "Geladen",
        ),
        "TEST_FIXTURE_ONLY" to mapOf(
            "it" to "Solo fixture di test", "en" to "Test fixture only", "es" to "Solo accesorio de prueba", "fr" to "Fixature de test uniquement", "de" to "Nur Test-Fixture",
        ),
        "CANDIDATE_PENDING_DISCOVERY" to mapOf(
            "it" to "Disponibile nel catalogo", "en" to "Available in the catalog", "es" to "Disponible en el catálogo", "fr" to "Disponible dans le catalogue", "de" to "Im Katalog verfügbar",
        ),
        "SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED" to mapOf(
            "it" to "Download bloccato: origine non verificata", "en" to "Download blocked: source unverified", "es" to "Descarga bloqueada: origen no verificado", "fr" to "Téléchargement bloqué : source non vérifiée", "de" to "Download blockiert: Quelle unverifiziert",
        ),
        "LICENSE_BLOCKED" to mapOf(
            "it" to "Licenza in revisione", "en" to "License under review", "es" to "Licencia en revisión", "fr" to "Licence en révision", "de" to "Lizenz in Prüfung",
        ),
        "COMPATIBILITY_BLOCKED" to mapOf(
            "it" to "Non compatibile", "en" to "Not compatible", "es" to "No compatible", "fr" to "Non compatible", "de" to "Nicht kompatibel",
        ),
        "SUPERSEDED" to mapOf(
            "it" to "Sostituito da versione più recente", "en" to "Superseded by a newer version", "es" to "Sustituido por una versión más reciente", "fr" to "Remplacé par une version plus récente", "de" to "Durch neuere Version ersetzt",
        ),
        "REJECTED" to mapOf(
            "it" to "Respinto", "en" to "Rejected", "es" to "Rechazado", "fr" to "Rejeté", "de" to "Abgelehnt",
        ),
        "RUNTIME_BUNDLED" to mapOf(
            "it" to "Incluso nell'app", "en" to "Included with the app", "es" to "Incluido con la aplicación", "fr" to "Inclus avec l'application", "de" to "Mit der App enthalten",
        ),
        "REFUSED" to mapOf(
            "it" to "Rifiutato", "en" to "Refused", "es" to "Rechazado", "fr" to "Refusé", "de" to "Abgelehnt",
        ),
        "PASSED" to mapOf(
            "it" to "Superato", "en" to "Passed", "es" to "Superada", "fr" to "Réussi", "de" to "Bestanden",
        ),
    )

    /** The localized label for a state identifier; falls back to the identifier itself. */
    fun label(stateName: String, localeTag: String): String {
        val locale = ResponseLocaleResolver.normalize(localeTag) ?: "en"
        return table[stateName]?.get(locale) ?: table[stateName]?.get("en") ?: stateName
    }

    fun hasLabel(stateName: String): Boolean = table.containsKey(stateName)
}
