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

### Esempio `.env` — Login locale (username/password)
```env
API_BASE_URL=https://your-api-server.com

# Modalità autenticazione: "local" o "sso" (auto-detect se omesso)
AUTH_MODE=local

# Cognito locale
COGNITO_REGION=eu-south-1
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_USERNAME=your.username@example.com
COGNITO_PASSWORD=YourSecurePassword123!
COGNITO_USE_ID_TOKEN=true
COGNITO_TOKEN_MARGIN=30
```

### Esempio `.env` — SSO Google
```env
API_BASE_URL=https://your-api-server.com

# Modalità autenticazione
AUTH_MODE=sso

# Cognito SSO (Hosted UI con Google)
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_IDP_NAME=GoogleSAML-dev    # Nome del provider SAML (es. GoogleSAML-dev, GoogleSAML-uat)
COGNITO_REDIRECT_PORT=3000         # Default 3000. Deve essere configurato nei Redirect URL del client Cognito
COGNITO_USE_ID_TOKEN=true
COGNITO_TOKEN_MARGIN=30
ENV=dev                            # Ambiente (dev, uat, prod). Il dominio Cognito viene costruito automaticamente
```

> **Nota sull'SSO**: Per utilizzare la modalità SSO con Google, è necessario che l'URL `http://localhost:3000/callback` sia censito tra i "Callback URLs" del Client Cognito su AWS. Il dominio Cognito viene costruito automaticamente come `pn-helpdesk-<ENV>.auth.eu-south-1.amazoncognito.com`.

## 🏃 Esempi di utilizzo

### Modalità Locale (Username/Password)
Assicurarsi di avere `AUTH_MODE=local` o di aver configurato `COGNITO_USERNAME` e `COGNITO_PASSWORD` nel `.env`.
```bash
node index.js data.csv
```

### Modalità SSO (Google)
Assicurarsi di avere `AUTH_MODE=sso` e `COGNITO_DOMAIN` nel `.env`.
```bash
node index.js data.csv
```

In modalità SSO, lo script aprirà automaticamente il browser predefinito per il login Google. Una volta completato il login, il browser mostrerà un messaggio di successo e potrai chiudere la tab; lo script riprenderà l'esecuzione nel terminale.

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

### Modalità 2: SSO Google (Authorization Code + PKCE)
- Lo script avvia un server locale sulla porta `COGNITO_REDIRECT_PORT` (default 3000).
- Si apre il browser sulla Hosted UI di Cognito che redirige a Google.
- Dopo il login Google, il callback locale riceve il codice e lo scambia per i token.
- Il token viene riutilizzato come nella modalità locale.

### Comune a entrambe
- Header: `Authorization: Bearer <token>`.
- Se ti serve il claim custom `custom:backoffice_tags` usa l'ID token (`COGNITO_USE_ID_TOKEN=true`).
- Supporto alternativo: puoi fornire `API_TOKEN` con un JWT statico per bypassare completamente Cognito.

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
