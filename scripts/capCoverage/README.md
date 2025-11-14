# CSV Coverage Processor

Un progetto Node.js che legge dati da file CSV e genera chiamate POST all'API `/radd-bo/api/v1/coverages`.

## 📋 Caratteristiche

- **Lettura CSV**: Supporta file CSV con colonne COMUNE, Provincia, CAP, Cod catastale
- **Mapping automatico**: Converte i dati CSV nel formato JSON richiesto dall'API
- **Gestione batch**: Processa i record in lotti per evitare di sovraccaricare l'API
- **Error handling**: Gestione robusta degli errori con retry logic
- **Logging dettagliato**: Output colorato con statistiche di elaborazione
- **Configurabile**: Parametri personalizzabili via linea di comando o file .env

## 🚀 Installazione

```bash
# Clona il progetto
git clone <repository-url>
cd cd scripts/capCoverage

# Installa le dipendenze
npm install

# (Opzionale) Copia e configura il file environment
cp .env.example .env
```
### Esempio .env
```env
API_BASE_URL=https://your-api-server.com

# Cognito
COGNITO_USE_ID_TOKEN=true
COGNITO_REGION=eu-central-1
COGNITO_USER_POOL_ID=eu-central-1_XXXXXXX
COGNITO_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
COGNITO_USERNAME=your.username@example.com
COGNITO_PASSWORD=YourSecurePassword123!
```

## 📖 Utilizzo

### Utilizzo base
```bash
node index.js data/sample.csv
```

### Utilizzo avanzato con opzioni
```bash
node index.js data/comuni.csv \
  --api-url https://your-api-server.com \
  --batch-size 3 \
  --delay 2000
```

### Script npm
```bash
# Esegue con il file di esempio
npm run sample

# Esegue lo script principale
npm start data/your-file.csv
```

## ⚙️ Opzioni

| Opzione | Descrizione | Default |
|---------|-------------|---------|
| `--api-url` | URL base dell'API | `https://api.example.com` |
| `--batch-size` | Numero di richieste concurrent | `5` |
| `--delay` | Ritardo tra i batch (ms) | `1000` |
| `--help` | Mostra il messaggio di aiuto | - |

## 📁 Struttura del progetto

```
├── index.js                 # Script principale
├── src/
│   └── csv-processor.js     # Classe per elaborazione CSV e API
├── data/
│   └── sample.csv          # File CSV di esempio
├── .vscode/
│   └── tasks.json          # Task VS Code
├── package.json            # Configurazione npm
└── README.md              # Documentazione
```

## 📊 Formato CSV richiesto

Il file CSV deve contenere le seguenti colonne:

```csv
COMUNE,Provincia,CAP,Cod catastale
Milano,MI,20100,F205
Roma,RM,00100,H501
Torino,TO,10100,L219
```

## 🔄 Mapping dei dati

I dati CSV vengono mappati nel seguente formato JSON per l'API:

```json
{
  "cap": "20100",
  "locality": "Milano", 
  "cadastralCode": "F205",
  "province": "MI"
}
```

## 🌐 Configurazione API

### Endpoint
- **URL**: `/radd-bo/api/v1/coverages`
- **Metodo**: POST
- **Content-Type**: application/json

### Autenticazione Cognito
Il processor ottiene un AccessToken tramite USER_PASSWORD_AUTH con le variabili ambiente fornite. Il token:
- Viene decodificato per leggere il campo exp
- È riutilizzato fino a 30 secondi prima della scadenza
- Inserito nell'header: Authorization: Bearer <AccessToken>

Se l'API richiede solo un token statico puoi ancora usare:
```env
API_TOKEN=your-jwt-token-here
```

## 📈 Output di esempio

```
🚀 Starting CSV processing...
   📁 File: data/sample.csv
   🌐 API URL: https://api.example.com
   📦 Batch size: 5
   ⏱️  Delay: 1000ms

📖 Reading CSV file: data/sample.csv
📊 Found 5 valid records to process
🔄 Processing batch 1/1 (5 records)
✅ Successfully created coverage for Milano (20100)
✅ Successfully created coverage for Roma (00100)
❌ Failed to create coverage for Torino (10100): Network error
✅ Successfully created coverage for Napoli (80100)
✅ Successfully created coverage for Palermo (90100)

📈 Processing complete:
   ✅ Successful: 4
   ❌ Failed: 1
   📊 Total: 5

🎉 Processing completed successfully!
```

## 🛠️ Sviluppo

### Requisiti
- Node.js >= 14.0.0
- npm >= 6.0.0

### Dipendenze
- `csv-parser`: Parsing file CSV
- `axios`: Client HTTP per chiamate API
- `dotenv`: Gestione variabili d'ambiente
- @aws-sdk/client-cognito-identity-provider

### Task VS Code
Il progetto include task VS Code preconfigurati:
- **Run CSV Processor**: Esegue lo script con il file di esempio

## 🚨 Gestione errori

Il script gestisce diversi tipi di errori:

- **File non trovato**: Verifica l'esistenza del file CSV
- **Dati mancanti**: Skips records con campi obbligatori vuoti  
- **Errori di rete**: Retry automatico e logging dettagliato
- **Timeout API**: Timeout configurabile (10 secondi default)