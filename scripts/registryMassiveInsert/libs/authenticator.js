const {
  CognitoIdentityProviderClient,
  InitiateAuthCommand,
  AdminInitiateAuthCommand
} = require('@aws-sdk/client-cognito-identity-provider');
const http = require('http');
const { execFile, execFileSync } = require('child_process');
const url = require('url');

class Authenticator {
  /**
   * Supporta tre modalità:
   * 1) Login locale (username + password)
   * 2) AWS SDK Admin Auth (usando AWS Profile già loggato via SSO CLI) - Più elegante
   * 3) Google SSO Browser (flusso localhost) - Fallback
   *
   * @param {string|null} username - Nome utente Cognito (null per SSO)
   * @param {string|null} password - Password Cognito (null per SSO)
   * @param {string} clientId - Client ID dell'app client Cognito
   * @param {object} [options]
   * @param {string} [options.env] - Ambiente (dev, uat, ecc)
   * @param {string} [options.awsProfile] - Profilo AWS CLI da usare
   */
  constructor(username, password, clientId, options = {}) {
    if (!clientId) {
      throw new Error("clientId è obbligatorio");
    }

    this.clientId = clientId;
    this.username = username || null;
    this.password = password || null;
    this.env = options.env || process.env.ENV || process.env.ENVIRONMENT || null;
    this.awsProfile = options.awsProfile || process.env.AWS_PROFILE || null;
    this.userPoolId = options.userPoolId || process.env.COGNITO_USER_POOL_ID || null;
    
    // Auto-costruzione dominio se manca ma abbiamo l'ambiente
    let domain = options.cognitoDomain || process.env.COGNITO_DOMAIN || null;
    if (!domain && this.env) {
      domain = `pn-helpdesk-${this.env}.auth.eu-south-1.amazoncognito.com`;
    }
    this.cognitoDomain = domain;

    this.port = options.port || parseInt(process.env.COGNITO_REDIRECT_PORT || '8087', 10);
    
    // Auto-costruzione IDP name se manca ma abbiamo l'ambiente
    let idp = options.idpName || process.env.COGNITO_IDP_NAME || 'Google';
    if (idp === 'Google' && this.env) {
      idp = `GoogleSAML-${this.env}`;
    }
    this.idpName = idp;

    // Determina modalità
    if (this.username && this.password) {
      this.authMode = 'local';
    } else {
      this.authMode = 'localhost';
    }

    // Inizializza client (per local)
    if (this.authMode === 'local') {
      const clientConfig = { region: 'eu-south-1' };
      this.client = new CognitoIdentityProviderClient(clientConfig);
    }
  }

  /**
   * Esegue login e restituisce un JWT (IdToken).
   * @returns {Promise<string>} IdToken (JWT)
   */
  async generateJwtToken() {
    if (this.authMode === 'localhost') {
      return this._authenticateLocalhost();
    }
    
    return this._authenticateLocal();
  }

  /**
   * Autenticazione via Browser (SSO Google) - completamente automatica.
   * Apre il browser, l'utente fa login, il token viene catturato automaticamente.
   */
  async _authenticateLocalhost() {
    return new Promise((resolve, reject) => {
      const port = 3000;
      const callbackUrl = `http://localhost:${port}/callback`;
      
      if (!this.cognitoDomain) {
        reject(new Error("Dominio Cognito non configurato. Passa --env o imposta ENV nel .env"));
        return;
      }

      const loginUrl = `https://${this.cognitoDomain}/oauth2/authorize?identity_provider=${this.idpName}&client_id=${this.clientId}&response_type=token&scope=email+openid+profile&redirect_uri=${encodeURIComponent(callbackUrl)}`;

      const server = http.createServer((req, res) => {
        const reqUrl = url.parse(req.url, true);

        if (reqUrl.pathname === "/callback") {
          // Cognito implicit flow restituisce il token nel fragment (#).
          // Il server non lo riceve, quindi serviamo una pagina che lo estrae via JS.
          res.writeHead(200, { "Content-Type": "text/html" });
          res.end(`<!DOCTYPE html>
<html><body style="font-family:sans-serif;text-align:center;padding-top:50px">
<h2>Autenticazione in corso...</h2>
<script>
  var h = window.location.hash.substring(1);
  if (h) {
    var p = new URLSearchParams(h);
    var t = p.get('id_token');
    if (t) {
      var x = new XMLHttpRequest();
      x.open('GET', '/callback/token?id_token=' + t);
      x.onload = function() {
        document.body.innerHTML = '<h1>Login completato!</h1><p>Puoi chiudere questa finestra.</p>';
      };
      x.send();
    } else {
      document.body.innerHTML = '<h1>Errore: id_token non trovato.</h1>';
    }
  } else {
    document.body.innerHTML = '<h1>Errore: nessun token ricevuto.</h1>';
  }
</script>
</body></html>`);
          return;
        }

        if (reqUrl.pathname === "/callback/token" && reqUrl.query.id_token) {
          res.writeHead(200, { "Content-Type": "text/plain" });
          res.end("OK");
          server.closeAllConnections();
          server.close();
          server.unref();
          console.log('[Authenticator] Token ricevuto automaticamente.');
          resolve(reqUrl.query.id_token);
          return;
        }
      });

      server.listen(port, () => {
        console.log(`[Authenticator] Server in ascolto su porta ${port}...`);
        console.log(`[Authenticator] Apertura browser per login SSO...`);
        const onOpen = (err) => {
          if (err) {
            console.error('Impossibile aprire il browser automaticamente. Apri manualmente:', loginUrl);
          }
        };
        if (process.platform === 'darwin') {
          execFile('open', [loginUrl], onOpen);
        } else if (process.platform === 'win32') {
          execFile('explorer.exe', [loginUrl], onOpen);
        } else {
          execFile('xdg-open', [loginUrl], onOpen);
        }
      });

      server.on('error', (err) => {
        if (err.code === 'EADDRINUSE') {
          reject(new Error(`Porta ${port} gia' in uso. Chiudi eventuali altri processi sulla porta ${port}.`));
        } else {
          reject(err);
        }
      });
    });
  }

