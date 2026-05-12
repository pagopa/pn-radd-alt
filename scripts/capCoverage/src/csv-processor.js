const fs = require('fs');
const csv = require('csv-parser');
const axios = require('axios');
const { CognitoAuth } = require('../../shared/cognito-auth');
require('dotenv').config();

class CSVCoverageProcessor {
    constructor(apiBaseUrl = 'https://api.example.com') {
        this.apiBaseUrl = apiBaseUrl;
        this.successCount = 0;
        this.errorCount = 0;

        // Usa il modulo condiviso per l'autenticazione (supporta local + SSO)
        this.auth = new CognitoAuth();
    }

    async getAuthToken() {
        return this.auth.getToken();
    }

    /**
     * Normalizza i campi supportando entrambe le varianti di CSV:
     * Variante "storica": COMUNE, Provincia, CAP, Cod catastale
     * Variante "nuova": CAP, LOCALITY, STARTVALIDITY, PUNTIPRESENTI
     * Province e cadastralCode diventano opzionali.
     */
    mapRowToApiPayload(row) {
        // Possibili alias
        const cap = row.CAP || row.cap;
        const locality = row.LOCALITY || row.COMUNE || row.locality || row.comune;
        const cadastralCode = row['Cod catastale'] || row.CODCATASTALE || row.cadastralCode;
        const province = row.Provincia || row.PROVINCE || row.province;
        const startValidity = row.STARTVALIDITY || row.startValidity || row.start_validity; // non usato nel body POST

        return {
            cap,
            locality,
            // Questi campi sono opzionali secondo CreateCoverageRequest
            ...(cadastralCode ? { cadastralCode } : {}),
            ...(province ? { province } : {}),
            // Campo aggiuntivo presente nel CSV nuovo: log informativo (non inviato)
            _startValidityCsv: startValidity || undefined
        };
    }

    async createCoverage(payload) {
        try {
            const token = await this.getAuthToken();
            // Rimuovo log completo del token (sensibile)
            console.log(`Richiesta autenticata per ${payload.locality}/${payload.cap}`);
            // Body conforme allo schema CreateCoverageRequest
            const body = {
                cap: payload.cap,
                locality: payload.locality,
                ...(payload.cadastralCode ? { cadastralCode: payload.cadastralCode } : {}),
                ...(payload.province ? { province: payload.province } : {})
            };
            if (payload._startValidityCsv) {
                console.log(`CSV contiene startValidity='${payload._startValidityCsv}' (ignorato nel POST, disponibile solo in PATCH).`);
            }
            const response = await axios.post(`${this.apiBaseUrl}/radd-bo/api/v1/coverages`, body, {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                timeout: 10000
            });
            console.log(`Creato coverage ${payload.locality} (${payload.cap})`);
            this.successCount++;
            return response.data;
        } catch (error) {
            console.error(`Errore creazione coverage ${payload.locality} (${payload.cap}):`, error.response?.data || error.message);
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

            console.log(`Lettura CSV: ${csvFilePath}`);

            fs.createReadStream(csvFilePath)
                .pipe(csv())
                .on('data', (row) => {
                    // Validazione minima: servono cap & locality
                    const cap = row.CAP || row.cap;
                    const locality = row.LOCALITY || row.COMUNE || row.locality || row.comune;
                    if (!cap || !/^\d{5}$/.test(cap)) {
                        console.warn(`Riga ignorata: CAP mancante/non valido`, row);
                        return;
                    }
                    if (!locality) {
                        console.warn(`Riga ignorata: LOCALITY/COMUNE mancante`, row);
                        return;
                    }
                    try {
                        const payload = this.mapRowToApiPayload(row);
                        results.push(payload);
                    } catch (e) {
                        console.warn(`Mappatura fallita: ${e.message}`, row);
                    }
                })
                .on('end', async () => {
                    console.log(`Record validi da processare: ${results.length}`);
                    await this.processBatches(results, batchSize, delayMs);
                    console.log(`\nCompletato:`);
                    console.log(`Successful: ${this.successCount}`);
                    console.log(`Failed: ${this.errorCount}`);
                    console.log(`Total: ${this.successCount + this.errorCount}`);
                    resolve({
                        success: this.successCount,
                        errors: this.errorCount,
                        total: this.successCount + this.errorCount
                    });
                })
                .on('error', (error) => {
                    console.error('Errore lettura CSV:', error);
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
            console.log(`Batch ${Math.floor(i/batchSize) + 1}/${Math.ceil(records.length/batchSize)} (${batch.length} record)`);
            const promises = batch.map(payload =>
                this.createCoverage(payload).catch(error => {
                    return { error: error.message, payload }; // non interrompe batch
                })
            );
            await Promise.all(promises);
            if (i + batchSize < records.length) {
                console.log(`Attesa ${delayMs}ms prima del prossimo batch...`);
                await new Promise(resolve => setTimeout(resolve, delayMs));
            }
        }
    }
}

module.exports = CSVCoverageProcessor;