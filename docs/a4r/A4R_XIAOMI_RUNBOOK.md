# A4R — Guida fisica per il Proprietario (Xiaomi 11 Lite 5G NE)

**Regola d'oro: un passo alla volta.** Nessun passo si salta; un passo fallito si
riporta TAL QUALE (testo a schermo, senza mai fotografare segreti) e si ripete SOLO
quello dopo la riparazione. Nessuna schermata con token, codici o chiavi.

## Preliminare — cosa ti serve
- Xiaomi 11 Lite 5G NE (1080×2400), WiFi connesso (LNAN o Tailscale dove disponibile).
- L'APK **B riparato** (quello consegnato dall'account Tower DOPO l'integrazione del
  pacchetto `A4R_CANDIDATE_B_KEYSTORE_REPAIR.zip`) — NON il build 908b289f che si è
  schiantato: quello resta registrato come evidenza.
- Facoltativo ma utile: `adb` sul PC per i numeri di risorsa (R1..R10).

## Fase 0 — Verifica identità (2 minuti)
1. **A8-01** Verifica l'hash dell'APK prima di installare (in-app o `adb shell pm
   hash`). Confronta con l'hash nella receipt di consegna. Se NON coincide → STOP,
   riporta.
2. **A8-02** Installa (o aggiorna, se il build riparato usa lo stesso keystore debug
   del precedente della Tower — altrimenti disinstalla prima il vecchio B).
3. **A8-03** Apri l'app a freddo: deve arrivare alla prima schermata senza crash.
   Se crasha: schermata MIUI feedback → inviala all'executor (è esattamente ciò che
   è successo con 908b289f).

## Fase 1 — Pairing (il percorso che si è rotto e che è stato riparato)
4. **A8-04** Genera il QR dalla Tower e scansiona (azioni sullo schermo: *Scansiona
   il QR della Tower* → perMESSO fotocamera → inquadra). Esito atteso: payload
   accettato, nessun crash. **Questo è il passo A4R-P1-001: il suo PASS chiude la
   prima P1.**
5. **A8-05** Ripeti con il **codice numerico** (*Inserisci codice numerico*).
6. **A8-06** Completa la **frase di conferma bilaterale** (la Tower mostra la
   frase; conferma sul telefono).

## Fase 2 — Voce (7→12)
7. **A8-07** Richiesta vocale push-to-talk: deve trascrivere e rispondere.
8. **A8-08** Chiedi una **data in italiano** («che giorno è oggi?»).
9. **A8-09** Chiedi un **calcolo con percentuale**.
10. **A8-10** Dettatura di un **codice prodotto** (deve restare identico).
11. **A8-11** Frase **lunga** end-to-end.
12. **A8-12** **Interruzione**: parla sopra la risposta TTS — deve fermarsi subito.

## Fase 3 — Rete e stati (13→14)
13. **A8-13** Aereo/ WiFi OFF a metà turno → riaccendi: il turno deve riprendere.
14. **A8-14** App in background 2 minuti → ritorno: sessione intatta.

## Fase 4 — Archive (15→18)
15. **A8-15** Apri **Archive**: elenco artefatti reali della Tower.
16. **A8-16** Scarica un **PDF** e aprilo.
17. **A8-17** Scarica uno **XLSX** e aprilo.
18. **A8-18** **Verifica SHA-256** dell'artefatto (in-app): deve corrispondere.

## Fase 5 — Revoca, replay, riparazioni (19→21)
19. **A8-19** Dalla Tower **revoca** il dispositivo: l'app deve rifletterlo.
20. **A8-20** Ri-usa un vecchio QR/codice (replay): deve essere **rifiutato**.
21. **A8-21** **Ri-associa** il dispositivo da zero.

## Fase 6 — Riavvii e reti (22→25)
22. **A8-22** Riavvia l'app: token/stato si **reidratano** (la riparazione A2).
23. **A8-23** Riavvia la Tower: la sessione riprende.
24. **A8-24** Pairing su endpoint **LAN**.
25. **A8-25** Pairing su **Tailscale** (se disponibile).

## Verifica visiva della Tower (A4)
- Schermata console con il monumento: il **portale circolare deve essere
  allineato** all'architettura, gli anelli concentrici, niente copie sdoppiate.
  Controlla: tema **chiaro e scuro**, scala testo **100% e 130%**, gesture bar,
  e (se puoi) tre larghezze diverse. Se accetti → A4R-TOWER-VISUAL-OWNER-ACCEPTED.

## Screenshot minimi richiesti
1. dispositivo associato; 2. una risposta vocale; 3. Archive/download;
4. la Tower visiva dopo la riparazione. **Mai** schermate con segreti; gli
screenshot di fallimento solo se un passo fallisce.

## Numeri di risorsa (facoltativi, R1..R10)
Con adb: dimensione installata, primo avvio, RSS baseline/peak (`dumpsys meminfo`),
batteria (`dumpsys batterystats`), termico (`dumpsys thermalservice`), comportamento
in background. L'app deve restare **utilizzabile Tower-first senza scaricare il
modello locale** (R6: profilo nuovo a modelli zero, gate completo).

## Registo
Ogni passo va nel `A4R_OWNER_PHYSICAL_LEDGER.json` (campi già pronti). L'executor
compila; tu riporti solo ciò che vedi. **Nessun PASS è dichiarato senza questa
evidenza fisica** — e il terminale `VOICE_CONSOLE_PAIRED_AND_VERIFIED` arriva solo
quando TUTTE le righe sono PASS e OPEN_P1 = 0.
