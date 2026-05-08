const path = require('path');
const { readCsv, writeReport } = require('./utils/csvUtils');
const { findLocationId, isFieldEmpty } = require('./utils/registryUtils');
const { getRegistriesByPartnerId, deleteRegistry, createRegistry } = require('./services/registryService');
const { Authenticator } = require('./libs/authenticator');
const { HEADERS } = require('./config');
const RegistryService = require('./services/registryService');

(async () => {
  const allowedEnvs = ['dev', 'test', 'uat', 'hotfix', 'prod'];
  const args = process.argv.slice(2);

  // Supporto modalità token:  node index.js --token <token> <env> <clientId> <csvFilePath>
  // Supporto modalità SSO:    node index.js --sso <env> <clientId> <csvFilePath>
  // Supporto modalità locale: node index.js <env> <username> <password> <clientId> <csvFilePath>
  const ssoMode = args.includes('--sso');
  const tokenIndex = args.indexOf('--token');
  const profileIndex = args.indexOf('--profile');
  const poolIndex = args.indexOf('--user-pool-id');
  
  const directToken = tokenIndex !== -1 ? args[tokenIndex + 1] : null;
  const awsProfile = profileIndex !== -1 ? args[profileIndex + 1] : (process.env.AWS_PROFILE || null);
  const userPoolId = poolIndex !== -1 ? args[poolIndex + 1] : (process.env.COGNITO_USER_POOL_ID || null);

  let env, username, password, clientId, csvFilePath;

  if (ssoMode || directToken) {
    const filtered = args.filter(a => 
      a !== '--sso' && 
      a !== '--token' && a !== directToken &&
      a !== '--profile' && a !== awsProfile && 
      a !== '--user-pool-id' && a !== userPoolId
    );
    [env, clientId, ...csvPathParts] = filtered;
    csvFilePath = csvPathParts.join(' ');
    username = null;
    password = null;

    if (!env || !clientId || !csvFilePath) {
      console.error('Uso token:  node index.js --token <token> <env> <clientId> <csvFilePath>');
      console.error('Uso SSO:    node index.js --sso <env> <clientId> <csvFilePath>');
      process.exit(1);
    }
  } else {
    [env, username, password, clientId, ...csvPathParts] = args;
    // accetto gli spazi nel nome del file
    csvFilePath = csvPathParts.join(' ');
    if (!env || !username || !password || !clientId || !csvFilePath) {
      console.error('Uso locale: node index.js <env> <username> <password> <clientId> <csvFilePath>');
      console.error('Uso SSO:    node index.js --sso <env> <clientId> <csvFilePath>');
      process.exit(1);
    }
  }

  if (!allowedEnvs.includes(env)) {
    console.error('Parametro <env> non valido. Valori ammessi: dev, test, uat, hotfix, prod');
    process.exit(1);
  }

  const apiBaseUrl = env === 'prod' ? 'https://api.radd.notifichedigitali.it' : `https://api.radd.${env}.notifichedigitali.it`;
  const registryService = new RegistryService(apiBaseUrl);
  const partnerId = path.basename(csvFilePath).replace('.csv', '').split("-")[0];

  if (partnerId.length !== 11){
    console.error('formato nome file errato. Esempio: <cf>-<des>.csv.');
    process.exit(1);
  }

  let jwt;
  if (directToken) {
    console.log('[Info] Uso token passato direttamente.');
    jwt = directToken;
  } else {
    const authenticator = new Authenticator(username, password, clientId, { 
      env, 
      awsProfile,
      userPoolId 
    });
    jwt = await authenticator.generateJwtToken();
  }
  const headers = { ...HEADERS, Authorization: `Bearer ${jwt}` };

  const csvRegistries = await readCsv(csvFilePath);
  const apiRegistries = await registryService.getRegistriesByPartnerId(partnerId, headers);

  const usedLocationIds = new Set();
  const report = [];

    if (!csvRegistries || csvRegistries.length === 0)
    {
      console.error('Il file CSV è vuoto o malformato. Deve contenere almeno una riga valida per aggiungere o aggiornare una sede.');
      process.exit(1);
    }

    for (const csvRegistry of csvRegistries) {
      const { locationId } = csvRegistry;

      if (locationId && locationId.trim() !== '') {
        // Aggiorna sede
        const result = await registryService.updateRegistry(partnerId, locationId, csvRegistry, headers);
        usedLocationIds.add(locationId);
        report.push(result);
      } else {
        // Crea nuova sede
        const result = await registryService.createRegistry(partnerId, csvRegistry, headers);
        if (result?.locationId) {
          usedLocationIds.add(result.locationId);
        }
        report.push(result);
      }
    }

    // Elimina sedi non presenti nel CSV
    for (const registry of apiRegistries) {
      if (!usedLocationIds.has(registry.locationId)) {
        await registryService.deleteRegistry(partnerId, registry.locationId, headers);
      }
    }

  await writeReport(report, partnerId);
})();
