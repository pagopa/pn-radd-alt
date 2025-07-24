const axios = require('axios');
const { convertToRegistryRequest } = require('../utils/registryUtils');

class RegistryService {
  constructor(apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  async getRegistriesByPartnerId(partnerId, headers) {
    try {
      const res = await axios.get(`${this.apiBaseUrl}/radd-bo/api/v2/registry/${partnerId}`, { headers });
      return res.data.items || [];
    } catch (err) {
      throw new Error(`Errore lettura sedi da API: ${err.response?.data?.detail || err.message}`);
    }
  }

  async deleteRegistry(partnerId, locationId, headers) {
    try {
      await axios.delete(`${this.apiBaseUrl}/radd-bo/api/v2/registry/${partnerId}/${locationId}`, { headers });
      console.log(`✅ Eliminata sede ${locationId}`);
    } catch (err) {
      console.error(`❌ Errore eliminazione sede ${locationId}: ${err.response?.data?.detail || err.message}`);
    }
  }

  async createRegistry(partnerId, csvRegistry, headers) {
    const registry = convertToRegistryRequest(csvRegistry);
    try {
      const res = await axios.post(`${this.apiBaseUrl}/radd-bo/api/v2/registry/${partnerId}`, registry, { headers });
      console.log(`➕ Aggiunta sede ${registry.locationId}`);
      return { ...csvRegistry, status: 'OK', result: JSON.stringify(res.data) };
    } catch (err) {
      const reason = err.response?.data?.detail || err.message;
      console.error(`⚠️ Inserimento KO: ${registry.locationId} - ${reason}`);
      return { ...csvRegistry, status: 'KO', error: reason };
    }
  }
}

module.exports = RegistryService;