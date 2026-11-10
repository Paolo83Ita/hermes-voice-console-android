# OWNER POCO XIAOMI RETEST PACKAGE 9 — GRAPH 4.14 (§27, round 3)

Build: **0.3.9-alpha-graph4.14-round2-repairs / versionCode 12 / label OWNER
RUNTIME THEME TOWER PACKAGE 9**.

Artifact da installare: **ARM64-V8A** del build cloud **run #179** al tip
`7fc1c0b` — `app-arm64-v8a-debug.apk`
SHA-256 `bc79ced64ce6321943514148618ed83e1f188a050f040d686910649ff80190e8`
(122,639,026 byte). Installazione: `adb install -r app-arm64-v8a-debug.apk`
oppure trasferimento diretto e apertura del file — **Android Studio NON è
necessario**. Dispositivi: POCO X6 Pro (2311DRK48G, primario) + Xiaomi 11
Lite 5G NE.

Questa round verifica i 4 punti del feedback round-2 (G-A scritture nere nel
menù impostazioni; G-B errore di connessione su Qwen 2.5 0.5B; G-C cerchio
centrale sdoppiato e traslato; G-D nessun effetto aggiunto) e la regression
full A..W. Nessun verdetto è auto-concesso: PENDING resta PENDING finché il
Owner non registra l'esito fisico.

## POCO X6 Pro (round 3 — 23 step)

**Priorità round-2 (le 4 finding del Owner):**

1. **[G-A] Menù Impostazioni leggibile in scuro**: apri il drawer →
   Impostazioni → scorri TUTTE le schermate (hub, Aspetto, Modelli e Voce,
   Voce e Audio, Connection Studio, Diagnostica, Auto-test, Informazioni,
   dialoghi). NESSUN testo nero su fondo scuro: tutto leggibile al primo
   colpo d'occhio, senza ingrandimenti.
2. **[G-B] Qwen 2.5 0.5B — download END-TO-END**: Model Library → Qwen 2.5
   0.5B → scarica (~522 MB, rete reale). Il download deve COMPLETARE senza
   "errore di connessione". Se si verifica un'interruzione di rete, il
   download deve RIPRENDERE dal punto raggiunto (non ricominciare da zero e
   non corrompersi).
3. **[G-B/F] Verifica SHA al termine**: al termine l'app verifica l'impronta
   SHA-256 e passa a INSTALLATO (nessun "download incompleto"/quarantena su
   rete stabile).
