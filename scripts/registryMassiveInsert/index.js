const path = require('path');
const { readCsv, writeReport } = require('./utils/csvUtils');
const { findLocationId } = require('./utils/registryUtils');
const { getRegistriesByPartnerId, deleteRegistry, createRegistry } = require('./services/registryService');
const { Authenticator } = require('./libs/authenticator');
const { HEADERS } = require('./config');
const RegistryService = require('./services/registryService');

(async () => {
  const allowedEnvs = ['dev', 'test', 'uat', 'hotfix', 'prod'];
  const [,, env, username, password, clientId, csvFilePath] = process.argv;

  if (!env || !username || !password || !clientId || !csvFilePath) {
    console.error('Uso: node index.js <env> <username> <password> <clientId> <csvFilePath>');
    process.exit(1);
  }

  if (!allowedEnvs.includes(env)) {
    console.error('Parametro <env> non valido. Valori ammessi: dev, test, uat, hotfix, prod');
    process.exit(1);
  }

  const apiBaseUrl = env === 'prod' ? 'https://api.radd.notifichedigitali.it' : `https://api.radd.${env}.notifichedigitali.it`;
  const registryService = new RegistryService(apiBaseUrl);

  const partnerId = path.basename(csvFilePath).replace('.csv', '');
  const authenticator = new Authenticator(username, password, clientId);
  const jwt = await authenticator.generateJwtToken();
  const headers = { ...HEADERS, Authorization: `Bearer ${jwt}` };

  const csvRegistries = await readCsv(csvFilePath);
  const apiRegistries = await registryService.getRegistriesByPartnerId(partnerId, headers);

  const usedLocationIds = new Set();
  const report = [];

  for (const csvRegistry of csvRegistries) {
    const locationId = findLocationId(apiRegistries, csvRegistry);
    if (locationId) {
      await registryService.deleteRegistry(partnerId, locationId, headers);
      usedLocationIds.add(locationId);
    }
    const result = await registryService.createRegistry(partnerId, csvRegistry, headers);
    report.push(result);
  }

  for (const registry of apiRegistries) {
    if (!usedLocationIds.has(registry.locationId)) {
      await deleteRegistry(partnerId, registry.locationId, headers);
    }
  }

  await writeReport(report, partnerId);
})();
