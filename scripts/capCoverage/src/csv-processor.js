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

        this.cognitoClient = new CognitoIdentityProviderClient({
            region: process.env.COGNITO_REGION
        });
        this.cognitoToken = null;
        this.cognitoTokenExpiry = 0; // epoch seconds
        this._tokenPromise = null; // lock per richieste concorrenti
        this._tokenMarginSeconds = parseInt(process.env.COGNITO_TOKEN_MARGIN || '30', 10); // margine prima della scadenza
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

            // Debug custom attribute se presente solo nell'ID token
            const customAttrName = 'custom:backoffice_tags';
            if (useIdToken) {
                if (payload[customAttrName]) {
                    console.log(`🧩 Claim '${customAttrName}' presente:`, payload[customAttrName]);
                } else {
                    console.warn(`⚠️ Claim '${customAttrName}' non presente nell'ID token. Verifica che l'app client abbia l'attributo in lettura e che l'utente lo abbia valorizzato.`);
                }
            } else {
                // Access token tipicamente NON contiene attributi custom
                if (payload[customAttrName]) {
                    console.log(`🧩 (Raro) Claim '${customAttrName}' presente nell'access token:`, payload[customAttrName]);
                } else {
                    console.log(`ℹ️ Usando access token: gli attributi custom come '${customAttrName}' non sono inclusi. Imposta COGNITO_USE_ID_TOKEN=true se l'authorizer richiede quel claim.`);
                }
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
            const token = await this.getAuthToken();
            // Rimuovo log completo del token (sensibile); opzionale debug lunghezza
            console.log(`🔑 Usando token Cognito (len=${token.length}) per ${payload.locality}/${payload.cap}`);
            const response = await axios.post(`${this.apiBaseUrl}/radd-bo/api/v1/coverages`, payload, {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                timeout: 10000
            });
            console.log(`✅ Successfully created coverage for ${payload.locality} (${payload.cap})`);
            this.successCount++;
            return response.data;
        } catch (error) {
            console.error(`❌ Failed to create coverage for ${payload.locality} (${payload.cap}):`, error.response?.data || error.message);
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
                    return { error: error.message, payload }; // non interrompe batch
                })
            );
            await Promise.all(promises);
            if (i + batchSize < records.length) {
                console.log(`⏳ Waiting ${delayMs}ms before next batch...`);
                await new Promise(resolve => setTimeout(resolve, delayMs));
            }
        }
    }
}

module.exports = CSVCoverageProcessor;