  /**
   * Autenticazione via AWS SDK usando i permessi dell'operatore (via CLI).
   * Non richiede password né apre il browser.
   */
  async _authenticateAWSAdmin() {
    const userPoolId = this.userPoolId;
    
    if (!userPoolId) {
      throw new Error(`User Pool ID mancante. Passalo con --user-pool-id o impostalo nel .env (COGNITO_USER_POOL_ID).`);
    }

    // Se l'utente non è passato, proviamo a prenderlo da env o a dedurlo dal profilo
    let targetUsername = this.username || process.env.COGNITO_USERNAME;
    
    if (!targetUsername && (this.awsProfile || process.env.AWS_PROFILE)) {
      try {
        console.log(`[Authenticator] Tentativo recupero username da profilo AWS...`);
        const profile = this.awsProfile || process.env.AWS_PROFILE;
        const identity = execFileSync(
          'aws',
          ['sts', 'get-caller-identity', '--profile', profile, '--query', 'Arn', '--output', 'text'],
          { encoding: 'utf8' }
        ).trim();
        console.log(`[Authenticator] ARN rilevato: ${identity}`);
        // L'ARN solitamente contiene l'email alla fine dopo / per utenti SSO
        if (identity.includes('/')) {
          targetUsername = identity.split('/').pop();
          console.log(`[Authenticator] Username estratto dall'ARN: ${targetUsername}`);
        }
      } catch (e) {
        console.error(`[Authenticator] Impossibile recuperare identità AWS: ${e.message}`);
      }
    }

    if (!targetUsername) {
      throw new Error("Username/Email non fornito. Configura COGNITO_USERNAME nel .env o passalo come argomento.");
    }

    // Per utenti SSO/External Provider, l'username in Cognito è spesso prefissato (es. GoogleSAML-dev_email@...)
    // Dal tuo output list-users, l'username esatto è "googlesaml-dev_matteo.brachi@pagopa.it"
    const usernamesToTry = [
      `googlesaml-dev_${targetUsername}`,
      `${this.idpName.toLowerCase()}_${targetUsername}`,
      targetUsername,
      `GoogleSAML-dev_${targetUsername}`,
      `${this.idpName}_${targetUsername}`
    ];

    let lastError = null;
    for (const [index, usernameToTry] of usernamesToTry.entries()) {
      try {
        console.log(`[Authenticator] Login via CLI (Admin Auth) - Tentativo ${index + 1}/${usernamesToTry.length}...`);
        
        const { AdminInitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');

        // Se ALLOW_ADMIN_USER_PASSWORD_AUTH è attivo, Cognito ESIGE il parametro PASSWORD.
        // Essendo un utente federato senza password, usiamo una stringa dummy.
        // I tuoi permessi Admin AWS (IAM) dovrebbero validare la richiesta, 
        // mentre il parametro serve solo a superare il controllo formale del Client ID.
        const command = new AdminInitiateAuthCommand({
          UserPoolId: userPoolId,
          ClientId: this.clientId,
          AuthFlow: 'ADMIN_USER_PASSWORD_AUTH', 
          AuthParameters: {
            USERNAME: usernameToTry,
            PASSWORD: 'DUMMY_PASSWORD_FOR_FEDERATED_USER'
          }
        });

        const response = await this.client.send(command);
        console.log(`[Authenticator] Login riuscito (tentativo ${index + 1}/${usernamesToTry.length}).`);
        return response.AuthenticationResult.IdToken;
      } catch (error) {
        lastError = error;
        console.log(`[Authenticator] Tentativo ${index + 1}/${usernamesToTry.length} fallito: ${error.name}`);
        continue;
      }
    }

    console.error(`[Authenticator] Errore critico: ${lastError.message}`);
    throw new Error(`L'utente non esiste o il flusso non è autorizzato per nessuno degli alias provati: ${usernamesToTry.join(', ')}.`);
  }

  async _authenticateLocal() {
    const command = new InitiateAuthCommand({
      AuthFlow: "USER_PASSWORD_AUTH",
      ClientId: this.clientId,
      AuthParameters: {
        USERNAME: this.username,
        PASSWORD: this.password
      }
    });

    try {
      const response = await this.client.send(command);
      const token = response?.AuthenticationResult?.IdToken;

      if (!token) {
        throw new Error("Token JWT non restituito da Cognito");
      }

      return token;
    } catch (err) {
      console.error("Errore durante l'autenticazione Cognito:", err.message || err);
      throw err;
    }
  }

  async _authenticateSSO() {
    try {
      const result = await authenticateWithSSO({
        cognitoDomain: this.cognitoDomain,
        clientId: this.clientId,
        port: this.port,
        idpName: this.idpName,
        useIdToken: true,
      });
      return result.token;
    } catch (err) {
      console.error("Errore durante l'autenticazione SSO:", err.message || err);
      throw err;
    }
  }
}

module.exports = { Authenticator };