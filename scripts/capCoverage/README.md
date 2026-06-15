# CSV Coverage Processor

Un progetto Node.js che legge dati da file CSV e genera chiamate POST all'API `/radd-bo/api/v1/coverages`.

## 📋 Caratteristiche

- **Lettura CSV**: Supporta due formati di file:
  1. Storico: `COMUNE,Provincia,CAP,Cod catastale`
  2. Nuovo (abilitazioni): `CAP,LOCALITY,STARTVALIDITY,PUNTIPRESENTI` (province e codice catastale assenti / opzionali)
- **Mapping automatico**: Converte i dati CSV nel formato JSON richiesto dall'API
- **Gestione batch**: Processa i record in lotti per evitare di sovraccaricare l'API
- **Autenticazione Cognito**: Ottiene e riusa il token fino a prossimità scadenza
- **Logging dettagliato**: Output con statistiche di elaborazione
- **Configurabile**: Parametri via CLI o file `.env`

## 🚀 Installazione

```bash
# Clona il progetto (esempio)
# git clone <repository-url>
cd scripts/capCoverage

npm install
```

### Esempio `.env` — Login locale (utenti Cognito non federati)
```env
API_BASE_URL=https://your-api-server.com

# Cognito locale (USER_PASSWORD_AUTH)
COGNITO_REGION=eu-south-1
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_USERNAME=your.username@example.com
COGNITO_PASSWORD=YourSecurePassword123!
COGNITO_USE_ID_TOKEN=true
COGNITO_TOKEN_MARGIN=30
```

### Esempio `.env` — Token statico (utenti SSO/Google)
```env
API_BASE_URL=https://your-api-server.com

# idToken copiato dal portale helpdesk dopo il login SSO
API_TOKEN=eyJraWQiOiJ...
```

> **Utenti SSO/Google**: il flusso SAML federato richiede un browser interattivo e
> non può essere automatizzato dalla CLI. Per ottenere un token:
> 1. Effettua il login sul portale helpdesk con il tuo account Google.
> 2. Apri DevTools → Application → Local Storage e copia il valore di `idToken`
>    (chiave del tipo `CognitoIdentityServiceProvider.<clientId>.<user>.idToken`).
> 3. Passa il token allo script con `--token <idToken>` oppure imposta `API_TOKEN`
>    nel `.env`.
>
> Il token Cognito ha durata limitata (60 minuti per default): se scade durante
> l'esecuzione è necessario rigenerarlo dal portale.

## 🏃 Esempi di utilizzo

### Modalità locale (username/password)
Configura `COGNITO_USERNAME` e `COGNITO_PASSWORD` nel `.env`.
```bash
node index.js data.csv
```


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
DRY_RUN=true node index.js data.csv
```

## 📄 Utilizzo Avanzato
```bash
# Formato storico
node index.js data/sample.csv --api-url https://your-api-server.com

# Formato nuovo (abilitazioni CAP)
node index.js "data/Copertura RADD Abilitazione CAP-Località.csv" --api-url https://your-api-server.com --batch-size 3 --delay 500
```

## ⚙️ Opzioni CLI
| Opzione | Descrizione | Default |
|---------|-------------|---------|
| `--api-url` | URL base dell'API | `https://api.example.com` |
| `--batch-size` | Richieste concorrenti per batch | `5` |
| `--delay` | Ritardo ms tra i batch | `1000` |
| `--token` | idToken Cognito da usare direttamente (utenti SSO) | - |
| `--sso` | Ambiente SSO (dev/test/uat/hotfix/prod) per recupero token automatico | - |
| `--helpdesk-url` | URL Helpdesk custom da usare con `--sso` | - |
| `--browser` | Browser per `--sso`: `chrome`, `edge`, `chromium` | - |
| `--help` | Mostra help | - |

## 🧱 Formati CSV supportati

### Formato storico
```csv
COMUNE,Provincia,CAP,Cod catastale
Milano,MI,20100,F205
Roma,RM,00100,H501
```

### Formato nuovo abilitazioni
```csv
CAP,LOCALITY,STARTVALIDITY,PUNTIPRESENTI
00012,GUIDONIA MONTECELIO,2025-11-25,COPERTO
00015,MONTEROTONDO,2025-11-25,COPERTO
```

Note:
- `STARTVALIDITY` viene solo loggato (campo `_startValidityCsv`) e non inviato nel body POST perché lo schema di creazione non prevede start/end validity.
- `PUNTIPRESENTI` è ignorato.
- Province e codice catastale sono opzionali: se assenti non vengono inviati.

## 🔀 Mapping dei dati

Il payload inviato al POST deriva dai campi trovati:
```json
{
  "cap": "00012",
  "locality": "GUIDONIA MONTECELIO",
  "cadastralCode": "F205", // opzionale se presente
  "province": "RM"          // opzionale se presente
}
```

## 🔐 Autenticazione

Lo script supporta **due modalità** di autenticazione verso il pool Cognito:

### Modalità 1: Login locale (username/password)
- Flusso `USER_PASSWORD_AUTH` con le variabili `COGNITO_USERNAME` e `COGNITO_PASSWORD`.
- Il token viene riutilizzato finché non è in prossimità della scadenza (`COGNITO_TOKEN_MARGIN`).
- Disponibile solo per utenti Cognito **non federati**.

### Modalità 2: Token statico (utenti SSO/Google)
- L'utente effettua il login sul portale helpdesk con Google e copia l'idToken
  dal LocalStorage del browser.
- Il token viene passato allo script via `--token <idToken>` o `API_TOKEN`.
- Il token NON viene rinnovato automaticamente: se scade occorre rigenerarlo.

### Comune a entrambe
- Header: `Authorization: Bearer <token>`.
- Se ti serve il claim custom `custom:backoffice_tags` usa l'ID token (`COGNITO_USE_ID_TOKEN=true`).

## 🗓️ Validità
Per gestire start/end validity utilizza invece lo script di patch (`scripts/coverageValidityPatch/`), che invia questi campi via PATCH.

## 📈 Output di esempio
```
📖 Lettura CSV: data/Copertura RADD Abilitazione CAP-Località.csv
📊 Record validi da processare: 120
🔄 Batch 1/60 (2 record)
🗓️ CSV contiene startValidity='2025-11-25' (ignorato nel POST, disponibile solo in PATCH).
✅ Creato coverage GUIDONIA MONTECELIO (00012)
...
```

## 🛠 Dipendenze principali
- `csv-parser`
- `axios`
- `dotenv`
- `@aws-sdk/client-cognito-identity-provider`

## 🧪 Troubleshooting
| Problema | Possibile causa | Soluzione |
|----------|-----------------|-----------|
| `Value null at 'clientId'` | Mancano variabili Cognito | Verifica `COGNITO_CLIENT_ID`, utente e password nel `.env` |
| Claim custom mancante | Usando Access Token | Imposta `COGNITO_USE_ID_TOKEN=true` |
| Nessun record elaborato | Intestazioni non riconosciute | Verifica nomi colonne (vedi formati) |

## ✅ Requisiti minimi
- Node.js >= 16

## 📜 Licenza
Uso interno. Adatta secondo necessità.
