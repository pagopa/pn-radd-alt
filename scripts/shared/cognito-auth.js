/**
 * Modulo di autenticazione Cognito condiviso.
 * Supporta due modalità:
 *  1) USER_PASSWORD_AUTH - Login locale con username/password
 *  2) Google SSO - Authorization Code Flow con PKCE via Cognito Hosted UI
 *
 * La modalità viene scelta in base alla variabile AUTH_MODE:
 *  - AUTH_MODE=sso   → Google SSO (browser)
 *  - AUTH_MODE=local → USER_PASSWORD_AUTH (default se COGNITO_USERNAME e COGNITO_PASSWORD presenti)
 *
 * Variabili ambiente per SSO:
 *  - COGNITO_DOMAIN          (es. your-domain.auth.eu-central-1.amazoncognito.com)
 *  - COGNITO_CLIENT_ID       (lo stesso usato per local)
 *  - COGNITO_REDIRECT_PORT   (default 8087)
 *  - COGNITO_SCOPES          (default "openid email profile")
 *  - COGNITO_IDP_NAME        (default "Google" - nome identity provider configurato in Cognito)
 *
 * Variabili ambiente per local:
 *  - COGNITO_REGION
 *  - COGNITO_CLIENT_ID
 *  - COGNITO_USERNAME
 *  - COGNITO_PASSWORD
 */

'use strict';

