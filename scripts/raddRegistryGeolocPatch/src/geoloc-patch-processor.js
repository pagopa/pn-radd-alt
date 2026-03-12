const fs = require('fs');
const csv = require('csv-parser');
const axios = require('axios');
const { CognitoIdentityProviderClient, InitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');
require('dotenv').config();

class GeolocPatchProcessor {
  constructor(apiBaseUrl, cxIdAuthFleet = null, options = {}) {
    this.apiBaseUrl = apiBaseUrl;
    // cxIdAuthFleet is an optional default CX ID (can be overridden per-row)
    this.cxIdAuthFleet = cxIdAuthFleet;
    this.successCount = 0;
    this.errorCount = 0;
    this.dryRun = !!options.dryRun;
    this.useIdToken = !!options.useIdToken;

    // Supporto token statico alternativo
    this.staticApiToken = process.env.API_TOKEN || null;
    this.requiredEnv = ['COGNITO_REGION', 'COGNITO_CLIENT_ID', 'COGNITO_USERNAME', 'COGNITO_PASSWORD'];

    this.cognitoClient = new CognitoIdentityProviderClient({ region: process.env.COGNITO_REGION });
    this.cognitoToken = null;
    this.cognitoTokenExpiry = 0;
    this._tokenPromise = null;
    this._tokenMarginSeconds = parseInt(process.env.COGNITO_TOKEN_MARGIN || '30', 10);

    this.validateEnvironment();
  }

  validateEnvironment() {
    if (this.staticApiToken) {
      console.log('ℹ️ Uso API_TOKEN (JWT statico), salto autenticazione Cognito.');
      return;
    }
    const missing = this.requiredEnv.filter(k => !process.env[k] || process.env[k].trim() === '');
    if (missing.length) {
      throw new Error(`Variabili Cognito mancanti: ${missing.join(', ')}. Esempio .env:\nCOGNITO_REGION=eu-central-1\nCOGNITO_CLIENT_ID=xxxxxxxx\nCOGNITO_USERNAME=utente@example.com\nCOGNITO_PASSWORD=Password123!`);
    }
  }

  decodeJwt(token) {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const b64 = (s) => Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
    try { return JSON.parse(b64(parts[1])); } catch { return null; }
  }

  async getAuthToken() {
    if (this.staticApiToken) {
      return this.staticApiToken;
    }
    const now = Math.floor(Date.now() / 1000);
    if (this.cognitoToken && now < (this.cognitoTokenExpiry - this._tokenMarginSeconds)) {
      return this.cognitoToken;
    }
    if (this._tokenPromise) return this._tokenPromise;

    this._tokenPromise = (async () => {
      const clientId = process.env.COGNITO_CLIENT_ID;
      if (!clientId) throw new Error('COGNITO_CLIENT_ID mancante: definisci la variabile o usa API_TOKEN');
      const params = {
        AuthFlow: 'USER_PASSWORD_AUTH',
        ClientId: clientId,
        AuthParameters: {
          USERNAME: process.env.COGNITO_USERNAME,
          PASSWORD: process.env.COGNITO_PASSWORD
        }
      };
      const command = new InitiateAuthCommand(params);
      const resp = await this.cognitoClient.send(command);
      const token = this.useIdToken ? resp.AuthenticationResult.IdToken : resp.AuthenticationResult.AccessToken;
      const payload = this.decodeJwt(token);
      if (!payload || !payload.exp) throw new Error('Token Cognito privo di exp');
      this.cognitoToken = token;
      this.cognitoTokenExpiry = payload.exp;
      console.log(`🔐 Token (${this.useIdToken ? 'ID' : 'Access'}) ottenuto. TTL ${(this.cognitoTokenExpiry - now)}s`);
      return token;
    })();
    try { return await this._tokenPromise; } finally { this._tokenPromise = null; }
  }

  validateCoordinate(value, fieldName, min, max) {
    if (value == null) return null; // opzionale
    
    const numValue = parseFloat(value);
    if (isNaN(numValue)) {
      throw new Error(`${fieldName} '${value}' non è un numero valido`);
    }
    
    if (numValue < min || numValue > max) {
      throw new Error(`${fieldName} ${numValue} fuori range [${min}, ${max}]`);
    }
    
    // Validazione massimo 6 decimali
    const decimalPlaces = (value.toString().split('.')[1] || '').length;
    if (decimalPlaces > 6) {
      throw new Error(`${fieldName} ha più di 6 decimali`);
    }
    
    return numValue;
  }

  mapRow(row) {
    // Robust detection of headers/columns. Prefer explicit headers for partnerId; if none and the CSV has at least 4 columns
    // we assume the first column is partnerId, second is locationId, third latitude, fourth longitude.
    const keys = Object.keys(row || {});
    const normalize = k => (k || '').toString().replace(/\s+/g, '').toLowerCase();
    const partnerCandidates = ['partnerid', 'partner_id', 'cx_id', 'cxid', 'partner', 'partner-id'];
    const locationCandidates = ['locationid', 'location_id', 'location', 'locid', 'id'];
    const latCandidates = ['latitude', 'lat'];
    const lngCandidates = ['longitude', 'lng', 'lon', 'long'];

    let partnerIdRaw = null;
    let locationId = null;
    let latitude = null;
    let longitude = null;

    // Try find explicit header keys
    for (const k of keys) {
      const nk = normalize(k);
      if (!partnerIdRaw && partnerCandidates.includes(nk)) partnerIdRaw = row[k];
      if (!locationId && locationCandidates.includes(nk)) locationId = row[k];
      if (!latitude && latCandidates.includes(nk)) latitude = row[k];
      if (!longitude && lngCandidates.includes(nk)) longitude = row[k];
    }

    // If partnerId missing but there are >=4 columns, assume first is partnerId
    if (!partnerIdRaw && keys.length >= 4) {
      const vals = Object.values(row);
      partnerIdRaw = vals[0];
      // fill any missing fields from positional values
      if (!locationId) locationId = vals[1];
      if (!latitude) latitude = vals[2];
      if (!longitude) longitude = vals[3];
    }

    // Fallback to configured default CX id
    const partnerId = partnerIdRaw || this.cxIdAuthFleet || null;

    if (!partnerId) throw new Error('partnerId (cxId) mancante: fornisci la prima colonna CSV o --cx-id / CX_ID_AUTH_FLEET');
    if (!locationId) throw new Error('locationId mancante');
    if (!latitude) throw new Error('latitude mancante');
    if (!longitude) throw new Error('longitude mancante');

    const lat = this.validateCoordinate(latitude, 'Latitude', -90, 90);
    const lng = this.validateCoordinate(longitude, 'Longitude', -180, 180);

    return { cxId: partnerId, locationId, latitude: lat, longitude: lng };
  }

  async patchRegistry(record) {
    if (this.dryRun) {
      const bodyPreview = {
        latitude: record.latitude,
        longitude: record.longitude
      };
      const cxIdToUse = record.cxId || this.cxIdAuthFleet;
      console.log(`📝 [dry-run] PATCH /radd-bo/api/v2/registry/${record.locationId} (x-pagopa-pn-cx-id: ${cxIdToUse}) body:`, bodyPreview);
      this.successCount++;
      return;
    }
    
    try {
      const token = await this.getAuthToken();
      const url = `${this.apiBaseUrl}/radd-bo/api/v2/registry/${record.locationId}`;
      const body = {
        coordinates: {
          latitude: record.latitude,
          longitude: record.longitude
        }
      };

      const resp = await axios.patch(url, body, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'x-pagopa-pn-cx-id': record.cxId || this.cxIdAuthFleet
        },
        timeout: 10000
      });
      
      console.log(`✅ PATCH ok ${record.locationId} (x-pagopa-pn-cx-id: ${record.cxId || this.cxIdAuthFleet}) ->`, body.coordinates);
      this.successCount++;
      return resp.data;
    } catch (error) {
      console.error(`❌ PATCH fallita ${record.locationId}:`, error.response?.data || error.message);
      this.errorCount++;
      return null;
    }
  }

  async processCsvFile(csvFilePath, batchSize = 5, delayMs = 1000) {
    return new Promise((resolve, reject) => {
      const records = [];
      fs.createReadStream(csvFilePath)
        .pipe(csv())
        .on('data', (row) => {
          try {
            const mapped = this.mapRow(row);
            records.push(mapped);
          } catch (e) {
            console.warn(`⚠️ Riga ignorata (${e.message})`, row);
          }
        })
        .on('end', async () => {
          console.log(`📊 Record validi da processare: ${records.length}`);
          await this.processBatches(records, batchSize, delayMs);
          console.log(`\n📈 Completato: ✅ ${this.successCount} ❌ ${this.errorCount} 📦 ${this.successCount + this.errorCount}`);
          resolve({ success: this.successCount, errors: this.errorCount, total: this.successCount + this.errorCount });
        })
        .on('error', (err) => {
          console.error('❌ Errore lettura CSV:', err);
          reject(err);
        });
    });
  }

  async processBatches(records, batchSize, delayMs) {
    for (let i = 0; i < records.length; i += batchSize) {
      const batch = records.slice(i, i + batchSize);
      console.log(`🔄 Batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(records.length / batchSize)} (${batch.length})`);
      await Promise.all(batch.map(r => this.patchRegistry(r)));
      if (i + batchSize < records.length) {
        console.log(`⏳ Attesa ${delayMs}ms...`);
        await new Promise(r => setTimeout(r, delayMs));
      }
    }
  }
}

module.exports = GeolocPatchProcessor;