4. **[G] Caricamento locale**: apri il modello installato → caricamento
   runtime llama locale (nessun errore runtime; la libreria nativa è
   verificata dentro l'APK).
5. **[H] Prompt funzionale**: invia il prompt esatto
   `Rispondi soltanto con: runtime locale operativo`
   → la risposta deve essere GENERATA dal modello locale (fromModel = vero,
   non la risposta mock fissa), con identità runtime/modello/hash visibili
   nella verità del pannello.
6. **[I] Annullamento**: avvia una generazione e premi Interrompi → torna a
   IDLE senza blocchi.
7. **[J] Scarico**: scarica/chiudi il modello → memoria restituita (read-back
   nello stato), riavvio → ricaricamento possibile.
8. **[G-C] Cerchio centrale — integrità durante CONTACTING**: nella Console,
   avvia una connessione (stato CONTATTING/in coda) e osserva il cerchio
   centrale: UN SOLO cerchio, centrato sull'anello della Torre, NESSUN
   sdoppiamento, NESSUN traslo verso il basso, NESSUN arco separato.
9. **[G-D] Effetti IDLE visibili ma sobri**: respirazione del portale più
   evidente (doveva essere percettibile), luce che scorre LUNGO l'anello
   (spazzata), nessuna rotazione delle parti meccaniche (ingranaggi/sigillo
   FERMi — se una parte ruota, è un difetto).
10. **[P] Ascolto**: luce ciano di ascolto accettata, più chiara dell'idle.
11. **[Q/R] Parlando**: luce soffusa sotto il portale ciano che risponde alla
    voce; NESSUNA linea lampeggiante in alto, NESSUN fascio duro, NESSUNO
    stroboscopio.

**Regression full:**

12. **[B] Versione e SHA**: Diagnostica mostra `0.3.9-alpha-graph4.14-round2-repairs`,
    versionCode 12, OWNER RUNTIME THEME TOWER PACKAGE 9, SHA `7fc1c0b`.
13. **[S] Bridge Mock**: pill/drawer = **SIMULAZIONE** con punto AMBRA — mai
    ciano "connesso", mai "TOWER OPERATIVA"; pannello verità = nessuna Torre
    reale collegata.
14. **[T] Marble regression**: Aspetto → MARBLE LIGHT → Console, Model
    Library, Voce e Audio, Connection Studio leggibili; ritorno a Obsidian;
    NESSUN riavvio richiesto per applicare il tema.
15. **[K] Riccardo**: installazione/qualifica/attivazione → sintesi reale
    sherpa con la frase
    `Benvenuto nella Torre di Hermes. Il canale vocale è pronto.`
16. **[L] Paola**: stessa sequenza con Paola.
17. **[M] A/B**: Riccardo → riproduci → Paola → riproduci: le identità runtime
    sono DISTINTE nel read-back (route truth, non etichetta).
18. **[N] Portale centrato**: portale/anello centrati sull'architettura della
    Torre (nessun scostamento visibile).
19. **[U] Interrompi**: unifica l'interruzione — ferma ascolto E parlato,
    nessuna animazione stantiva di SPEAKING rimasta.
20. **[V] Ascolto continuo**: stato di ascolto sostenuto senza eco.
21. **[W] QR scanner**: apre, scansiona il payload, ritorna a Connection.
22. **[E] Ripresa Qwen dopo interruzione** (se non già coperta al punto 2):
    metti in pausa la rete a metà download → reprise dal byte raggiunto →
    SHA finale OK.
23. **Riavvio**: impostazioni + stati qualificati persistono; versione/SHA
    invariati.

## Xiaomi 11 Lite 5G NE (spot: 8 step)

1. Installazione ARM64 + versione/SHA (step 12 sopra).
2. [G-A] Impostazioni leggibili in scuro (hub + 3 sottopagine a campione).
3. [G-C] Cerchio centrale singolo e centrato in CONTACTING.
4. [G-D] Respirazione portale + spazzata anello visibili in IDLE.
5. [K/L] Riccardo e Paola: frase di benvenuto, identità distinte.
6. [T] Marble ↔ Obsidian senza riavvio.
7. [U] Interrompi completa.
8. Riavvio: persistenza.

## Esiti da registrare (Owner only)

| # | Voce | Esito (OK / Difetto + descrizione) |
| --- | --- | --- |
| 1 | Impostazioni scuro leggibile | |
| 2 | Qwen download completo (rete reale) | |
| 3 | Qwen ripresa dopo interruzione | |
| 4 | Qwen SHA verificato → INSTALLATO | |
| 5 | Qwen caricamento locale | |
| 6 | Prompt `runtime locale operativo` generato dal modello | |
| 7 | Annullamento generazione | |
| 8 | Scarico/riavvio modello | |
| 9 | Cerchio centrale: nessun sdoppiamento/traslo (anche in CONTACTING) | |
| 10 | Effetti idle visibili (respiro + spazzata anello) | |
| 11 | Ascolto ciano / parlato sotto-luce / nessuna linea lampeggiante in alto | |
| 12 | Riccardo | |
| 13 | Paola | |
| 14 | A/B voci | |
| 15 | Bridge Mock ambra | |
| 16 | Marble regression | |
| 17 | Interrompi | |
| 18 | Ascolto continuo | |
| 19 | QR scanner | |
| 20 | Riavvio/persistenza | |

## CHECKPOINT / NEXT

- Esiti OK → il Owner seule può impostare `*_OWNER_VERIFIED` (§28).
- Qualsiasi difetto → registra dispositivo + schermata + passaggio esatto;
  il difetto entra come round 4 con lo stesso protocollo C-series.
- Stati attesi fino a prova fisica: QWEN_RUNTIME_IMPLEMENTED_PHYSICAL_RETEST_PENDING,
  RICCARDO_RUNTIME_IMPLEMENTED_PHYSICAL_RETEST_PENDING,
  PAOLA_RUNTIME_IMPLEMENTED_PHYSICAL_RETEST_PENDING,
  OBSIDIAN_CONTRAST_REPAIR_OWNER_RETEST_PENDING,
  TOWER_EFFECT_REPAIR_OWNER_RETEST_PENDING.
