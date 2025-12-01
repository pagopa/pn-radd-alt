#!/usr/bin/env node

const path = require('path');
const fs = require('fs');

// Pre-scan degli argomenti per individuare un eventuale --env-file <path>
let rawArgs = process.argv.slice(2);
let envFileIndex = rawArgs.indexOf('--env-file');
let customEnvPath = null;
if (envFileIndex !== -1 && rawArgs[envFileIndex + 1]) {
    customEnvPath = path.resolve(process.cwd(), rawArgs[envFileIndex + 1]);
}

// Se specificato --env-file usa quello, altrimenti forza .env nella cartella dello script
const defaultEnvPath = path.resolve(__dirname, '.env');
const envPathToLoad = customEnvPath || defaultEnvPath;

try {
    const dotenv = require('dotenv');
    const result = dotenv.config({ path: envPathToLoad });
    if (result.error) {
        console.warn(`⚠️  .env non caricato da '${envPathToLoad}' (${result.error.message}). Procedo comunque (usa variabili shell).`);
    } else {
        console.log(`🔧 Variabili ambiente caricate da '${envPathToLoad}'.`);
    }
} catch (e) {
    console.warn(`⚠️  Impossibile usare dotenv: ${e.message}`);
}

const CSVCoverageProcessor = require('./src/csv-processor');

async function main() {
    // Parse command line arguments
    const args = rawArgs; // già ottenuti

    // Help
    if (args.includes('--help') || args.length === 0) {
        console.log(`\n📂 CSV Coverage Processor\nUsage: node index.js <csv-file-path> [options]\n\nOptions:\n  --api-url <url>       API base URL (default: from .env or https://api.example.com)\n  --batch-size <num>    Numero di richieste concorrenti (default: 5)\n  --delay <ms>          Ritardo tra batch in ms (default: 1000)\n  --env-file <path>     Percorso file .env da caricare (default: ./scripts/capCoverage/.env)\n  --help                Mostra questo messaggio\n\nExample:\n  node index.js data/comuni.csv --api-url https://your-api.com --batch-size 3 --delay 2000\n  node index.js "data/Copertura RADD Abilitazione CAP-Località.csv" --env-file ./scripts/capCoverage/.env\n        `);
        process.exit(0);
    }

    const csvFilePath = args[0];
    let apiUrl = process.env.API_BASE_URL || 'https://api.example.com';
    let batchSize = 5;
    let delay = 1000;

    // Nuovo parsing argomenti (skip primo che è il file)
    for (let i = 1; i < args.length; i++) {
        const opt = args[i];
        switch (opt) {
            case '--api-url':
                apiUrl = args[i + 1];
                i++;
                break;
            case '--batch-size':
                batchSize = parseInt(args[i + 1]);
                i++;
                break;
            case '--delay':
                delay = parseInt(args[i + 1]);
                i++;
                break;
            case '--env-file':
                // Già gestito prima; consuma valore
                i++;
                break;
            case '--help':
                console.log('Help message shown sopra');
                process.exit(0);
                break;
            default:
                console.warn(`Unknown option: ${opt}`);
        }
    }

    if (!fs.existsSync(csvFilePath)) {
        console.error(`❌ CSV file not found: ${csvFilePath}`);
        process.exit(1);
    }

    console.log(`🚀 Starting CSV processing...`);
    console.log(`   📁 File: ${csvFilePath}`);
    console.log(`   🌐 API URL: ${apiUrl}`);
    console.log(`   📦 Batch size: ${batchSize}`);
    console.log(`   ⏱️  Delay: ${delay}ms\n`);

    try {
        const processor = new CSVCoverageProcessor(apiUrl);
        const result = await processor.processCsvFile(csvFilePath, batchSize, delay);
        
        console.log(`\n🎉 Processing completed successfully!`);
        process.exit(0);
    } catch (error) {
        console.error(`\n💥 Processing failed:`, error.message);
        process.exit(1);
    }
}

process.on('uncaughtException', (error) => {
    console.error('💥 Uncaught Exception:', error);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('💥 Unhandled Rejection at:', promise, 'reason:', reason);
    process.exit(1);
});

main();