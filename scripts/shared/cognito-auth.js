/**
 * Modulo di autenticazione Cognito condiviso.
 *
 * Supporta due modalità:
 *  1) Token statico - Token ottenuto manualmente dal portale web e passato via
 *     opzione `--token` (o variabile ambiente API_TOKEN). È l'unica modalità
 *     supportata per gli utenti SSO/Google, dato che il flusso SAML richiede
 *     un browser interattivo che non può essere automatizzato dalla CLI.
 *  2) USER_PASSWORD_AUTH - Login locale con username/password (solo per utenti
 *     locali Cognito, NON federati).
 *
 * Workflow consigliato:
 *  - Utenti SSO: effettuare login sul portale helpdesk, copiare l'idToken dal
 *    LocalStorage del browser e passarlo allo script con `--token <idToken>`.
 *  - Utenti locali: configurare COGNITO_USERNAME / COGNITO_PASSWORD nel .env.
 *
 * Variabili ambiente per login locale:
 *  - COGNITO_REGION
 *  - COGNITO_CLIENT_ID
 *  - COGNITO_USERNAME
 *  - COGNITO_PASSWORD
 */

'use strict';

const { CognitoIdentityProviderClient, InitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');

// --------------- Local Flow (USER_PASSWORD_AUTH) ---------------

/**
 * Esegue login locale con username/password via Cognito USER_PASSWORD_AUTH.
 *
 * @param {object} opts
 * @param {string} opts.region - AWS Region
 * @param {string} opts.clientId - Cognito App Client ID
 * @param {string} opts.username - Username
 * @param {string} opts.password - Password
 * @param {boolean} [opts.useIdToken=true] - Ritorna IdToken anziché AccessToken
 * @returns {Promise<{token: string, expiresAt: number}>}
 */
async function authenticateWithPassword(opts) {
  const { region, clientId, username, password, useIdToken = true } = opts;

  if (!clientId) throw new Error('COGNITO_CLIENT_ID è obbligatorio');
  if (!username) throw new Error('COGNITO_USERNAME è obbligatorio');
  if (!password) throw new Error('COGNITO_PASSWORD è obbligatorio');

  const client = new CognitoIdentityProviderClient({ region });
  const command = new InitiateAuthCommand({
    AuthFlow: 'USER_PASSWORD_AUTH',
    ClientId: clientId,
    AuthParameters: {
      USERNAME: username,
      PASSWORD: password,
    },
  });

  const resp = await client.send(command);
  const token = useIdToken
    ? resp.AuthenticationResult.IdToken
    : resp.AuthenticationResult.AccessToken;

  if (!token) throw new Error('Token non restituito da Cognito');

  const payload = decodeJwt(token);
  const expiresAt = (payload && payload.exp) || (Math.floor(Date.now() / 1000) + 3600);

  return { token, expiresAt };
}

// --------------- Utility ---------------

function decodeJwt(token) {
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    const payload = Buffer.from(parts[1].replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
    return JSON.parse(payload);
  } catch {
    return null;
  }
}

// --------------- CognitoAuth Class ---------------

/**
 * Classe wrapper che gestisce automaticamente la modalità di autenticazione
 * e il refresh del token (solo per la modalità local).
 */
class CognitoAuth {
  /**
   * @param {object} [options]
   * @param {boolean} [options.useIdToken] - Se usare IdToken (default: true)
   * @param {string} [options.staticToken] - Token statico (bypassa Cognito).
   *   Tipicamente passato via flag CLI `--token` o variabile ambiente API_TOKEN.
   */
  constructor(options = {}) {
    this.staticToken = options.staticToken || process.env.API_TOKEN || null;
    this.useIdToken = options.useIdToken !== undefined
      ? options.useIdToken
      : (process.env.COGNITO_USE_ID_TOKEN || 'true').toLowerCase() === 'true';

    this.authMode = this.staticToken ? 'static' : 'local';

    this._token = null;
    this._tokenExpiresAt = 0;
    this._tokenPromise = null;
    this._tokenMarginSeconds = parseInt(process.env.COGNITO_TOKEN_MARGIN || '30', 10);

    this._validate();
  }

  _validate() {
    if (this.authMode === 'static') {
      console.log('Uso token statico (passato via --token o API_TOKEN).');
      return;
    }

    const required = ['COGNITO_REGION', 'COGNITO_CLIENT_ID', 'COGNITO_USERNAME', 'COGNITO_PASSWORD'];
    const missing = required.filter(k => !process.env[k] || process.env[k].trim() === '');
    if (missing.length) {
      throw new Error(
        `Variabili Cognito mancanti: ${missing.join(', ')}.\n\n` +
        `Per utenti SSO/Google: effettua il login sul portale helpdesk, copia\n` +
        `l'idToken dal LocalStorage del browser e passalo allo script con\n` +
        `l'opzione --token <idToken> (oppure imposta API_TOKEN nel .env).\n\n` +
        `Per utenti locali Cognito (NON federati) imposta nel .env:\n` +
        `  COGNITO_REGION=eu-south-1\n` +
        `  COGNITO_CLIENT_ID=xxxxxxxx\n` +
        `  COGNITO_USERNAME=utente@example.com\n` +
        `  COGNITO_PASSWORD=Password123!`
      );
    }
    console.log('Modalità autenticazione: Cognito locale (username/password)');
  }

  /**
   * Ritorna un token valido. Se scaduto o non presente, lo rinnova
   * (solo in modalità local; in modalità static il token è immutabile).
   * @returns {Promise<string>}
   */
  async getToken() {
    if (this.authMode === 'static') {
      return this.staticToken;
    }

    const now = Math.floor(Date.now() / 1000);
    if (this._token && now < (this._tokenExpiresAt - this._tokenMarginSeconds)) {
      return this._token;
    }

    if (this._tokenPromise) return this._tokenPromise;

    this._tokenPromise = this._authenticate();
    try {
      return await this._tokenPromise;
    } finally {
      this._tokenPromise = null;
    }
  }

  async _authenticate() {
    const result = await authenticateWithPassword({
      region: process.env.COGNITO_REGION,
      clientId: process.env.COGNITO_CLIENT_ID,
      username: process.env.COGNITO_USERNAME,
      password: process.env.COGNITO_PASSWORD,
      useIdToken: this.useIdToken,
    });

    this._token = result.token;
    this._tokenExpiresAt = result.expiresAt;
    const now = Math.floor(Date.now() / 1000);
    const ttl = this._tokenExpiresAt - now;
    console.log(`Token (${this.useIdToken ? 'ID' : 'Access'}) ottenuto. TTL ${ttl}s`);

    return this._token;
  }
}

module.exports = {
  CognitoAuth,
  authenticateWithPassword,
  decodeJwt,
};
