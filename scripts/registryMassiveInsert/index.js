const path = require('path');
const { readCsv, writeReport } = require('./utils/csvUtils');
const { findLocationId } = require('./utils/registryUtils');
const { getRegistriesByPartnerId, deleteRegistry, createRegistry } = require('./services/registryService');
const { Authenticator } = require('./libs/authenticator');
const { API_BASE_URL, HEADERS } = require('./config');

(async () => {
  const [,, username, password, clientId, csvFilePath] = process.argv;

  if (!username || !password || !clientId || !csvFilePath) {
    console.error('Uso: node index.js <username> <password> <clientId> <csvFilePath>');
    process.exit(1);
  }

  const partnerId = path.basename(csvFilePath).replace('.csv', '');
  const authenticator = new Authenticator(username, password, clientId);
  const jwt = await authenticator.generateJwtToken();
  const headers = { ...HEADERS, Authorization: `Bearer ${jwt}` };

  const csvRegistries = await readCsv(csvFilePath);
  const apiRegistries = await getRegistriesByPartnerId(partnerId, headers);

  const usedLocationIds = new Set();
  const report = [];

  for (const csvRegistry of csvRegistries) {
    const locationId = findLocationId(apiRegistries, csvRegistry);
    if (locationId) {
      await deleteRegistry(partnerId, locationId, headers);
      usedLocationIds.add(locationId);
    }
    const result = await createRegistry(partnerId, csvRegistry, headers);
    report.push(result);
  }

  for (const registry of apiRegistries) {
    if (!usedLocationIds.has(registry.locationId)) {
      await deleteRegistry(partnerId, registry.locationId, headers);
    }
  }

  await writeReport(report, partnerId);
})();
