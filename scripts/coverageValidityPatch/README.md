# Coverage Validity Patch Script

Script Node.js per aggiornare le date di validità (`startValidity`, `endValidity`), la provincia (`province`) e il codice catastale (`cadastralCode`) delle coperture RADD tramite l'endpoint PATCH `/radd-bo/api/v1/coverages/{cap}/{locality}` con autenticazione Cognito.

## Funzionalità
- Lettura di un CSV con colonne: `cap, locality, startValidity, endValidity, province, cadastralCode`
- Supporto alias colonne (CAP, COMUNE, Provincia, Cod catastale, start_validity, end_validity, ecc.)
- Validazione formato date `YYYY-MM-DD`
- Validazione CAP (5 cifre), provincia (2 lettere), codice catastale (4 alfanumerici)
- Controllo coerenza: `startValidity` non può essere successiva ad `endValidity`
- Batch paralleli configurabili + delay tra batch
- Dry-run (nessuna chiamata, stampa cosa verrebbe inviato)
- Token Cognito riutilizzato fino a scadenza (margin configurabile)
- Possibilità di usare ID Token (`COGNITO_USE_ID_TOKEN=true`) se servono custom claims per l'authorizer

## Installazione
```bash
cd scripts/coverageValidityPatch
npm install
```

## Variabili Ambiente (`.env`)
```env
API_BASE_URL=https://api.radd.notifichedigitali.it
COGNITO_REGION=eu-central-1
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_USERNAME=user@example.com
COGNITO_PASSWORD=SuperPassword123!
COGNITO_USE_ID_TOKEN=true          # opzionale
COGNITO_TOKEN_MARGIN=45            # opzionale (default 30)
```

## Formato CSV
Minimo richiesto: `cap`, `locality`. Gli altri campi sono opzionali; vengono aggiornati solo se presenti.

Esempio:
```csv
cap,locality,startValidity,endValidity,province,cadastralCode
00100,Roma,2025-01-01,2025-12-31,RM,H501
20100,Milano,2025-02-01,2025-11-30,MI,F205
09100,Cagliari,,2025-10-15,CA,B354
```

Alias supportati:
- CAP per `cap`
- COMUNE per `locality`
- Provincia / pr / PR per `province`
- Cod catastale / codCatastale / CODCATASTALE per `cadastralCode`
- start_validity / start / STARTVALIDITY per `startValidity`
- end_validity / end / ENDVALIDITY per `endValidity`

## Uso
```bash
node index.js data/validity.csv --api-url https://api.radd.uat.notifichedigitali.it --batch-size 4 --delay 500
```

Dry run:
```bash
node index.js data/validity.csv --dry-run
```

Usare ID Token:
```bash
node index.js data/validity.csv --use-id-token
```

## Output tipico
```
📊 Record validi da processare: 3
🔄 Batch 1/1 (3)
✅ PATCH ok 00100/Roma -> { startValidity: '2025-01-01', endValidity: '2025-12-31', province: 'RM', cadastralCode: 'H501' }
✅ PATCH ok 20100/Milano -> { startValidity: '2025-02-01', endValidity: '2025-11-30', province: 'MI', cadastralCode: 'F205' }
✅ PATCH ok 09100/Cagliari -> { endValidity: '2025-10-15', province: 'CA', cadastralCode: 'B354' }

📈 Completato: ✅ 3 ❌ 0 📊 3
```

## Errori e validazioni
- CAP non valido (non 5 cifre) -> riga scartata
- Date malformate -> riga scartata
- startValidity > endValidity -> riga scartata
- Provincia non 2 lettere -> riga scartata
- Codice catastale non 4 alfanumerici -> riga scartata
- Nessun campo da aggiornare (solo cap/locality senza altri campi) -> riga saltata

## Note sull'autenticazione
- Per attributi custom (es. `custom:backoffice_tags`) usare ID Token (env o `--use-id-token`)
- Access Token non contiene attributi custom
- Token rinnovato solo vicino alla scadenza (margin configurabile)

## Path Param locality & URL encoding
Il campo `locality` viene automaticamente URL-encodato prima di essere inserito nel path, per gestire spazi, apostrofi, caratteri accentati o simboli come `/`, `#`, `%`.
Esempi di conversione:
- "Sant'Agata Bolognese" -> `Sant%27Agata%20Bolognese`
- "Lido di Camaiore" -> `Lido%20di%20Camaiore`
- "Padergnò" -> `Padergn%C3%B2`
- "ACQUA/BUONA" -> `ACQUA%2FBUONA`

La normalizzazione Unicode (NFC) viene applicata prima dell'encoding per evitare problemi di confronti tra forme composte/decomposte.

## Estensioni possibili
- Retry con backoff su 429/5xx
- Export risultati in JSON/CSV
- Log su file
- Supporto a input JSON oltre al CSV

## Licenza
ISC
