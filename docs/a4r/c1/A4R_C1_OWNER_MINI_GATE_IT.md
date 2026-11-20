# MINI-GATE FISICO OWNER — P1..P8 (Xiaomi 11 Lite 5G NE)

GRAPH A4R C1-C4 — CP-A4R-C1-014. Generato 2026-11-20T12:15:54Z.

## Cosa ti serve

- L'APK consegnato: `HERMES_VOICE_CONSOLE_A4R_REPAIRED_DEBUG.apk`
  (ARM64; sha256 nel file `A4R_REPAIRED_APK_SHA256.txt`).
- Il telefono Xiaomi 11 Lite 5G NE usato per il crash originale.
- **Non serve compilare, non serve SDK, non serve adb.**

## Regola d'oro

- UN passo alla volta. Rispondi sì/no per ogni passo.
- Se un passo fallisce, fermati lì e riporta la schermata (screenshot
  minimi, SENZA password, token, chiavi o challenge attivi).
- L'app si installa ACCANTO alle due app esistenti (non le sovrascrive):
  nuova app `Hermes Voice Console (A4R)` — pacchetto
  `com.hermestower.voiceconsole.a4rdebug`.

## Passi

| Passo | Cosa fai | Risultato atteso | Esito (sì/no) |
|---|---|---|---|
| P1 | Scarica l'APK dal link consegnato e verifica che il file sia valido (dimensione ~123 MB) | download completato, dimensione corrispondente | |
| P2 | Installa l'APK (apri il file dal file manager; consenti l'installazione) | l'app si installa senza errori, accanto alle altre | |
| P3 | Apri l'app | si apre SENZA crash (nessuna schermata di errore MIUI) | |
| P4 | Vai su Tower Studio → "Scansiona il QR della Tower"; concedi il permesso fotocamera quando richiesto | anteprima fotocamera attiva; se rifiuti, vedi lo stato tipizzato, non un crash | |
| P5 | Inquadra il QR della Tower (voice-console/v2) e digita il codice a 6 cifre mostrato dalla console Tower | il parsing va a buon fine ("Payload accettato dal parser v2") e la claim parte | |
| P6 | Conferma la frase a tre parole sulla console della Tower quando mostrata | l'app mostra "Tower associata (dispositivo …)" | |
| P7 | Chiudi completamente l'app (swipe via dalle recenti) | — | |
| P8 | Riapri l'app e verifica lo stato Tower | l'app risulta ancora associata: lo stato sopravvive al riavvio del processo | |

## Se P5/P6 fallisce

- Verifica che il QR sia ancora valido (TTL 2-5 minuti dal momento
  dell'apertura sulla Tower: riapri il QR sulla console e riprova).
- Lo scanner mostra SEMPRE uno stato tipizzato (QR scaduto / campo vietato /
  schema non supportato / endpoint non raggiungibile / dispositivo revocato)
  — riporta QUELLO stato: è esattamente l'evidenza che serve.
- Il vecchio crash `UnsupportedOperationException: Cannot initialize without
  an android.security.keystore.KeyGenParameterSpec` NON deve più comparire:
  se compare, fermati e riporta lo screenshot (significherebbe un
  regression non vista).

## Dopo P1-P8

Solo se TUTTI e otto i passi passano si apre la campagna completa
(codice numerico da solo, voce, Archive, download artifact, revoca,
replay, ri-associazione, riavvio Tower, LAN/Tailscale).
