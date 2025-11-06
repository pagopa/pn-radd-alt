#!/usr/bin/env node
const path = require('path');
const fs = require('fs');
const Processor = require('./src/patch-processor');
const yargs = require('yargs');
const { hideBin } = require('yargs/helpers');
require('dotenv').config();

const argv = yargs(hideBin(process.argv))
  .usage('Usage: $0 <file.csv> [options]')
  .command('$0 <file>', 'Aggiorna le date di validità delle coperture', (y) => {
    y.positional('file', {
      describe: 'Percorso file CSV',
      type: 'string'
    });
  })
  .option('api-url', {
    alias: 'u',
    describe: 'Base URL API',
    type: 'string',
    default: process.env.API_BASE_URL || 'https://api.example.com'
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
  .help()
  .alias('h', 'help')
  .argv;

async function main() {
  const csvFilePath = argv.file;
  if (!fs.existsSync(csvFilePath)) {
    console.error('File CSV non trovato:', csvFilePath);
    process.exit(1);
  }
  const processor = new Processor(argv.apiUrl, { useIdToken: argv.useIdToken, dryRun: argv.dryRun });
  try {
    const stats = await processor.processCsvFile(csvFilePath, argv.batchSize, argv.delay);
    console.log('\nRisultato finale:', stats);
  } catch (e) {
    console.error('Errore esecuzione:', e.message);
    process.exit(1);
  }
}

main();

