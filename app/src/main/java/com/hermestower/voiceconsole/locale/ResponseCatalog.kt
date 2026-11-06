package com.hermestower.voiceconsole.locale

/**
 * GRAPH 4.10 CP-G4.10-002 (§5 LOCAL RESPONSE CATALOG).
 *
 * Every user-visible deterministic response lives here — never hard-coded
 * English inside a producer. Five locales with key-set parity enforced by
 * unit test (ResponseCatalogTest): Italian, English, Spanish, French, German.
 *
 * Truth rules preserved verbatim:
 *  - the Italian identity response is the Owner-mandated string;
 *  - the simulation explanation states SIMULATED truth in every locale
 *    ("Questa è una risposta simulata e deterministica. Non è stata
 *     contattata alcuna Tower reale.");
 *  - diagnostic reasons and protocol identifiers stay outside this catalog
 *    (event log / Advanced Diagnostics only, per §23).
 *
 * Format strings use %s (String.format, Locale.ROOT). Placeholders are
 * diagnostic values (status text, quoted transcript, receipt refs) or
 * already-localized strings produced upstream.
 */
enum class ResponseCategory {
    // §5 required categories
    GREETING,
    IDENTITY,
    HELP,
    CONSOLE_STATUS,
    CONNECTION_STATUS,
    PRIVACY_STATUS,
    VOICE_STATUS,
    MODEL_STATUS,
    TOWER_AUTHORITY,
    OFFLINE,
    QUEUED,
    CANCELLATION,
    LOCAL_ERROR,
    UNKNOWN_SAFE,
    SIMULATED_ACK,

    // deterministic Tower simulator variants (MockTowerBridge response strings)
    SIM_TOWER_STATUS,
    SIM_TOWER_PLAN,
    SIM_TOWER_REPORT,
    SIM_TOWER_APPROVAL,
    SIM_TOWER_IDENTITY,

    // ScriptedConsoleDriver turn/queue/error surface
    DECLINE,
    TIME_DATE,
    THANKS,
    TOWER_ACK,
    TOWER_PROGRESS,
    TOWER_RECEIPT,
    TOWER_ERROR,
    TOWER_ERROR_RETRYABLE_NOTE,
    TOWER_ERROR_FATAL_NOTE,

    // microphone / capture status notes (ConsoleVoiceCoordinator)
    PRIVACY_IGNORED_REQUEST,
    MIC_PRIVACY_REFUSAL,
    MIC_UNAVAILABLE,
    MIC_REFUSED,
    MIC_CAPTURE_STOPPED,
    MIC_NO_SPEECH,
    MIC_EMPTY,
    MIC_NETWORK,
    MIC_BUSY,
    MIC_PERMISSION_REVOKED,
    MIC_FAILED,

    // repeat action (§11)
    REPEAT_NOTHING_YET,

    // simulated bridge faults (MockTowerBridge) + localized error classifications
    BRIDGE_TRANSPORT_FAILURE,
    BRIDGE_TOWER_FAILURE,
    BRIDGE_ERR_TRANSPORT,
    BRIDGE_ERR_TOWER_INTERNAL,
    BRIDGE_ERR_OTHER,
}

object ResponseCatalog {

    val LOCALES: Set<String> = setOf("it", "en", "es", "fr", "de")

    fun isSupported(tag: String): Boolean = tag in LOCALES

    fun text(category: ResponseCategory, tag: String, vararg args: Any?): String {
        val locale = if (isSupported(tag)) tag else "en"
        val format = table[category]?.get(locale)
            ?: table[category]?.get("en")
            ?: error("ResponseCatalog missing entry for ${category.name}")
        return String.format(java.util.Locale.ROOT, format, *args)
    }

    /** All categories must exist in every locale (LocalizationParityTest for the catalog). */
    fun allCategories(): Set<ResponseCategory> = table.keys

    /** Non-blank for every category in the given locale. */
    fun isComplete(tag: String): Boolean =
        isSupported(tag) && table.values.all { it[tag]?.isNotBlank() == true }

