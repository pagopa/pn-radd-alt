const axios = require('axios');
const { convertToRegistryRequest, mapFieldsToUpdate } = require('../utils/registryUtils');

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

function isSocketHangUpError(error) {
  if (!error) return false;

  const values = [
    error.message,
    error.code,
    error.errno,
    error?.cause?.message,
    error?.cause?.code,
    error?.response?.data?.message,
    error?.cause?.response?.data?.message
  ]
    .filter(Boolean)
    .map(v => String(v).toLowerCase());

  return values.some(v => v.includes('socket hang up')) ||
         values.some(v => v.includes('econnreset'));
}

async function withSocketHangUpRetry(fn, maxRetries = 5, delayMs = 1000) {
  let lastError;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;

      if (!isSocketHangUpError(error)) {
        throw error;
      }

      if (attempt === maxRetries) {
        const finalError = new Error(
          `Socket Hang Up after ${maxRetries} retries`,
          { cause: error }
        );
        finalError.code = error?.code;
        finalError.response = error?.response;
        throw finalError;
      }

      console.warn(`Socket hang up rilevato. Retry ${attempt}/${maxRetries - 1} tra ${delayMs} ms...`);
      await sleep(delayMs);
    }
  }

  throw lastError;
}

class RegistryService {
  constructor(apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  // Private helper to add the partnerId header
  #prepareHeaders(partnerId, headers = {}) {
    return {
      ...headers,
      'x-pagopa-pn-cx-id': partnerId,
    };
  }

  // Private helper to extract error message
  #getErrorMessage(error) {
    if (error?.response?.data) return JSON.stringify(error.response.data);
    if (error?.cause?.response?.data) return JSON.stringify(error.cause.response.data);
    return error?.message || error?.cause?.message || String(error);
  }

  async getRegistriesByPartnerId(partnerId, headers = {}) {
    try {
      const finalHeaders = this.#prepareHeaders(partnerId, headers);
      let allItems = [];
      let lastKey = null;

      do {
        const params = {};
        if (lastKey) {
          params.lastKey = lastKey;
        }

        const res = await withSocketHangUpRetry(() =>
          axios.get(`${this.apiBaseUrl}/radd-bo/api/v2/registry`, {
            headers: finalHeaders,
            params: params
          })
        );

        const responseData = res.data || {};
        const items = responseData.items || [];
        allItems = allItems.concat(items);
        lastKey = responseData.lastKey;

      } while (lastKey);

      console.log(`📋 Trovate ${allItems.length} sedi.`);
      return allItems;
    } catch (err) {
      throw new Error(`Errore lettura sedi da API: ${this.#getErrorMessage(err)}`);
    }
  }

  async deleteRegistry(partnerId, locationId, headers = {}) {
    try {
      const finalHeaders = this.#prepareHeaders(partnerId, headers);

      await withSocketHangUpRetry(() =>
        axios.delete(`${this.apiBaseUrl}/radd-bo/api/v2/registry/${locationId}`, {
          headers: finalHeaders
        })
      );

      console.log(`✅ Eliminata sede ${locationId}`);
    } catch (err) {
      console.error(`❌ Errore eliminazione sede ${locationId}: ${this.#getErrorMessage(err)}`);
    }
  }

  async createRegistry(partnerId, csvRegistry, headers = {}) {
    try {
      const finalHeaders = this.#prepareHeaders(partnerId, headers);
      const registry = convertToRegistryRequest(csvRegistry, partnerId);

      const res = await withSocketHangUpRetry(() =>
        axios.post(`${this.apiBaseUrl}/radd-bo/api/v2/registry`, registry, {
          headers: finalHeaders
        })
      );

      console.log(`➕ Aggiunta sede ${res.data.locationId}`);
      const locationId = res.data.locationId;

      return {
        ...csvRegistry,
        locationId: locationId,
        status: 'OK',
        result: JSON.stringify(res.data)
      };
    } catch (err) {
      const reason = this.#getErrorMessage(err);
      console.error(`⚠️ Inserimento KO: ${reason}`);
      return { ...csvRegistry, status: 'KO', error: reason };
    }
  }

  async updateRegistry(partnerId, locationId, csvRegistry, headers = {}) {
    try {
      const finalHeaders = this.#prepareHeaders(partnerId, headers);
      const updateRequest = mapFieldsToUpdate(csvRegistry);

      const res = await withSocketHangUpRetry(() =>
        axios.put(`${this.apiBaseUrl}/radd-bo/api/v2/registry/${locationId}`, updateRequest, {
          headers: finalHeaders
        })
      );

      console.log(`📝 Aggiornata sede ${res.data.locationId}`);
      return {
        ...csvRegistry,
        status: 'OK',
        result: JSON.stringify(res.data)
      };
    } catch (err) {
      const reason = this.#getErrorMessage(err);
      console.error(`⚠️ Aggiornamento KO: ${locationId} - ${reason}`);
      return { ...csvRegistry, status: 'KO', error: reason };
    }
  }
}

module.exports = RegistryService;