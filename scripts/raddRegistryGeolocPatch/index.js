#!/usr/bin/env node
const path = require('path');
const fs = require('fs');

// Pre-scan argomenti per trovare --env-file
let rawArgs = process.argv.slice(2);
let envIdx = rawArgs.indexOf('--env-file');
let customEnvPath = null;
if (envIdx !== -1 && rawArgs[envIdx + 1]) {
  customEnvPath = path.resolve(process.cwd(), rawArgs[envIdx + 1]);
}
const defaultEnvPath = path.resolve(__dirname, '.env');
const envPathToLoad = customEnvPath || defaultEnvPath;

try {
  const dotenv = require('dotenv');
  const result = dotenv.config({ path: envPathToLoad });
  if (result.error) {
    console.warn(`⚠️ .env non caricato da '${envPathToLoad}' (${result.error.message}). Userò variabili shell.`);
  } else {
    console.log(`🔧 Variabili ambiente caricate da '${envPathToLoad}'.`);
  }
} catch (e) {
  console.warn(`⚠️ Impossibile caricare .env: ${e.message}`);
}

const Processor = require('./src/geoloc-patch-processor');
const yargs = require('yargs');
const { hideBin } = require('yargs/helpers');

const argv = yargs(hideBin(process.argv))
  .usage('Usage: $0 <file.csv> [options]')
  .command('$0 <file>', 'Aggiorna le coordinate geografiche dei punti di ritiro RADD', (y) => {
    y.positional('file', { describe: 'Percorso file CSV', type: 'string' });
  })
  .option('api-url', {
    alias: 'u',
    describe: 'Base URL API',
    type: 'string',
    default: process.env.API_BASE_URL || 'https://api.example.com'
  })
  .option('cx-id', {
    alias: 'c',
    describe: 'ID dell\'operatore RADD (header x-pagopa-pn-cx-id)',
    type: 'string',
    default: process.env.CX_ID_AUTH_FLEET || null
  })
  .option('batch-size', {
    alias: 'b',
    describe: 'Numero richieste parallele per batch',
    type: 'number',
    default: 5
  })
  .option('delay', {
    alias: 'd',
    describe: 'Delay ms tra batch',
    type: 'number',
    default: 1000
  })
  .option('dry-run', {
    describe: 'Non effettua PATCH, mostra solo cosa farebbe',
    type: 'boolean',
    default: false
  })
  .option('use-id-token', {
    describe: 'Usa IdToken invece di AccessToken (COGNITO_USE_ID_TOKEN=true)',
    type: 'boolean',
    default: (process.env.COGNITO_USE_ID_TOKEN || 'false').toLowerCase() === 'true'
  })
  .option('env-file', {
    describe: 'Percorso file .env da caricare',
    type: 'string',
    default: envPathToLoad
  })
  .help()
  .alias('h', 'help')
  .argv;

async function main() {
  const csvFilePath = argv.file;
  if (!fs.existsSync(csvFilePath)) {
    console.error('❌ File CSV non trovato:', csvFilePath);
    process.exit(1);
  }
  
  // cxId can be provided globally via --cx-id or per-row in the CSV's first column.
  if (!argv.cxId && !process.env.CX_ID_AUTH_FLEET) {
    console.log('ℹ️ Nessun --cx-id fornito: lo script userà il valore della prima colonna del CSV come partnerId (x-pagopa-pn-cx-id)');
  }

  const processor = new Processor(argv.apiUrl, argv.cxId || process.env.CX_ID_AUTH_FLEET || null, { useIdToken: argv.useIdToken, dryRun: argv.dryRun });
  try {
    const stats = await processor.processCsvFile(csvFilePath, argv.batchSize, argv.delay);
    console.log('\n✅ Risultato finale:', stats);
  } catch (e) {
    console.error('💥 Errore esecuzione:', e.message);
    process.exit(1);
  }
}

main();
