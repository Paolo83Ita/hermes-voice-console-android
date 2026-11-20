# Guida al rollback — APK riparato GRAPH A4R (debug di qualifica)

Generato 2026-11-20T12:15:54Z.

## Disinstallare il candidato riparato (nessun impatto sugli altri)

- Impostazioni → App → "Hermes Voice Console (A4R)" → Disinstalla.
- Candidate A (`com.hermestower.voiceconsole`) e Candidate B
  (`com.hermestower.voiceconsole.debug`) restano installati e intatti:
  il candidato usa un pacchetto isolato per non toccarli.

## Tornare al comportamento precedente

- Nessuna azione serve: il candidato NON modifica dati delle altre app,
  né i file della Tower. Lo stato di pairing del candidato vive solo nelle
  sue SharedPreferences cifrate e nel suo AndroidKeyStore, e viene
  eliminato con la disinstallazione.
- Se la Tower ha un "dispositivo" in più nel registro (creato dal pairing
  del candidato), puoi revocarlo dalla console Tower (o non far nulla:
  la revoca è l'operazione prevista dal contratto).

## Se qualcosa va storto durante il pairing

- Lo stato tipizzato è visibile nella schermata di scansione (QR scaduto,
  campo vietato, schema non supportato, endpoint non raggiungibile,
  challenge rifiutata, salvataggio non riuscito, dispositivo revocato).
- Ripetere il pairing è sempre sicuro: nessuno stato parziale viene mai
  scritto (il commit di pairing è atomico: o tutto o nulla).
- Rollback documentato anche in `docs/a4r/A4R_ROLLBACK_PLAN.json`
  (piano A5) e nel ledger fisico `A4R_OWNER_PHYSICAL_LEDGER.json`.
