const path = require('path');
const { readCsv, writeReport } = require('./utils/csvUtils');
const { findLocationId, isFieldEmpty } = require('./utils/registryUtils');
const { getRegistriesByPartnerId, deleteRegistry, createRegistry } = require('./services/registryService');
const { Authenticator } = require('./libs/authenticator');
const { fetchHelpdeskIdToken } = require('../shared/helpdesk-token');
const { HEADERS } = require('./config');
const RegistryService = require('./services/registryService');

(async () => {
  const allowedEnvs = ['dev', 'test', 'uat', 'hotfix', 'prod'];
  const args = process.argv.slice(2);
  const expectedHelpdeskHostByEnv = {
    dev: 'helpdesk.dev.notifichedigitali.it',
    test: 'helpdesk.test.notifichedigitali.it',
    uat: 'helpdesk.uat.notifichedigitali.it',
    hotfix: 'helpdesk.hotfix.notifichedigitali.it',
    prod: 'helpdesk.notifichedigitali.it',
  };

  const ssoMode = args.includes('--sso');
  if (args.includes('--auto-token')) {
    console.error('Il flag --auto-token non e piu supportato. Usa --sso.');
    process.exit(1);
  }
  if (args.includes('--profile')) {
    console.warn('[Compat] --profile e ignorato in modalita auto-token/token.');
  }
  if (args.includes('--user-pool-id')) {
    console.warn('[Compat] --user-pool-id e ignorato in modalita auto-token/token.');
  }

  // Supporto modalità token (utenti SSO/Google):
  //   node index.js --token <idToken> <env> [clientId] <csvFilePath>
  // Supporto modalità SSO automatica (utenti SSO/Google):
  //   node index.js --sso <env> [clientId] <csvFilePath>
  // Supporto modalità locale (utenti Cognito non federati):
  //   node index.js <env> <username> <password> <clientId> <csvFilePath>
  // In modalità SSO/token il clientId NON è usato per l'autenticazione (il token
  // arriva dall'Helpdesk): è quindi opzionale e accettato solo per
  // retrocompatibilità con i comandi esistenti.
  const tokenIndex = args.indexOf('--token');
  if (tokenIndex !== -1 && !args[tokenIndex + 1]) {
    console.error('Uso token:  node index.js --token <idToken> <env> <clientId> <csvFilePath>');
    process.exit(1);
  }
  // Il token può arrivare da --token <jwt> oppure, per non esporlo nella process
  // list (ps), dalla variabile d'ambiente namespaced PN_RADD_ID_TOKEN. L'argomento CLI ha priorità.
  const directToken = tokenIndex !== -1 ? args[tokenIndex + 1] : (process.env.PN_RADD_ID_TOKEN || null);
  const helpdeskUrlIndex = args.indexOf('--helpdesk-url');
  const helpdeskUrl = helpdeskUrlIndex !== -1 ? args[helpdeskUrlIndex + 1] : null;
  const browserIndex = args.indexOf('--browser');
  const browser = browserIndex !== -1 ? args[browserIndex + 1] : null;
  const profileIndex = args.indexOf('--profile');
  const userPoolIdIndex = args.indexOf('--user-pool-id');
  const autoToken = ssoMode;

  let env, username, password, clientId, csvFilePath, csvPathParts;

  if (directToken || autoToken) {
    const filtered = args.filter((a, i) => {
      if (tokenIndex !== -1 && (i === tokenIndex || i === tokenIndex + 1)) return false;
      if (helpdeskUrlIndex !== -1 && (i === helpdeskUrlIndex || i === helpdeskUrlIndex + 1)) return false;
      if (browserIndex !== -1 && (i === browserIndex || i === browserIndex + 1)) return false;
      if (profileIndex !== -1 && (i === profileIndex || i === profileIndex + 1)) return false;
      if (userPoolIdIndex !== -1 && (i === userPoolIdIndex || i === userPoolIdIndex + 1)) return false;
      if (a === '--sso') return false;
      return true;
    });
    [env, clientId, ...csvPathParts] = filtered;
    username = null;
    password = null;

    // clientId è opzionale in SSO/token. Il primo argomento dopo <env> è un
    // clientId solo se ne ha la forma (stringa alfanumerica minuscola di almeno
    // 20 caratteri, come i Client ID Cognito) ed è seguito da altri argomenti
    // (il path CSV). Altrimenti fa già parte del path del file.
    const rest = filtered.slice(1);
    const looksLikeClientId = (s) => typeof s === 'string' && /^[a-z0-9]{20,}$/.test(s);
    if (rest.length > 1 && looksLikeClientId(rest[0])) {
      clientId = rest[0];
      csvPathParts = rest.slice(1);
    } else {
      clientId = null;
      csvPathParts = rest;
    }
    csvFilePath = csvPathParts.join(' ');

    if (!env || !csvFilePath) {
      console.error('Uso token:  node index.js --token <idToken> <env> [clientId] <csvFilePath>');
      console.error('Uso SSO:    node index.js --sso <env> [clientId] <csvFilePath> [--helpdesk-url <url>] [--browser chrome|edge|chromium]');
      console.error('Uso locale: node index.js <env> <username> <password> <clientId> <csvFilePath>');
      process.exit(1);
    }
  } else {
    [env, username, password, clientId, ...csvPathParts] = args;
    // accetto gli spazi nel nome del file
    csvFilePath = csvPathParts.join(' ');
    if (!env || !username || !password || !clientId || !csvFilePath) {
      console.error('Uso locale: node index.js <env> <username> <password> <clientId> <csvFilePath>');
      console.error('Uso token:  node index.js --token <idToken> <env> [clientId] <csvFilePath>');
      console.error('Uso SSO:    node index.js --sso <env> [clientId] <csvFilePath> [--helpdesk-url <url>] [--browser chrome|edge|chromium]');
      process.exit(1);
    }
  }

  if (!allowedEnvs.includes(env)) {
    console.error('Parametro <env> non valido. Valori ammessi: dev, test, uat, hotfix, prod');
    process.exit(1);
  }

  const expectedHelpdeskHost = expectedHelpdeskHostByEnv[env];
  if (autoToken && helpdeskUrl) {
    let parsed;
    try {
      parsed = new URL(helpdeskUrl);
    } catch (_) {
      console.error(`Valore non valido per --helpdesk-url: ${helpdeskUrl}`);
      process.exit(1);
    }

    if (parsed.host !== expectedHelpdeskHost) {
      console.error(
        `Mismatch env/helpdesk-url: env=${env}, host atteso=${expectedHelpdeskHost}, host ricevuto=${parsed.host}`
      );
      process.exit(1);
    }
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
  } else if (autoToken) {
    const defaultHelpdeskUrl = `https://${expectedHelpdeskHost}`;
    const playwright = require('playwright');
    jwt = await fetchHelpdeskIdToken({
      helpdeskUrl: helpdeskUrl || defaultHelpdeskUrl,
      browser,
      playwright,
    });
  } else {
    const authenticator = new Authenticator(username, password, clientId);
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
