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

Lo script supporta **tre modalità** di autenticazione:

### Login SSO Google (Raccomandato)

In questa modalità lo script apre il browser per il login aziendale. Non occorre passare password né configurare il dominio manualmente: lo script lo costruisce automaticamente in base all'ambiente.

```bash
# Esempio comando per ambiente dev:
node index.js --sso dev <clientId> ./input/data.csv
```

Lo script utilizzerà automaticamente il dominio:
`pn-helpdesk-dev.auth.eu-south-1.amazoncognito.com`
e il provider:
`GoogleSAML-dev`

> **Nota sull'SSO**: Per il corretto funzionamento, l'URL `http://localhost:3000/callback` deve essere autorizzato nel Client Cognito.

---

### Login con token diretto

Se hai già un token (es. dal portale helpdesk), puoi passarlo direttamente:

```bash
node index.js --token <IL_TUO_ID_TOKEN> dev <clientId> ./input/data.csv
```

---

### Login locale (username/password)

```bash
node index.js dev <username> <password> <clientId> ./input/data.csv
```

### Parametri

| Parametro        | Descrizione                                                                   |
|------------------|-------------------------------------------------------------------------------|
| `--sso`          | Flag per attivare il login tramite browser (Google SSO)                       |
| `--token <jwt>`  | Passa un JWT direttamente, senza autenticazione                              |
| `<env>`          | Ambiente: `dev`, `test`, `uat`, `hotfix`, `prod`                              |
| `<clientId>`     | Client ID di Cognito (ApiClient)                                              |
| `<csvFilePath>`  | Percorso del file CSV. Il nome deve essere `<partnerId>-<descrizione>.csv`    |
| `<username>`     | Username per autenticazione Cognito (solo modo locale)                        |
| `<password>`     | Password per autenticazione Cognito (solo modo locale)                        |

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
