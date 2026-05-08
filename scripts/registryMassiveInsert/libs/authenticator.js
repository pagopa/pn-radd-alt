const {
  CognitoIdentityProviderClient,
  InitiateAuthCommand
} = require('@aws-sdk/client-cognito-identity-provider');
const { authenticateWithSSO } = require('../../shared/cognito-auth');

class Authenticator {
  /**
   * Supporta due modalità:
   * 1) Login locale Cognito (username + password + clientId)
   * 2) SSO Google (cognitoDomain + clientId, senza username/password)
   *
   * Se username e password sono vuoti/null e cognitoDomain è fornito, usa SSO.
   *
   * @param {string|null} username - Nome utente Cognito (null per SSO)
   * @param {string|null} password - Password Cognito (null per SSO)
   * @param {string} clientId - Client ID dell'app client Cognito
   * @param {object} [options]
   * @param {string} [options.cognitoDomain] - Dominio Hosted UI per SSO
   * @param {number} [options.port] - Porta locale callback SSO (default 8087)
   * @param {string} [options.idpName] - Nome identity provider (default 'Google')
   */
  constructor(username, password, clientId, options = {}) {
    if (!clientId) {
      throw new Error("clientId è obbligatorio");
    }

    this.clientId = clientId;
    this.username = username || null;
    this.password = password || null;
    this.cognitoDomain = options.cognitoDomain || process.env.COGNITO_DOMAIN || null;
    this.port = options.port || parseInt(process.env.COGNITO_REDIRECT_PORT || '8087', 10);
    this.idpName = options.idpName || process.env.COGNITO_IDP_NAME || 'Google';

    // Determina modalità: se username+password presenti → local, altrimenti → sso
    if (this.username && this.password) {
      this.authMode = 'local';
    } else if (this.cognitoDomain) {
      this.authMode = 'sso';
    } else {
      throw new Error(
        "Fornisci username e password per login locale, oppure COGNITO_DOMAIN per SSO Google."
      );
    }

    if (this.authMode === 'local') {
      this.client = new CognitoIdentityProviderClient({});
    }
  }

  /**
   * Esegue login e restituisce un JWT (IdToken).
   * @returns {Promise<string>} IdToken (JWT)
   */
  async generateJwtToken() {
    if (this.authMode === 'sso') {
      return this._authenticateSSO();
    }
    return this._authenticateLocal();
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