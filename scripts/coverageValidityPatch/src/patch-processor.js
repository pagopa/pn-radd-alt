const fs = require('fs');
const csv = require('csv-parser');
const axios = require('axios');
const { CognitoIdentityProviderClient, InitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');
require('dotenv').config();

class CoverageValidityPatchProcessor {
  constructor(apiBaseUrl, options = {}) {
    this.apiBaseUrl = apiBaseUrl;
    this.successCount = 0;
    this.errorCount = 0;
    this.dryRun = !!options.dryRun;
    this.useIdToken = !!options.useIdToken;

    this.cognitoClient = new CognitoIdentityProviderClient({ region: process.env.COGNITO_REGION });
    this.cognitoToken = null;
    this.cognitoTokenExpiry = 0;
    this._tokenPromise = null;
    this._tokenMarginSeconds = parseInt(process.env.COGNITO_TOKEN_MARGIN || '30', 10);
  }

  decodeJwt(token) {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const b64 = (s) => Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
    try { return JSON.parse(b64(parts[1])); } catch { return null; }
  }

  async getAuthToken() {
    const now = Math.floor(Date.now() / 1000);
    if (this.cognitoToken && now < (this.cognitoTokenExpiry - this._tokenMarginSeconds)) {
      return this.cognitoToken;
    }
    if (this._tokenPromise) return this._tokenPromise;

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

  validateDate(value, fieldName) {
    if (!value) return null; // opzionale
    // formato ISO date (YYYY-MM-DD) richiesto da format: date
    const regex = /^\d{4}-\d{2}-\d{2}$/;
    if (!regex.test(value)) throw new Error(`Data '${value}' non valida per ${fieldName}. Atteso YYYY-MM-DD`);
    const d = new Date(value + 'T00:00:00Z');
    if (isNaN(d.getTime())) throw new Error(`Data '${value}' non interpretabile per ${fieldName}`);
    return value;
  }

  mapRow(row) {
    const cap = row.cap || row.CAP;
    const locality = row.locality || row.LOCALITY || row.comune || row.COMUNE;
    const startValidity = row.startValidity || row.start_validity || row.start || row.STARTVALIDITY;
    const endValidity = row.endValidity || row.end_validity || row.end || row.ENDVALIDITY;
    const provinceRaw = row.province || row.Provincia || row.PROVINCE || row.pr || row.PR;
    const cadastralRaw = row.cadastralCode || row['Cod catastale'] || row.codCatastale || row.CODCATASTALE;

    if (!cap || !/^\d{5}$/.test(cap)) throw new Error(`CAP mancante o non valido: '${cap}'`);
    if (!locality) throw new Error('Locality mancante');

    const sv = this.validateDate(startValidity, 'startValidity');
    const ev = this.validateDate(endValidity, 'endValidity');
    if (sv && ev && sv > ev) throw new Error(`startValidity (${sv}) successiva a endValidity (${ev})`);

    let province = null;
    if (provinceRaw) {
      if (!/^([A-Za-z]{2})$/.test(provinceRaw)) throw new Error(`Provincia non valida: '${provinceRaw}' (atteso 2 lettere)`);
      province = provinceRaw.toUpperCase();
    }
    let cadastralCode = null;
    if (cadastralRaw) {
      if (!/^([A-Za-z0-9]{4})$/.test(cadastralRaw)) throw new Error(`Codice catastale non valido: '${cadastralRaw}' (atteso 4 alfanumerici)`);
      cadastralCode = cadastralRaw.toUpperCase();
    }

    return { cap, locality, startValidity: sv, endValidity: ev, province, cadastralCode };
  }

  encodePathParam(value) {
    if (value == null) return '';
    const trimmed = value.toString().trim();
    // Normalizza caratteri Unicode (accenti) in NFC per coerenza
    const normalized = trimmed.normalize('NFC');
    return encodeURIComponent(normalized);
  }

  async patchCoverage(record) {
    if (this.dryRun) {
      const bodyPreview = {};
      if (record.startValidity) bodyPreview.startValidity = record.startValidity;
      if (record.endValidity) bodyPreview.endValidity = record.endValidity;
      if (record.province) bodyPreview.province = record.province;
      if (record.cadastralCode) bodyPreview.cadastralCode = record.cadastralCode;
      const encodedLocality = this.encodePathParam(record.locality);
      const pathShown = encodedLocality !== record.locality ? `${record.cap}/${encodedLocality} (raw: ${record.locality})` : `${record.cap}/${encodedLocality}`;
      console.log(`📝 [dry-run] PATCH /coverages/${pathShown} body:`, bodyPreview);
      this.successCount++; // considerata ok
      return;
    }
    try {
      const token = await this.getAuthToken();
      const encodedLocality = this.encodePathParam(record.locality);
      const url = `${this.apiBaseUrl}/radd-bo/api/v1/coverages/${record.cap}/${encodedLocality}`;
      const body = {};
      if (record.startValidity) body.startValidity = record.startValidity;
      if (record.endValidity) body.endValidity = record.endValidity;
      if (record.province) body.province = record.province;
      if (record.cadastralCode) body.cadastralCode = record.cadastralCode;
      if (Object.keys(body).length === 0) {
        console.log(`⚠️ Nessun campo da aggiornare per ${record.cap}/${record.locality}, salto.`);
        this.successCount++;
        return;
      }
      const resp = await axios.patch(url, body, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        timeout: 10000
      });
      if (encodedLocality !== record.locality) {
        console.log(`✅ PATCH ok ${record.cap}/${record.locality} (encoded: ${encodedLocality}) ->`, body);
      } else {
        console.log(`✅ PATCH ok ${record.cap}/${record.locality} ->`, body);
      }
      this.successCount++;
      return resp.data;
    } catch (error) {
      console.error(`❌ PATCH fallita ${record.cap}/${record.locality}:`, error.response?.data || error.message);
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
          console.log(`\n📈 Completato: ✅ ${this.successCount} ❌ ${this.errorCount} 📊 ${this.successCount + this.errorCount}`);
          resolve({ success: this.successCount, errors: this.errorCount, total: this.successCount + this.errorCount });
        })
        .on('error', (err) => {
          console.error('Errore lettura CSV:', err);
          reject(err);
        });
    });
  }

  async processBatches(records, batchSize, delayMs) {
    for (let i = 0; i < records.length; i += batchSize) {
      const batch = records.slice(i, i + batchSize);
      console.log(`🔄 Batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(records.length / batchSize)} (${batch.length})`);
      await Promise.all(batch.map(r => this.patchCoverage(r)));
      if (i + batchSize < records.length) {
        console.log(`⏳ Attesa ${delayMs}ms...`);
        await new Promise(r => setTimeout(r, delayMs));
      }
    }
  }
}

module.exports = CoverageValidityPatchProcessor;