    private val table: Map<ResponseCategory, Map<String, String>> = mapOf(

        ResponseCategory.GREETING to mapOf(
            "it" to "Buon giorno. La console vocale di Hermes è pronta.",
            "en" to "Good day. Hermes Console standing by.",
            "es" to "Buenos días. La consola vocal de Hermes está lista.",
            "fr" to "Bonjour. La console vocale Hermes est prête.",
            "de" to "Guten Tag. Die Hermes-Sprachkonsole ist bereit.",
        ),

        ResponseCategory.IDENTITY to mapOf(
            // Owner-mandated Italian identity response (briefing §5, verbatim).
            "it" to "Sono la console vocale di Hermes Tower. Gestisco localmente le funzioni della console, mentre Hermes Tower rimane l’autorità per le missioni e le operazioni complesse.",
            "en" to "I am the Hermes Console, the voice front of Hermes Tower. Locally I speak only for myself; the Tower remains the authority for missions and complex operations.",
            "es" to "Soy la consola vocal de Hermes Tower. Gestiono localmente las funciones de la consola, mientras Hermes Tower sigue siendo la autoridad para las misiones y las operaciones complejas.",
            "fr" to "Je suis la console vocale de Hermes Tower. Je gère localement les fonctions de la console, tandis que Hermes Tower reste l’autorité pour les missions et les opérations complexes.",
            "de" to "Ich bin die Sprachkonsole von Hermes Tower. Ich verwalte die Konsolenfunktionen lokal, während Hermes Tower die Autorität für Missionen und komplexe Operationen bleibt.",
        ),

        ResponseCategory.HELP to mapOf(
            "it" to "Posso rispondere localmente a saluti, orario, identità e stato veritiero del dispositivo. Tutto il resto arriva a Hermes Tower in modalità HYBRID, oppure viene messo in coda in sicurezza quando sei offline.",
            "en" to "I can answer locally for greetings, time, identity and truthful device status. Everything else reaches Hermes Tower in HYBRID mode, or queues safely when offline.",
            "es" to "Puedo responder localmente a saludos, hora, identidad y estado veraz del dispositivo. Todo lo demás llega a Hermes Tower en modo HYBRID, o se pone en cola de forma segura cuando estás sin conexión.",
            "fr" to "Je peux répondre localement aux salutations, à l’heure, à l’identité et à l’état réel de l’appareil. Tout le reste est transmis à Hermes Tower en mode HYBRID, ou mis en file d’attente en toute sécurité hors ligne.",
            "de" to "Ich kann lokal für Begrüßungen, Uhrzeit, Identität und den wahren Gerätestatus antworten. Alles andere erreicht Hermes Tower im HYBRID-Modus oder wird offline sicher in die Warteschlange gestellt.",
        ),

        ResponseCategory.CONSOLE_STATUS to mapOf(
            "it" to "Stato console: %s",
            "en" to "Console status: %s",
            "es" to "Estado de la consola: %s",
            "fr" to "État de la console : %s",
            "de" to "Konsolenstatus: %s",
        ),

        ResponseCategory.CONNECTION_STATUS to mapOf(
            "it" to "Stato connessione: %s",
            "en" to "Connection status: %s",
            "es" to "Estado de la conexión: %s",
            "fr" to "État de la connexion : %s",
            "de" to "Verbindungsstatus: %s",
        ),

        ResponseCategory.PRIVACY_STATUS to mapOf(
            "it" to "Richiesta ignorata: il microfono è disattivato (mute privacy).",
            "en" to "Request ignored: privacy mute is engaged.",
            "es" to "Solicitud ignorada: el micrófono está desactivado (silencio de privacidad).",
            "fr" to "Requête ignorée : le micro est désactivé (mute de confidentialité).",
            "de" to "Anfrage ignoriert: Das Mikrofon ist deaktiviert (Privatsphäre-Stummschaltung).",
        ),

        ResponseCategory.VOICE_STATUS to mapOf(
            "it" to "Voce attiva: %s",
            "en" to "Active voice: %s",
            "es" to "Voz activa: %s",
            "fr" to "Voix active : %s",
            "de" to "Aktive Stimme: %s",
        ),

        ResponseCategory.MODEL_STATUS to mapOf(
            "it" to "Stato modelli: %s",
            "en" to "Model status: %s",
            "es" to "Estado de los modelos: %s",
            "fr" to "État des modèles : %s",
            "de" to "Modellstatus: %s",
        ),

        ResponseCategory.TOWER_AUTHORITY to mapOf(
            "it" to "Hermes Tower rimane l’autorità per le missioni e le operazioni complesse; la console non esegue mai azioni consequenziali in locale.",
            "en" to "Hermes Tower remains the authority for missions and complex operations; the console never performs consequential actions locally.",
            "es" to "Hermes Tower sigue siendo la autoridad para las misiones y las operaciones complejas; la consola nunca realiza acciones consecuentes localmente.",
            "fr" to "Hermes Tower reste l’autorité pour les missions et les opérations complexes ; la console n’effectue jamais d’actions conséquentes localement.",
            "de" to "Hermes Tower bleibt die Autorität für Missionen und komplexe Operationen; die Konsole führt lokal nie Folgeschritte aus.",
        ),

        ResponseCategory.OFFLINE to mapOf(
            "it" to "Sei offline. Le funzioni locali restano disponibili; le richieste non consequenziali vengono messe in coda.",
            "en" to "You are offline. Local functions remain available; non-consequential requests are queued.",
            "es" to "Estás sin conexión. Las funciones locales siguen disponibles; las solicitudes no consecuentes se ponen en cola.",
            "fr" to "Vous êtes hors ligne. Les fonctions locales restent disponibles ; les requêtes non conséquentes sont mises en file d’attente.",
            "de" to "Du bist offline. Lokale Funktionen bleiben verfügbar; nicht-folgende Anfragen werden in die Warteschlange gestellt.",
        ),

        ResponseCategory.QUEUED to mapOf(
            "it" to "Richiesta messa in coda; verrà inviata alla Tower al ritorno della connessione.",
            "en" to "Request queued; it will be sent to the Tower when the connection returns.",
            "es" to "Solicitud en cola; se enviará a la Tower cuando vuelva la conexión.",
            "fr" to "Requête mise en file d’attente ; elle sera envoyée à la Tower au retour de la connexion.",
            "de" to "Anfrage in der Warteschlange; sie wird gesendet, sobald die Verbindung zur Tower zurückkehrt.",
        ),

        ResponseCategory.CANCELLATION to mapOf(
            "it" to "Turno annullato.",
            "en" to "Turn cancelled.",
            "es" to "Turno cancelado.",
            "fr" to "Tour annulé.",
            "de" to "Zug abgebrochen.",
        ),

        ResponseCategory.LOCAL_ERROR to mapOf(
            "it" to "Si è verificato un errore locale: %s",
            "en" to "A local error occurred: %s",
            "es" to "Se produjo un error local: %s",
            "fr" to "Une erreur locale s’est produite : %s",
            "de" to "Ein lokaler Fehler ist aufgetreten: %s",
        ),

        ResponseCategory.UNKNOWN_SAFE to mapOf(
            "it" to "Per ora questa console risponde solo agli intent locali consentiti; le tue parole sarebbero più al sicuro con la Tower in modalità HYBRID.",
            "en" to "This console answers only whitelisted local intents for now; your words would be safer with the Tower in HYBRID mode.",
            "es" to "Por ahora esta consola solo responde a intenciones locales permitidas; tus palabras estarían más seguras con la Tower en modo HYBRID.",
            "fr" to "Pour l’instant, cette console ne répond qu’aux intentions locales autorisées ; vos mots seraient plus en sécurité avec la Tower en mode HYBRID.",
            "de" to "Diese Konsole antwortet derzeit nur auf zugelassene lokale Absichten; Ihre Worte wären im HYBRID-Modus mit der Tower sicherer.",
        ),

        ResponseCategory.SIMULATED_ACK to mapOf(
            // Owner-mandated Italian simulation truth (briefing §5) embedded per locale.
            "it" to "Tower ha confermato: «%s». Questa è una risposta simulata e deterministica. Non è stata contattata alcuna Tower reale.",
            "en" to "Tower acknowledged: «%s». This is a simulated, deterministic response; no real Tower was contacted.",
            "es" to "Tower confirmó: «%s». Esta es una respuesta simulada y determinista; no se contactó ninguna Tower real.",
            "fr" to "Tower a confirmé : « %s ». Ceci est une réponse simulée et déterministe ; aucune Tower réelle n’a été contactée.",
            "de" to "Tower bestätigt: «%s». Dies ist eine simulierte, deterministische Antwort; es wurde keine echte Tower kontaktiert.",
        ),

        ResponseCategory.SIM_TOWER_STATUS to mapOf(
            "it" to "La Tower riferisce: la coda delle missioni è vuota in questa simulazione. Non esistono ancora dati reali della Tower; questa è una risposta deterministica simulata.",
            "en" to "Tower reports: mission queue is empty in this simulation. No real Tower data exists yet; this is a deterministic mock answer.",
            "es" to "Tower informa: la cola de misiones está vacía en esta simulación. Aún no existen datos reales de la Tower; esta es una respuesta simulada determinista.",
            "fr" to "Tower rapporte : la file des missions est vide dans cette simulation. Aucune donnée réelle de la Tower n’existe encore ; ceci est une réponse simulée déterministe.",
            "de" to "Tower meldet: Die Missions-Warteschlange ist in dieser Simulation leer. Es existieren noch keine echten Tower-Daten; dies ist eine simulierte, deterministische Antwort.",
        ),

        ResponseCategory.SIM_TOWER_PLAN to mapOf(
            "it" to "Capito. Posso predisporre un piano di missione in questa simulazione. Nel sistema reale pianificherei con attenzione e chiederei la tua approvazione prima di qualsiasi passo consequenziale.",
            "en" to "Understood. I can draft a mission plan in this simulation. In the real system I would plan carefully and ask for your approval before any consequential step.",
            "es" to "Entendido. Puedo elaborar un plan de misión en esta simulación. En el sistema real planificaría con cuidado y pediría tu aprobación antes de cualquier paso consecuente.",
            "fr" to "Compris. Je peux préparer un plan de mission dans cette simulation. Dans le système réel, je planifierais soigneusement et je demanderais votre approbation avant toute étape conséquente.",
            "de" to "Verstanden. Ich kann in dieser Simulation einen Missionsplan entwerfen. Im echten System würde ich sorgfältig planen und vor jedem Folgeschritt Ihre Zustimmung einholen.",
        ),

        ResponseCategory.SIM_TOWER_REPORT to mapOf(
            "it" to "Rapporto ricevuto e confermato dalla Tower simulata. Qui verrebbe emesso un riferimento di ricevuta.",
            "en" to "Report received and acknowledged by the simulated Tower. A receipt reference would be issued here.",
            "es" to "Informe recibido y confirmado por la Tower simulada. Aquí se emitiría una referencia de recibo.",
            "fr" to "Rapport reçu et confirmé par la Tower simulée. Une référence de récépissé serait émise ici.",
            "de" to "Bericht von der simulierten Tower empfangen und bestätigt. Hier würde eine Beleg-Referenz ausgestellt.",
        ),

        ResponseCategory.SIM_TOWER_APPROVAL to mapOf(
            "it" to "I flussi di approvazione sono di competenza della Tower. In produzione nulla di consequenziale procede senza la tua approvazione esplicita.",
            "en" to "Approval flows are Tower authority. In production, nothing consequential proceeds without your explicit approval.",
            "es" to "Los flujos de aprobación son autoridad de la Tower. En producción, nada consecuente procede sin tu aprobación explícita.",
            "fr" to "Les flux d’approbation relèvent de l’autorité de la Tower. En production, rien de conséquent ne se fait sans votre approbation explicite.",
            "de" to "Genehmigungsabläufe liegen in der Autorität der Tower. In der Produktion schreitet nichts Folgendes ohne Ihre ausdrückliche Genehmigung voran.",
        ),

        ResponseCategory.SIM_TOWER_IDENTITY to mapOf(
            "it" to "Sono Hermes Tower, edizione simulata: il lato autorità di questa console, ridotto a risposte deterministiche per la revisione V0.1.",
            "en" to "I am Hermes Tower, simulated edition: the authority side of this console, reduced to deterministic answers for V0.1 review.",
            "es" to "Soy Hermes Tower, edición simulada: el lado de autoridad de esta consola, reducido a respuestas deterministas para la revisión V0.1.",
            "fr" to "Je suis Hermes Tower, édition simulée : le côté autorité de cette console, réduit à des réponses déterministes pour la revue V0.1.",
            "de" to "Ich bin Hermes Tower, simulierte Ausgabe: die Autoritätsseite dieser Konsole, reduziert auf deterministische Antworten für die V0.1-Prüfung.",
        ),

        ResponseCategory.DECLINE to mapOf(
            "it" to "Non posso farlo.",
            "en" to "I cannot do that.",
            "es" to "No puedo hacer eso.",
            "fr" to "Je ne peux pas faire cela.",
            "de" to "Das kann ich nicht tun.",
        ),

        ResponseCategory.TIME_DATE to mapOf(
            "it" to "Orario e data sono letti da questo dispositivo: %s",
            "en" to "Time and date are read from this device: %s",
            "es" to "La hora y la fecha se leen de este dispositivo: %s",
            "fr" to "L’heure et la date sont lues sur cet appareil : %s",
            "de" to "Uhrzeit und Datum werden von diesem Gerät gelesen: %s",
        ),

        ResponseCategory.THANKS to mapOf(
            "it" to "Al tuo servizio.",
            "en" to "At your service.",
            "es" to "A su servicio.",
            "fr" to "À votre service.",
            "de" to "Zu Ihren Diensten.",
        ),

        ResponseCategory.TOWER_ACK to mapOf(
            "it" to "La Tower ha accettato la richiesta.",
            "en" to "Tower accepted the request.",
            "es" to "Tower aceptó la solicitud.",
            "fr" to "Tower a accepté la requête.",
            "de" to "Tower hat die Anfrage akzeptiert.",
        ),

        ResponseCategory.TOWER_PROGRESS to mapOf(
            "it" to "Tower: %s",
            "en" to "Tower: %s",
            "es" to "Tower: %s",
            "fr" to "Tower : %s",
            "de" to "Tower: %s",
        ),

        ResponseCategory.TOWER_RECEIPT to mapOf(
            "it" to "Ricevuta %s",
            "en" to "Receipt %s",
            "es" to "Recibo %s",
            "fr" to "Récépissé %s",
            "de" to "Beleg %s",
        ),

        ResponseCategory.TOWER_ERROR to mapOf(
            "it" to "La richiesta è fallita presso la Tower (%s). %s",
            "en" to "The request failed at the Tower (%s). %s",
            "es" to "La solicitud falló en la Tower (%s). %s",
            "fr" to "La requête a échoué au niveau de la Tower (%s). %s",
            "de" to "Die Anfrage ist bei der Tower fehlgeschlagen (%s). %s",
        ),

        ResponseCategory.TOWER_ERROR_RETRYABLE_NOTE to mapOf(
            "it" to "Nuovo tentativo disponibile per le richieste non consequenziali.",
            "en" to "Retry available for non-consequential requests.",
            "es" to "Reintento disponible para solicitudes no consecuentes.",
            "fr" to "Nouvel essai disponible pour les requêtes non conséquentes.",
            "de" to "Wiederholung für nicht-folgende Anfragen verfügbar.",
        ),

        ResponseCategory.TOWER_ERROR_FATAL_NOTE to mapOf(
            "it" to "Errore della Tower non ripetibile.",
            "en" to "Non-retryable Tower error.",
            "es" to "Error de la Tower no reintentable.",
            "fr" to "Erreur de la Tower non renouvelable.",
            "de" to "Nicht wiederholbarer Tower-Fehler.",
        ),

        ResponseCategory.PRIVACY_IGNORED_REQUEST to mapOf(
            "it" to "Richiesta ignorata: il microfono è disattivato (mute privacy).",
            "en" to "Request ignored: privacy mute is engaged.",
            "es" to "Solicitud ignorada: el micrófono está desactivado.",
            "fr" to "Requête ignorée : le micro est désactivé.",
            "de" to "Anfrage ignoriert: Das Mikrofon ist deaktiviert.",
        ),

        ResponseCategory.MIC_PRIVACY_REFUSAL to mapOf(
            "it" to "Il microfono è fisicamente disconnesso (mute privacy).",
            "en" to "Privacy mute is engaged — the microphone is physically disconnected.",
            "es" to "El silencio de privacidad está activado: el micrófono está físicamente desconectado.",
            "fr" to "Le mute de confidentialité est activé — le micro est physiquement déconnecté.",
            "de" to "Die Privatsphäre-Stummschaltung ist aktiv — das Mikrofon ist physisch getrennt.",
        ),

        ResponseCategory.MIC_UNAVAILABLE to mapOf(
            "it" to "Nessun riconoscitore vocale di sistema disponibile su questo dispositivo; usa l’input testuale.",
            "en" to "No system speech recognizer is available on this device — use text input.",
            "es" to "No hay ningún reconocedor de voz del sistema disponible en este dispositivo; usa la entrada de texto.",
            "fr" to "Aucun reconnaisseur vocal système n’est disponible sur cet appareil — utilisez la saisie texte.",
            "de" to "Auf diesem Gerät ist keine System-Spracherkennung verfügbar — bitte Texteingabe verwenden.",
        ),

        ResponseCategory.MIC_REFUSED to mapOf(
            "it" to "Il riconoscitore ha rifiutato l’avvio; usa l’input testuale.",
            "en" to "The recognizer refused to start — use text input.",
            "es" to "El reconocedor rechazó iniciarse; usa la entrada de texto.",
            "fr" to "Le reconnaisseur a refusé de démarrer — utilisez la saisie texte.",
            "de" to "Die Spracherkennung hat den Start verweigert — bitte Texteingabe verwenden.",
        ),

        ResponseCategory.MIC_CAPTURE_STOPPED to mapOf(
            "it" to "Acquisizione interrotta (%s).",
            "en" to "Capture stopped (%s).",
            "es" to "Captura detenida (%s).",
            "fr" to "Capture arrêtée (%s).",
            "de" to "Aufnahme gestoppt (%s).",
        ),

        ResponseCategory.MIC_NO_SPEECH to mapOf(
            "it" to "Nessun discorso rilevato nella finestra di ascolto.",
            "en" to "No speech detected within the listening window.",
            "es" to "No se detectó voz dentro de la ventana de escucha.",
            "fr" to "Aucune parole détectée dans la fenêtre d’écoute.",
            "de" to "In der Zuhör-Zeitspanne wurde keine Sprache erkannt.",
        ),

        ResponseCategory.MIC_EMPTY to mapOf(
            "it" to "Nessun discorso acquisito.",
            "en" to "No speech captured.",
            "es" to "No se capturó ninguna voz.",
            "fr" to "Aucune parole capturée.",
            "de" to "Keine Sprache aufgenommen.",
        ),

        ResponseCategory.MIC_NETWORK to mapOf(
            "it" to "Il riconoscitore di sistema richiede la rete per questa frase; lo STT offline è in fase di qualifica. L’input testuale funziona offline.",
            "en" to "The system recognizer needs network for this phrase — offline STT is pending qualification. Text input works offline.",
            "es" to "El reconocedor del sistema necesita red para esta frase; el STT offline está pendiente de calificación. La entrada de texto funciona sin conexión.",
            "fr" to "Le reconnaisseur système nécessite le réseau pour cette phrase — la STT hors ligne est en cours de qualification. La saisie texte fonctionne hors ligne.",
            "de" to "Die Systemerkennung benötigt für diesen Satz Netzwerk — die Offline-STT steht noch in der Qualifikation. Texteingabe funktioniert offline.",
        ),

        ResponseCategory.MIC_BUSY to mapOf(
            "it" to "Il riconoscitore è occupato; riprova.",
            "en" to "The recognizer is busy — try again.",
            "es" to "El reconocedor está ocupado; inténtalo de nuevo.",
            "fr" to "Le reconnaisseur est occupé — réessayez.",
            "de" to "Die Spracherkennung ist beschäftigt — bitte erneut versuchen.",
        ),

        ResponseCategory.MIC_PERMISSION_REVOKED to mapOf(
            "it" to "Il permesso del microfono è stato revocato durante l’acquisizione.",
            "en" to "Microphone permission was revoked during capture.",
            "es" to "El permiso del micrófono fue revocado durante la captura.",
            "fr" to "L’autorisation du micro a été révoquée pendant la capture.",
            "de" to "Die Mikrofon-Berechtigung wurde während der Aufnahme widerrufen.",
        ),

        ResponseCategory.MIC_FAILED to mapOf(
            "it" to "Riconoscimento vocale non riuscito (codice %s).",
            "en" to "Speech recognition failed (code %s).",
            "es" to "El reconocimiento de voz falló (código %s).",
            "fr" to "Échec de la reconnaissance vocale (code %s).",
            "de" to "Spracherkennung fehlgeschlagen (Code %s).",
        ),

        ResponseCategory.REPEAT_NOTHING_YET to mapOf(
            "it" to "Non è ancora stata pronunciata alcuna risposta.",
            "en" to "Nothing has been spoken yet.",
            "es" to "Todavía no se ha pronunciado ninguna respuesta.",
            "fr" to "Aucune réponse n’a encore été prononcée.",
            "de" to "Es wurde noch keine Antwort gesprochen.",
        ),

        ResponseCategory.BRIDGE_TRANSPORT_FAILURE to mapOf(
            "it" to "Guasto di trasporto simulato.",
            "en" to "Simulated transport failure.",
            "es" to "Fallo de transporte simulado.",
            "fr" to "Panne de transport simulée.",
            "de" to "Simulierter Transportfehler.",
        ),

        ResponseCategory.BRIDGE_TOWER_FAILURE to mapOf(
            "it" to "Errore interno simulato della Tower.",
            "en" to "Simulated Tower internal error.",
            "es" to "Error interno simulado de la Tower.",
            "fr" to "Erreur interne simulée de la Tower.",
            "de" to "Simulierter interner Tower-Fehler.",
        ),

        ResponseCategory.BRIDGE_ERR_TRANSPORT to mapOf(
            "it" to "trasporto",
            "en" to "transport",
            "es" to "transporte",
            "fr" to "transport",
            "de" to "Transport",
        ),

        ResponseCategory.BRIDGE_ERR_TOWER_INTERNAL to mapOf(
            "it" to "errore interno della Tower",
            "en" to "Tower internal error",
            "es" to "error interno de la Tower",
            "fr" to "erreur interne de la Tower",
            "de" to "interner Tower-Fehler",
        ),

        ResponseCategory.BRIDGE_ERR_OTHER to mapOf(
            "it" to "errore della Tower",
            "en" to "Tower error",
            "es" to "error de la Tower",
            "fr" to "erreur de la Tower",
            "de" to "Tower-Fehler",
        ),
    )
}
