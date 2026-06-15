# RADD Registry Geoloc Patch Script

Script Node.js per aggiornare le coordinate geografiche (`latitude`, `longitude`) dei punti di ritiro RADD tramite l'endpoint PATCH `/radd-bo/api/v2/registry/{locationId}` con autenticazione Cognito.

## Funzionalità
- Lettura di un CSV con colonne: `locationId, latitude, longitude`
- Supporto alias colonne (LOCATIONID, location_id, lat, lng, ecc.)
- Validazione formato coordinate (formato numero con max 6 decimali)
- Validazione range: latitude [-90, 90], longitude [-180, 180]
- Batch paralleli configurabili + delay tra batch
- Dry-run (nessuna chiamata, stampa cosa verrebbe inviato)
- Token Cognito riutilizzato fino a scadenza (margin configurabile)
- Possibilità di usare ID Token (`COGNITO_USE_ID_TOKEN=true`) se servono custom claims per l'authorizer

## Installazione
```bash
cd scripts/raddRegistryGeolocPatch
npm install
```

## Variabili Ambiente (`.env`)

### Login locale (utenti Cognito non federati)
```env
API_BASE_URL=https://api.radd.dev.notifichedigitali.it
COGNITO_REGION=eu-south-1
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_USERNAME=user@example.com
COGNITO_PASSWORD=SuperPassword123!
COGNITO_USE_ID_TOKEN=false         # opzionale
COGNITO_TOKEN_MARGIN=30            # opzionale (default 30)
CX_ID_AUTH_FLEET=operatore-001     # ID dell'operatore (header x-pagopa-pn-cx-id)
```

### Token statico (utenti SSO/Google)
```env
API_BASE_URL=https://api.radd.dev.notifichedigitali.it
API_TOKEN=eyJraWQiOiJ...           # idToken copiato dal portale helpdesk
CX_ID_AUTH_FLEET=operatore-001
```

> **Utenti SSO/Google**: il flusso SAML federato richiede un browser interattivo
> e non può essere automatizzato dalla CLI. Per ottenere un token:
> 1. Effettua il login sul portale helpdesk con il tuo account Google.
> 2. Apri DevTools → Application → Local Storage e copia il valore di `idToken`
>    (chiave del tipo `CognitoIdentityServiceProvider.<clientId>.<user>.idToken`).
> 3. Passa il token allo script con `--token <idToken>` oppure imposta
>    `API_TOKEN` nel `.env`.
>
> Il token Cognito ha durata limitata (60 minuti per default): se scade durante
> l'esecuzione è necessario rigenerarlo dal portale.

## 🏃 Esempi di utilizzo

### Esecuzione standard (con .env configurato)
```bash
node index.js data.csv
```

### Esecuzione con token (utenti SSO)

### Modalità SSO automatica (consigliata)
Lo script può aprire automaticamente il portale Helpdesk, attendere il login SSO e recuperare l'idToken dal browser:
```bash
node index.js data.csv --sso dev
```
Opzionale:
```bash
node index.js data.csv --sso dev --browser edge --helpdesk-url https://helpdesk.dev.notifichedigitali.it
```

Se la procedura automatica non funziona (browser non disponibile, errori Playwright, ecc.), puoi sempre inserire il token manualmente:
```bash
node index.js data.csv --token eyJraWQiOiJ...
```
oppure impostare `API_TOKEN` nel `.env` e lanciare lo script senza `--token`.

> **Best practice:** Prova prima `--sso` per comodità, ma tieni sempre a portata di mano la modalità manuale `--token` come fallback.

### Dry-run (Simulazione)
```bash
node index.js data.csv --dry-run
```

## Formato CSV
Minimo richiesto: `partnerId` (prima colonna) o un valore globale `--cx-id`/`CX_ID_AUTH_FLEET` come fallback, più `locationId`, `latitude`, `longitude`.

Esempio (partnerId come prima colonna):
```csv
partnerId,locationId,latitude,longitude
operatore-001,loc-001,41.9028,12.4964
operatore-001,loc-002,45.4642,9.1900
operatore-002,loc-003,40.8518,14.2681
```

Se preferisci, puoi omettere la colonna `partnerId` e passare un valore globale con `--cx-id` o impostare `CX_ID_AUTH_FLEET` nel file `.env` (verrà usato come fallback per tutte le righe).

Alias supportati:
- partnerId / PARTNERID / partner_id / CX_ID / cxId per `partnerId`
- LOCATIONID / location_id / LOCATION_ID / locId / LOC_ID per `locationId`
- LATITUDE / lat / LAT per `latitude`
- LONGITUDE / lng / LNG per `longitude`

## Uso
```bash
node index.js data/coordinates.csv --api-url https://api.radd.uat.notifichedigitali.it --batch-size 4 --delay 500 --cx-id operatore-001
```

Dry run:
```bash
node index.js data/coordinates.csv --dry-run --cx-id operatore-001
```

Usare ID Token:
```bash
node index.js data/coordinates.csv --use-id-token --cx-id operatore-001
```

## Output tipico
```
📊 Record validi da processare: 3
🔄 Batch 1/1 (3)
✅ PATCH ok loc-001 -> { latitude: 41.9028, longitude: 12.4964 }
✅ PATCH ok loc-002 -> { latitude: 45.4642, longitude: 9.1900 }
✅ PATCH ok loc-003 -> { latitude: 40.8518, longitude: 14.2681 }

📈 Completato: ✅ 3 ❌ 0 📦 3
```

## Errori e validazioni
- locationId mancante -> riga scartata
- Latitude/Longitude malformate (non numeri) -> riga scartata
- Latitude fuori range [-90, 90] -> riga scartata
- Longitude fuori range [-180, 180] -> riga scartata
- Coordinate con più di 6 decimali -> riga scartata

## Note sull'autenticazione
- Per attributi custom (es. `custom:backoffice_tags`) usare ID Token (env o `--use-id-token`)
- Access Token non contiene attributi custom
- Token rinnovato solo vicino alla scadenza (margin configurabile)
- Header `x-pagopa-pn-cx-id` deve contenere l'ID dell'operatore RADD

## Estensioni possibili
- Retry con backoff su 429/5xx
- Export risultati in JSON/CSV
- Log su file
- Supporto a input JSON oltre al CSV

## Licenza
ISC
