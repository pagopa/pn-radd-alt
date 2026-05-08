const fs = require('fs');
const csv = require('csv-parser');
const axios = require('axios');
const { CognitoAuth } = require('./cognito-auth');
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

    // Usa il modulo condiviso per l'autenticazione (supporta local + SSO)
    this.auth = new CognitoAuth({ useIdToken: this.useIdToken });
  }

  async getAuthToken() {
    return this.auth.getToken();
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
      console.log(`[dry-run] PATCH /radd-bo/api/v2/registry/${record.locationId} (x-pagopa-pn-cx-id: ${cxIdToUse}) body:`, bodyPreview);
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
      
      console.log(`PATCH ok ${record.locationId} (x-pagopa-pn-cx-id: ${record.cxId || this.cxIdAuthFleet}) ->`, body.coordinates);
      this.successCount++;
      return resp.data;
    } catch (error) {
      console.error(`PATCH fallita ${record.locationId}:`, error.response?.data || error.message);
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
            console.warn(`Riga ignorata (${e.message})`, row);
          }
        })
        .on('end', async () => {
          console.log(`Record validi da processare: ${records.length}`);
          await this.processBatches(records, batchSize, delayMs);
          console.log(`\nCompletato: OK ${this.successCount} | KO ${this.errorCount} | Totale ${this.successCount + this.errorCount}`);
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
      console.log(`Batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(records.length / batchSize)} (${batch.length})`);
      await Promise.all(batch.map(r => this.patchRegistry(r)));
      if (i + batchSize < records.length) {
        console.log(`Attesa ${delayMs}ms...`);
        await new Promise(r => setTimeout(r, delayMs));
      }
    }
  }
}

module.exports = GeolocPatchProcessor;
