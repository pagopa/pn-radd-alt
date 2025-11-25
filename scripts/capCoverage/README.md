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

### Esempio `.env`
```env
API_BASE_URL=https://your-api-server.com

# Cognito
COGNITO_USE_ID_TOKEN=true
COGNITO_REGION=eu-central-1
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_USERNAME=your.username@example.com
COGNITO_PASSWORD=YourSecurePassword123!
# Margine secondi prima della scadenza per refresh
COGNITO_TOKEN_MARGIN=30
```

## 📄 Utilizzo

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

## 🔐 Autenticazione Cognito
- USER_PASSWORD_AUTH con le variabili fornite.
- Il token (Access o Id in base a `COGNITO_USE_ID_TOKEN`) viene riutilizzato finché non è in prossimità della scadenza (`COGNITO_TOKEN_MARGIN`).
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
