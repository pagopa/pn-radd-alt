const fs = require('fs');
const csv = require('csv-parser');
const axios = require('axios');
require('dotenv').config();

class CSVCoverageProcessor {
    constructor(apiBaseUrl = 'https://api.example.com') {
        this.apiBaseUrl = apiBaseUrl;
        this.successCount = 0;
        this.errorCount = 0;
    }

    /**
     * Map CSV row to API payload format
     * @param {Object} row - CSV row with COMUNE, Provincia, CAP, Cod catastale
     * @returns {Object} API payload
     */
    mapRowToApiPayload(row) {
        return {
            cap: row.CAP,
            locality: row.COMUNE,
            cadastralCode: row['Cod catastale'],
            province: row.Provincia
        };
    }

    /**
     * Make POST request to coverage API
     * @param {Object} payload - API payload
     */
    async createCoverage(payload) {
        try {
            const response = await axios.post(`${this.apiBaseUrl}/radd-bo/api/v1/coverages`, payload, {
                headers: {
                    'Content-Type': 'application/json',
                    // Add authentication headers if needed
                    // 'Authorization': `Bearer ${process.env.API_TOKEN}`
                },
                timeout: 10000 // 10 second timeout
            });

            console.log(`✅ Successfully created coverage for ${payload.locality} (${payload.cap})`);
            this.successCount++;
            return response.data;
        } catch (error) {
            console.error(`❌ Failed to create coverage for ${payload.locality} (${payload.cap}):`, 
                error.response?.data || error.message);
            this.errorCount++;
            throw error;
        }
    }

    /**
     * Process CSV file and create coverages
     * @param {string} csvFilePath - Path to CSV file
     * @param {number} batchSize - Number of concurrent requests
     * @param {number} delayMs - Delay between batches in milliseconds
     */
    async processCsvFile(csvFilePath, batchSize = 5, delayMs = 1000) {
        return new Promise((resolve, reject) => {
            const results = [];
            const errors = [];

            console.log(`📖 Reading CSV file: ${csvFilePath}`);

            fs.createReadStream(csvFilePath)
                .pipe(csv())
                .on('data', (row) => {
                    // Validate required fields
                    if (!row.CAP || !row.COMUNE || !row['Cod catastale'] || !row.Provincia) {
                        console.warn(`⚠️  Skipping invalid row:`, row);
                        return;
                    }

                    const payload = this.mapRowToApiPayload(row);
                    results.push(payload);
                })
                .on('end', async () => {
                    console.log(`📊 Found ${results.length} valid records to process`);
                    
                    await this.processBatches(results, batchSize, delayMs);
                    
                    console.log(`\n📈 Processing complete:`);
                    console.log(`   ✅ Successful: ${this.successCount}`);
                    console.log(`   ❌ Failed: ${this.errorCount}`);
                    console.log(`   📊 Total: ${this.successCount + this.errorCount}`);
                    
                    resolve({
                        success: this.successCount,
                        errors: this.errorCount,
                        total: this.successCount + this.errorCount
                    });
                })
                .on('error', (error) => {
                    console.error('❌ Error reading CSV file:', error);
                    reject(error);
                });
        });
    }

    /**
     * Process records in batches to avoid overwhelming the API
     * @param {Array} records - Array of API payloads
     * @param {number} batchSize - Number of concurrent requests
     * @param {number} delayMs - Delay between batches
     */
    async processBatches(records, batchSize, delayMs) {
        for (let i = 0; i < records.length; i += batchSize) {
            const batch = records.slice(i, i + batchSize);
            
            console.log(`🔄 Processing batch ${Math.floor(i/batchSize) + 1}/${Math.ceil(records.length/batchSize)} (${batch.length} records)`);
            
            const promises = batch.map(payload => 
                this.createCoverage(payload).catch(error => {
                    // Continue processing even if individual requests fail
                    return { error: error.message, payload };
                })
            );

            await Promise.all(promises);

            // Add delay between batches to be respectful to the API
            if (i + batchSize < records.length) {
                console.log(`⏳ Waiting ${delayMs}ms before next batch...`);
                await new Promise(resolve => setTimeout(resolve, delayMs));
            }
        }
    }
}

module.exports = CSVCoverageProcessor;