const http = require('http');
const crypto = require('crypto');
const { URL, URLSearchParams } = require('url');
const { CognitoIdentityProviderClient, InitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');

// --------------- PKCE helpers ---------------

function base64url(buffer) {
  return buffer.toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function generateCodeVerifier() {
  return base64url(crypto.randomBytes(32));
}

function generateCodeChallenge(verifier) {
  const hash = crypto.createHash('sha256').update(verifier).digest();
  return base64url(hash);
}

// --------------- Browser open (cross-platform) ---------------

function openBrowser(url) {
  const { execFile } = require('child_process');
  const onOpen = (err) => {
    if (err) {
      console.error('Impossibile aprire il browser automaticamente. Apri manualmente:', url);
    }
  };
  if (process.platform === 'darwin') {
    execFile('open', [url], onOpen);
  } else if (process.platform === 'win32') {
    execFile('cmd', ['/c', 'start', '', url], onOpen);
  } else {
    execFile('xdg-open', [url], onOpen);
  }
}

// --------------- SSO Flow (Authorization Code + PKCE) ---------------

/**
 * Esegue il flusso SSO Google tramite Cognito Hosted UI.
 * Apre il browser, attende il callback con l'authorization code,
 * lo scambia per i token.
 *
 * @param {object} opts
 * @param {string} opts.cognitoDomain - Dominio Cognito Hosted UI
 * @param {string} opts.clientId - App Client ID
 * @param {number} [opts.port=3000] - Porta locale per il callback
 * @param {string} [opts.scopes='openid email profile'] - OAuth scopes
 * @param {string} [opts.idpName='Google'] - Nome IdP in Cognito
 * @param {boolean} [opts.useIdToken=true] - Ritorna IdToken anziché AccessToken
 * @returns {Promise<{token: string, expiresAt: number}>}
 */
async function authenticateWithSSO(opts) {
  const {
    cognitoDomain,
    clientId,
    port = 3000,
    scopes = 'openid email profile',
    idpName = 'Google',
    useIdToken = true,
  } = opts;

  if (!cognitoDomain) throw new Error('COGNITO_DOMAIN è obbligatorio per il flusso SSO');
  if (!clientId) throw new Error('COGNITO_CLIENT_ID è obbligatorio');

  const redirectUri = `http://localhost:${port}/callback`;
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = generateCodeChallenge(codeVerifier);

  // Costruisci URL di autorizzazione
  const authUrl = new URL(`https://${cognitoDomain}/oauth2/authorize`);
  authUrl.searchParams.set('response_type', 'code');
  authUrl.searchParams.set('client_id', clientId);
  authUrl.searchParams.set('redirect_uri', redirectUri);
  authUrl.searchParams.set('scope', scopes);
  authUrl.searchParams.set('code_challenge', codeChallenge);
  authUrl.searchParams.set('code_challenge_method', 'S256');
  if (idpName) {
    authUrl.searchParams.set('identity_provider', idpName);
  }

  return new Promise((resolve, reject) => {
    const server = http.createServer(async (req, res) => {
      try {
        const reqUrl = new URL(req.url, `http://localhost:${port}`);
        if (reqUrl.pathname !== '/callback') {
          res.writeHead(404);
          res.end('Not found');
          return;
        }

        const code = reqUrl.searchParams.get('code');
        const error = reqUrl.searchParams.get('error');

        if (error) {
          const errorDesc = reqUrl.searchParams.get('error_description') || error;
          const safeErrorDesc = escapeHtml(errorDesc);
          res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
          res.end(`<html><body><h2>Errore autenticazione</h2><p>${safeErrorDesc}</p><p>Puoi chiudere questa finestra.</p></body></html>`);
          server.closeAllConnections();
          server.close();
          reject(new Error(`SSO error: ${errorDesc}`));
          return;
        }

        if (!code) {
          res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
          res.end('<html><body><h2>Nessun codice di autorizzazione ricevuto</h2><p>Puoi chiudere questa finestra.</p></body></html>');
          server.closeAllConnections();
          server.close();
          reject(new Error('Nessun authorization code ricevuto'));
          return;
        }

        // Scambia il code per i token
        const tokenUrl = `https://${cognitoDomain}/oauth2/token`;
        const body = new URLSearchParams({
          grant_type: 'authorization_code',
          client_id: clientId,
          code,
          redirect_uri: redirectUri,
          code_verifier: codeVerifier,
        });

        const axios = require('axios');
        const tokenResp = await axios.post(tokenUrl, body.toString(), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        });

        const { id_token, access_token, expires_in } = tokenResp.data;
        const selectedToken = useIdToken ? id_token : access_token;

        if (!selectedToken) {
          throw new Error('Token non presente nella risposta di Cognito');
        }

        const expiresAt = Math.floor(Date.now() / 1000) + (expires_in || 3600);

        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end('<html><body><h2>Autenticazione riuscita!</h2><p>Puoi chiudere questa finestra e tornare al terminale.</p></body></html>');
        server.closeAllConnections();
        server.close();
        server.unref();
        resolve({ token: selectedToken, expiresAt });
      } catch (err) {
        const safeMessage = escapeHtml(err && err.message ? err.message : String(err));
        res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`<html><body><h2>Errore</h2><p>${safeMessage}</p></body></html>`);
        server.closeAllConnections();
        server.close();
        reject(err);
      }
    });

    server.listen(port, () => {
      console.log(`\nAvvio flusso SSO Google...`);
      console.log(`Server callback in ascolto su http://localhost:${port}/callback`);
      console.log(`Apertura browser per l'autenticazione...\n`);
      console.log(`Se il browser non si apre, copia e incolla questo URL:\n  ${authUrl.toString()}\n`);
      openBrowser(authUrl.toString());
    });

    server.on('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        reject(new Error(`Porta ${port} già in uso. Configura COGNITO_REDIRECT_PORT con una porta libera.`));
      } else {
        reject(err);
      }
    });

    // Timeout dopo 120 secondi
    setTimeout(() => {
      server.closeAllConnections();
      server.close();
      reject(new Error('Timeout: autenticazione SSO non completata entro 120 secondi'));
    }, 120000);
  });
}

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

  // Decodifica per ottenere exp
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
 * e il refresh del token.
 */
class CognitoAuth {
  /**
   * @param {object} [options]
   * @param {string} [options.authMode] - 'local' | 'sso' (default: auto-detect)
   * @param {boolean} [options.useIdToken] - Se usare IdToken (default: true per SSO, da env per local)
   * @param {string} [options.staticToken] - Token statico (bypassa Cognito)
   */
  constructor(options = {}) {
    this.staticToken = options.staticToken || process.env.API_TOKEN || null;
    this.useIdToken = options.useIdToken !== undefined
      ? options.useIdToken
      : (process.env.COGNITO_USE_ID_TOKEN || 'true').toLowerCase() === 'true';

    // Determina la modalità di autenticazione
    if (this.staticToken) {
      this.authMode = 'static';
    } else if (options.authMode) {
      this.authMode = options.authMode;
    } else {
      this.authMode = this._detectAuthMode();
    }

    this._token = null;
    this._tokenExpiresAt = 0;
    this._tokenPromise = null;
    this._tokenMarginSeconds = parseInt(process.env.COGNITO_TOKEN_MARGIN || '30', 10);

    this._validate();
  }

