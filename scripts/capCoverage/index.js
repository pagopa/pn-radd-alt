#!/usr/bin/env node

const path = require('path');
const CSVCoverageProcessor = require('./src/csv-processor');

async function main() {
    // Parse command line arguments
    const args = process.argv.slice(2);
    
    // Check for help first
    if (args.includes('--help') || args.length === 0) {
        console.log(`
📋 CSV Coverage Processor
Usage: node index.js <csv-file-path> [options]

Options:
  --api-url <url>     API base URL (default: from .env or https://api.example.com)
  --batch-size <num>  Number of concurrent requests (default: 5)
  --delay <ms>        Delay between batches in milliseconds (default: 1000)
  --help              Show this help message

Example:
  node index.js data/comuni.csv --api-url https://your-api.com --batch-size 3 --delay 2000
        `);
        process.exit(0);
    }

    const csvFilePath = args[0];
    let apiUrl = process.env.API_BASE_URL || 'https://api.example.com';
    let batchSize = 5;
    let delay = 1000;

    // Parse options
    for (let i = 1; i < args.length; i += 2) {
        switch (args[i]) {
            case '--api-url':
                apiUrl = args[i + 1];
                break;
            case '--batch-size':
                batchSize = parseInt(args[i + 1]);
                break;
            case '--delay':
                delay = parseInt(args[i + 1]);
                break;
            case '--help':
                console.log('Help message shown above');
                process.exit(0);
                break;
            default:
                console.warn(`Unknown option: ${args[i]}`);
        }
    }

    // Validate CSV file exists
    const fs = require('fs');
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

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
    console.error('💥 Uncaught Exception:', error);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('💥 Unhandled Rejection at:', promise, 'reason:', reason);
    process.exit(1);
});

// Run the main function
main();