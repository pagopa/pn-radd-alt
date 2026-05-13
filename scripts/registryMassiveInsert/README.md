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

### Login con token (utenti SSO/Google)

Il flusso SAML federato richiede un browser interattivo e non può essere
automatizzato dalla CLI. Per gli utenti Google occorre quindi ottenere
manualmente l'idToken dal portale helpdesk:

1. Effettua il login sul portale helpdesk con il tuo account Google.
2. Apri DevTools → Application → Local Storage e copia il valore di `idToken`
   (chiave del tipo `CognitoIdentityServiceProvider.<clientId>.<user>.idToken`).
3. Passa il token allo script:

```bash
node index.js --token <IL_TUO_ID_TOKEN> dev <clientId> ./input/data.csv
```

In alternativa puoi usare l'automatismo browser:

```bash
node index.js --sso dev <clientId> ./input/data.csv
```

Con `--sso` lo script apre automaticamente `https://helpdesk.<env>.notifichedigitali.it`,
attende il login interattivo e legge l'idToken dal LocalStorage del browser.
Per compliance, prova prima browser di sistema (Chrome/Edge) e usa Chromium Playwright solo come fallback.
Se vuoi forzare una URL specifica:

```bash
node index.js --sso dev <clientId> ./input/data.csv --helpdesk-url https://helpdesk.dev.notifichedigitali.it
```

Se vuoi forzare il browser:

```bash
node index.js --sso dev <clientId> ./input/data.csv --browser edge
```

Valori supportati per `--browser`: `chrome`, `edge`, `chromium`.

> Il token Cognito ha durata limitata (60 minuti per default): se scade durante
> l'esecuzione è necessario rigenerarlo dal portale.

---

### Login locale (utenti Cognito non federati)

```bash
node index.js dev <username> <password> <clientId> ./input/data.csv
```

### Parametri

| Parametro        | Descrizione                                                                   |
|------------------|-------------------------------------------------------------------------------|
| `--token <jwt>`  | idToken Cognito da usare direttamente (utenti SSO)                            |
| `--sso`          | Apre Helpdesk, attende login SSO e recupera automaticamente l'idToken         |
| `--helpdesk-url <url>` | URL Helpdesk custom da usare con `--sso`                               |
| `--browser <name>` | Browser da usare con `--sso`: `chrome`, `edge`, `chromium`                  |
| `<env>`          | Ambiente: `dev`, `test`, `uat`, `hotfix`, `prod`                              |
| `<clientId>`     | Client ID di Cognito (ApiClient)                                              |
| `<csvFilePath>`  | Percorso del file CSV. Il nome deve essere `<partnerId>-<descrizione>.csv`    |
| `<username>`     | Username per autenticazione Cognito (solo modalità locale)                    |
| `<password>`     | Password per autenticazione Cognito (solo modalità locale)                    |

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