  _detectAuthMode() {
    const explicitMode = (process.env.AUTH_MODE || '').toLowerCase();
    if (explicitMode === 'sso') return 'sso';
    if (explicitMode === 'local' || explicitMode === 'password') return 'local';

    // Auto-detect: se username e password presenti → local, altrimenti → sso
    if (process.env.COGNITO_USERNAME && process.env.COGNITO_PASSWORD) {
      return 'local';
    }
    if (process.env.COGNITO_DOMAIN || (process.env.ENV || process.env.ENVIRONMENT)) {
      return 'sso';
    }
    // Fallback: local (fallirà nella validate se mancano credenziali)
    return 'local';
  }

  _validate() {
    if (this.authMode === 'static') {
      console.log('Uso token statico da API_TOKEN.');
      return;
    }

    if (this.authMode === 'sso') {
      const required = ['COGNITO_DOMAIN', 'COGNITO_CLIENT_ID'];
      const missing = required.filter(k => !process.env[k] || process.env[k].trim() === '');
      if (missing.length) {
        throw new Error(
          `Variabili mancanti per SSO: ${missing.join(', ')}.\n` +
          `Configura nel .env:\n` +
          `  COGNITO_DOMAIN=your-domain.auth.eu-central-1.amazoncognito.com\n` +
          `  COGNITO_CLIENT_ID=xxxxxxxx\n` +
          `  AUTH_MODE=sso`
        );
      }
      console.log('Modalità autenticazione: SSO Google');
      return;
    }

    // local
    const required = ['COGNITO_REGION', 'COGNITO_CLIENT_ID', 'COGNITO_USERNAME', 'COGNITO_PASSWORD'];
    const missing = required.filter(k => !process.env[k] || process.env[k].trim() === '');
    if (missing.length) {
      throw new Error(
        `Variabili Cognito mancanti: ${missing.join(', ')}.\n` +
        `Per login locale:\n` +
        `  COGNITO_REGION=eu-central-1\n` +
        `  COGNITO_CLIENT_ID=xxxxxxxx\n` +
        `  COGNITO_USERNAME=utente@example.com\n` +
        `  COGNITO_PASSWORD=Password123!\n\n` +
        `Per SSO Google, imposta AUTH_MODE=sso e COGNITO_DOMAIN.`
      );
    }
    console.log('Modalità autenticazione: Cognito locale (username/password)');
  }

  /**
   * Ritorna un token valido. Se scaduto o non presente, lo rinnova.
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

    // Evita richieste concorrenti
    if (this._tokenPromise) return this._tokenPromise;

    this._tokenPromise = this._authenticate();
    try {
      return await this._tokenPromise;
    } finally {
      this._tokenPromise = null;
    }
  }

  async _authenticate() {
    let result;

    if (this.authMode === 'sso') {
      let cognitoDomain = process.env.COGNITO_DOMAIN;
      let idpName = process.env.COGNITO_IDP_NAME || 'Google';
      const env = process.env.ENV || process.env.ENVIRONMENT;

      // Se manca il dominio ma abbiamo l'ambiente, lo costruiamo
      if (!cognitoDomain && env) {
        cognitoDomain = `pn-helpdesk-${env}.auth.eu-south-1.amazoncognito.com`;
        console.log(`[CognitoAuth] Ambiente rilevato: ${env}. Utilizzo dominio: ${cognitoDomain}`);
        // Se l'IDP name è quello di default, lo adattiamo alla convenzione GoogleSAML-<env>
        if (idpName === 'Google') {
          idpName = `GoogleSAML-${env}`;
          console.log(`[CognitoAuth] Provider IDP impostato a: ${idpName}`);
        }
      }

      result = await authenticateWithSSO({
        cognitoDomain,
        clientId: process.env.COGNITO_CLIENT_ID,
        port: parseInt(process.env.COGNITO_REDIRECT_PORT || '3000', 10),
        scopes: process.env.COGNITO_SCOPES || 'openid email profile',
        idpName,
        useIdToken: this.useIdToken,
      });
    } else {
      result = await authenticateWithPassword({
        region: process.env.COGNITO_REGION,
        clientId: process.env.COGNITO_CLIENT_ID,
        username: process.env.COGNITO_USERNAME,
        password: process.env.COGNITO_PASSWORD,
        useIdToken: this.useIdToken,
      });
    }

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
  authenticateWithSSO,
  authenticateWithPassword,
  decodeJwt,
};
