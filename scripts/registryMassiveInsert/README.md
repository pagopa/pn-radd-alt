# Script di Sincronizzazione Registri RADD

Questo script consente di sincronizzare i registri (registries) di un partner con quelli presenti in un file CSV. Viene
eseguita un'autenticazione, lettura dei dati dal CSV, confronto con i registri esistenti e aggiornamento remoto tramite
API.

## Requisiti

- Node.js 18+
- Accesso alle API RADD (`https://api.radd.<env>.notifichedigitali.it`)
- File CSV con nome `<partnerId>.csv`

## Installazione

1. Clona il repository o copia lo script in un progetto.
2. Installa le dipendenze (se previste):

   ```bash
   npm install
   ```

## Utilizzo

Lo script supporta **due modalità** di autenticazione:

### Login SSO Google (Raccomandato)

In questa modalità lo script apre il browser per il login aziendale. Non occorre passare password né configurare il dominio manualmente: lo script lo costruisce automaticamente in base all'ambiente.

```bash
# Esempio comando per ambiente uat:
node index.js --sso uat <clientId> ./input/data.csv
```

Lo script utilizzerà automaticamente il dominio:
`pn-helpdesk-uat.auth.eu-central-1.amazoncognito.com`
e il provider:
`GoogleSAML-uat`

---

### Login locale (username/password)

```bash
node index.js dev <username> <password> <clientId> ./input/data.csv
```

### Parametri e Variabili Ambiente

| Parametro / Var  | Descrizione                                                                   |
|------------------|-------------------------------------------------------------------------------|
| `--sso`          | Flag per attivare il login tramite browser (Google SSO)                       |
| `<env>`          | Ambiente: `dev`, `uat`, `prod`, ecc.                                          |
| `<clientId>`     | Client ID di Cognito (ApiClient)                                              |
| `<csvFilePath>`  | Percorso del file CSV da processare                                           |
| `COGNITO_DOMAIN` | Dominio Cognito (es: `prefix-dev.auth.eu-central-1.amazoncognito.com`)        |
| `COGNITO_IDP`    | (Var Ambiente) Nome del provider Google in Cognito (es: `GoogleSAML-dev`)     |

> **Nota sull'SSO**: Per il corretto funzionamento, l'URL `http://localhost:8087/callback` deve essere autorizzato nel Client Cognito.
| `<password>`     | Password per autenticazione Cognito (solo modo locale)                        |
| `<clientId>`     | Client ID per autenticazione Cognito                                          |
| `<csvFilePath>`  | Percorso completo al file CSV. Il nome del file deve essere `<partnerId>.csv` |
| `--sso`          | Abilita login SSO Google (non richiede username/password)                     |

### Variabili ambiente per SSO

| Variabile               | Descrizione                                      | Default    |
|-------------------------|--------------------------------------------------|------------|
| `COGNITO_DOMAIN`        | Dominio Cognito Hosted UI (obbligatorio per SSO) | -          |
| `COGNITO_REDIRECT_PORT` | Porta locale per il callback                     | `8087`     |
| `COGNITO_IDP_NAME`      | Nome Identity Provider configurato in Cognito    | `Google`   |

### Esempi

```bash
# Login locale
node index.js test myuser mypass abc123 ./csv/12345678901.csv

# SSO Google
export COGNITO_DOMAIN=pn-radd.auth.eu-central-1.amazoncognito.com
node index.js --sso test abc123 ./csv/12345678901.csv
```

## Cosa fa lo script

1. Legge il file CSV specificato.
2. Estrae il `partnerId` dal nome del file CSV.
3. Autentica l’utente tramite il client Cognito e ottiene un JWT.
4. Recupera i registri esistenti del partner.
5. Per ogni registro nel CSV:
    - Se esiste già un registro con stesso `locationId`, lo aggiorna con le informazioni fornite nel CSV.
    - Altrimenti crea un nuovo registro con i dati aggiornati.
6. Elimina tutti i registri esistenti non presenti nel CSV per lo stesso partnerId.
7. Salva un report dell’operazione in un file CSV.

## Output

Un file CSV di report con nome `report-<partnerId>-<timestamp>.csv` verrà generato nella directory di output.

## Avvertenze

- Lo script **sovrascrive** tutti i registri remoti con quelli del file CSV.
