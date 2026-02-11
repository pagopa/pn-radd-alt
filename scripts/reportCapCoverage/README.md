# CSV Coverage Processor

Un progetto Node.js che legge dati da file CSV e genera chiamate POST all'API `/radd-bo/api/v1/coverages`.

## 📋 Caratteristiche

- **Lettura CSV**: Supporta file CSV con colonne COMUNE, Provincia, CAP, Cod catastale
- **Mapping automatico**: Converte i dati CSV nel formato JSON richiesto dall'API
- **Gestione batch**: Processa i record in lotti per evitare di sovraccaricare l'API
# CSV Coverage Processor

Un piccolo progetto Node.js per estrarre e processare dati di CAP / località da file di log o CSV e, se necessario, inviare i dati all'endpoint API `/radd-bo/api/v1/coverages`.

## Contenuto aggiornato
- script principale per l'invio a API: `index.js`
- script di utilità per estrarre coppie distinte CAP / LOCALITY da log: `extract-cap-locality.js`
- output estrazione (file CSV): `data/cap-locality.csv`

## Requisiti
- Node.js 14+ (consigliato 16+)
- npm

## Installazione

```bash
git clone <repository-url>
cd "Script creazione CAP coverage"
npm install
```

## File importanti
- `index.js` — script principale che legge CSV e invia richieste all'API.
- `extract-cap-locality.js` — script che estrae coppie uniche CAP,LOCALITY dai file di log (o CSV) e genera `data/cap-locality.csv`.
- `data/sample.csv` — esempio input per `index.js`.
- `data/cap-locality.csv` — output generato dallo script di estrazione (header `CAP,LOCALITY`).

## Estrarre CAP e LOCALITY (utility)

Lo script `extract-cap-locality.js` legge il file di log (o qualsiasi file di testo con messaggi simili a quelli presenti in `logs-insights-results.csv`), estrae le coppie `CAP,LOCALITY`, rimuove i duplicati e scrive il risultato ordinato in `data/cap-locality.csv`.

Esempio di esecuzione (usa il file di log scaricato):

```bash
node extract-cap-locality.js /Users/alessandro.masci/Downloads/logs-insights-results.csv
```

Output prodotto (default):

```
data/cap-locality.csv  # file CSV con header: CAP,LOCALITY
```

Note:
- Lo script effettua una `distinct` sulle coppie `CAP,LOCALITY` e salva i risultati ordinati per CAP.
- Se il pattern del log cambia, adattare la regex in `extract-cap-locality.js`.

## Uso di `index.js` (invio a API)

Esempio base:

```bash
node index.js data/sample.csv --api-url https://api.example.com
```

Opzioni utili (se implementate nello script):
- `--api-url` : URL base dell'API (es. `https://api.example.com`)
- `--batch-size` : numero di richieste per batch
- `--delay` : ritardo tra batch in ms

Controlla la top-level `index.js` per tutte le opzioni supportate.

## Formato CSV richiesto

Per `index.js` il CSV di input tipico contiene (almeno) queste colonne:

```
COMUNE,Provincia,CAP,Cod catastale
```

Esempio:

```
Milano,MI,20100,F205
Roma,RM,00100,H501
```

## Output dell'estrazione CAP/LOCALITY

Il file `data/cap-locality.csv` è generato nello stesso workspace e ha formato:

```
CAP,LOCALITY
00100,ROMA
20100,MILANO
```

## Suggerimenti operativi
- Verifica i permessi di lettura/scrittura nella cartella `data/`.
- Per file molto grandi, lo script `extract-cap-locality.js` legge riga-per-riga (streaming) per contenere l'utilizzo di memoria.
- Se vuoi integrare l'output con `index.js`, usa `data/cap-locality.csv` come input dopo eventuale trasformazione.

## Contribuire

1. Fork
2. Crea un branch (`git checkout -b feature/nome`)
3. Commit e PR

---

File aggiornato: vedi `extract-cap-locality.js` e `data/cap-locality.csv` per esempi e output.

