const fs = require('fs');
const csv = require('csv-parser');
const axios = require('axios');
const { CognitoIdentityProviderClient, InitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');
require('dotenv').config();

class CSVCoverageProcessor {
    constructor(apiBaseUrl = 'https://api.example.com') {
        this.apiBaseUrl = apiBaseUrl;
        this.successCount = 0;
        this.errorCount = 0;
        // Fallback token statico opzionale
        this.staticApiToken = process.env.API_TOKEN || null;
        // Variabili richieste per Cognito se non si usa API_TOKEN
        this.requiredCognitoEnv = ['COGNITO_REGION', 'COGNITO_CLIENT_ID', 'COGNITO_USERNAME', 'COGNITO_PASSWORD'];

        this.cognitoClient = new CognitoIdentityProviderClient({
            region: process.env.COGNITO_REGION
        });
        this.cognitoToken = null;
        this.cognitoTokenExpiry = 0; // epoch seconds
        this._tokenPromise = null; // lock per richieste concorrenti
        this._tokenMarginSeconds = parseInt(process.env.COGNITO_TOKEN_MARGIN || '30', 10); // margine prima della scadenza

        // Validazione iniziale (non blocca se presente API_TOKEN)
        this.validateEnvironment();
    }

    validateEnvironment() {
        if (this.staticApiToken) {
            console.log('ℹ️ Uso token statico da variabile API_TOKEN (Cognito non richiesto).');
            return;
        }
        const missing = this.requiredCognitoEnv.filter(k => !process.env[k] || process.env[k].trim() === '');
        if (missing.length) {
            throw new Error(`Variabili ambiente Cognito mancanti: ${missing.join(', ')}. Configura .env oppure esportale prima di eseguire. Esempio:\nCOGNITO_REGION=eu-central-1\nCOGNITO_CLIENT_ID=xxxxxxxx\nCOGNITO_USERNAME=utente@example.com\nCOGNITO_PASSWORD=Password123!`);
        }
    }

    decodeJwt(token) {
        const [header, payload] = token.split('.');
        if (!payload) return null;
        const b64 = (s) => Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
        try {
            return JSON.parse(b64(payload));
        } catch (e) {
            return null;
        }
    }

    async getAuthToken() {
        // Se presente token statico salta Cognito
        if (this.staticApiToken) {
            return this.staticApiToken;
        }
        const now = Math.floor(Date.now() / 1000);
        if (this.cognitoToken && now < (this.cognitoTokenExpiry - this._tokenMarginSeconds)) {
            return this.cognitoToken; // già valido
        }
        if (this._tokenPromise) {
            return this._tokenPromise; // riusa richiesta in corso
        }
        this._tokenPromise = (async () => {
            const params = {
                AuthFlow: 'USER_PASSWORD_AUTH',
                ClientId: process.env.COGNITO_CLIENT_ID,
                AuthParameters: {
                    USERNAME: process.env.COGNITO_USERNAME,
                    PASSWORD: process.env.COGNITO_PASSWORD
                }
            };
            if (!params.ClientId) {
                throw new Error('COGNITO_CLIENT_ID mancante: definisci la variabile ambiente o usa API_TOKEN.');
            }
            const command = new InitiateAuthCommand(params);
            const resp = await this.cognitoClient.send(command);
            const useIdToken = (process.env.COGNITO_USE_ID_TOKEN || 'false').toLowerCase() === 'true';
            const rawAccessToken = resp.AuthenticationResult.AccessToken;
            const rawIdToken = resp.AuthenticationResult.IdToken;
            const selectedToken = useIdToken ? rawIdToken : rawAccessToken;

            const payload = this.decodeJwt(selectedToken);
            if (!payload || !payload.exp) {
                throw new Error('Impossibile determinare scadenza token Cognito (payload mancante o senza exp)');
            }
            this.cognitoToken = selectedToken;
            this.cognitoTokenExpiry = payload.exp;
            console.log(`🔐 Token Cognito (${useIdToken ? 'ID' : 'Access'}) ottenuto. Scade tra ${(this.cognitoTokenExpiry - now)}s.`);

            const customAttrName = 'custom:backoffice_tags';
            if (useIdToken) {
                if (payload[customAttrName]) {
                    console.log(`🧪 Claim '${customAttrName}' presente:`, payload[customAttrName]);
                } else {
                    console.warn(`⚠️ Claim '${customAttrName}' non presente nell'ID token.`);
                }
            } else {
                console.log(`ℹ️ Usando access token: gli attributi custom come '${customAttrName}' non sono inclusi.`);
            }

            return selectedToken;
        })();

        try {
            return await this._tokenPromise;
        } finally {
            this._tokenPromise = null;
        }
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
            console.log(`🔑 Usando token Cognito (len=${token.length}) per ${payload.locality}/${payload.cap}`);
            // Body conforme allo schema CreateCoverageRequest
            const body = {
                cap: payload.cap,
                locality: payload.locality,
                ...(payload.cadastralCode ? { cadastralCode: payload.cadastralCode } : {}),
                ...(payload.province ? { province: payload.province } : {})
            };
            if (payload._startValidityCsv) {
                console.log(`🗓️ CSV contiene startValidity='${payload._startValidityCsv}' (ignorato nel POST, disponibile solo in PATCH).`);
            }
            const response = await axios.post(`${this.apiBaseUrl}/radd-bo/api/v1/coverages`, body, {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                timeout: 10000
            });
            console.log(`✅ Creato coverage ${payload.locality} (${payload.cap})`);
            this.successCount++;
            return response.data;
        } catch (error) {
            console.error(`❌ Errore creazione coverage ${payload.locality} (${payload.cap}):`, error.response?.data || error.message);
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

            console.log(`📖 Lettura CSV: ${csvFilePath}`);

            fs.createReadStream(csvFilePath)
                .pipe(csv())
                .on('data', (row) => {
                    // Validazione minima: servono cap & locality
                    const cap = row.CAP || row.cap;
                    const locality = row.LOCALITY || row.COMUNE || row.locality || row.comune;
                    if (!cap || !/^\d{5}$/.test(cap)) {
                        console.warn(`⚠️  Riga ignorata: CAP mancante/non valido`, row);
                        return;
                    }
                    if (!locality) {
                        console.warn(`⚠️  Riga ignorata: LOCALITY/COMUNE mancante`, row);
                        return;
                    }
                    try {
                        const payload = this.mapRowToApiPayload(row);
                        results.push(payload);
                    } catch (e) {
                        console.warn(`⚠️  Mappatura fallita: ${e.message}`, row);
                    }
                })
                .on('end', async () => {
                    console.log(`📊 Record validi da processare: ${results.length}`);
                    await this.processBatches(results, batchSize, delayMs);
                    console.log(`\n📈 Completato:`);
                    console.log(`   ✅ Successful: ${this.successCount}`);
                    console.log(`   ❌ Failed: ${this.errorCount}`);
                    console.log(`   📦 Total: ${this.successCount + this.errorCount}`);
                    resolve({
                        success: this.successCount,
                        errors: this.errorCount,
                        total: this.successCount + this.errorCount
                    });
                })
                .on('error', (error) => {
                    console.error('❌ Errore lettura CSV:', error);
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
            console.log(`🔄 Batch ${Math.floor(i/batchSize) + 1}/${Math.ceil(records.length/batchSize)} (${batch.length} record)`);
            const promises = batch.map(payload =>
                this.createCoverage(payload).catch(error => {
                    return { error: error.message, payload }; // non interrompe batch
                })
            );
            await Promise.all(promises);
            if (i + batchSize < records.length) {
                console.log(`⏳ Attesa ${delayMs}ms prima del prossimo batch...`);
                await new Promise(resolve => setTimeout(resolve, delayMs));
            }
        }
    }
}

module.exports = CSVCoverageProcessor;