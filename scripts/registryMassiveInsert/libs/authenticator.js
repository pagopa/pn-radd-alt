const {
  CognitoIdentityProviderClient,
  InitiateAuthCommand
} = require('@aws-sdk/client-cognito-identity-provider');

/**
 * Autenticazione Cognito via USER_PASSWORD_AUTH (solo utenti locali, NON federati).
 *
 * Per gli utenti SSO/Google il flusso CLI non è supportato: occorre
 * effettuare il login sul portale helpdesk, copiare l'idToken dal LocalStorage
 * del browser e passarlo allo script con l'opzione `--token <idToken>`.
 */
class Authenticator {
  /**
   * @param {string} username - Nome utente Cognito
   * @param {string} password - Password Cognito
   * @param {string} clientId - Client ID dell'app client Cognito
   * @param {object} [options]
   * @param {string} [options.region='eu-south-1']
   */
  constructor(username, password, clientId, options = {}) {
    if (!clientId) throw new Error("clientId è obbligatorio");
    if (!username) throw new Error("username è obbligatorio (per utenti SSO usa --token)");
    if (!password) throw new Error("password è obbligatoria (per utenti SSO usa --token)");

    this.clientId = clientId;
    this.username = username;
    this.password = password;
    this.client = new CognitoIdentityProviderClient({
      region: options.region || 'eu-south-1',
    });
  }

  /**
   * Esegue login e restituisce un JWT (IdToken).
   * @returns {Promise<string>}
   */
  async generateJwtToken() {
    const command = new InitiateAuthCommand({
      AuthFlow: "USER_PASSWORD_AUTH",
      ClientId: this.clientId,
      AuthParameters: {
        USERNAME: this.username,
        PASSWORD: this.password,
      },
    });

    try {
      const response = await this.client.send(command);
      const token = response?.AuthenticationResult?.IdToken;
      if (!token) throw new Error("Token JWT non restituito da Cognito");
      return token;
    } catch (err) {
      console.error("Errore durante l'autenticazione Cognito:", err.message || err);
      throw err;
    }
  }
}

module.exports = { Authenticator };
