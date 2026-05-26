function parseAllowedPathPrefixes(rawAllowedPathPrefixes) {
  if (rawAllowedPathPrefixes === undefined || rawAllowedPathPrefixes === null) {
    return [];
  }

  const allowedPathPrefixes = String(rawAllowedPathPrefixes)
    .split(",")
    .map((prefix) => prefix.trim())
    .filter(Boolean);

  if (allowedPathPrefixes.length === 0) {
    return [];
  }

  return allowedPathPrefixes;
}

function parseTrustedHeaders(rawTrustedHeaders) {
  if (!rawTrustedHeaders) {
    throw new Error("Missing trusted headers configuration");
  }

  const parsed = JSON.parse(rawTrustedHeaders);
  if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
    throw new Error("Trusted headers configuration must be a JSON object");
  }

  return Object.entries(parsed).reduce((acc, [name, value]) => {
    if (!name || value === undefined || value === null || value === "") {
      return acc;
    }
    acc[name.toLowerCase()] = String(value);
    return acc;
  }, {});
}

function parseBaseUrlPort(rawBaseUrlPort) {
  if (rawBaseUrlPort === undefined || rawBaseUrlPort === null) {
    throw new Error("Missing base URL port configuration");
  }

  const baseUrlPort = String(rawBaseUrlPort).trim();
  if (!baseUrlPort) {
    throw new Error("Missing base URL port configuration");
  }

  if (!/^\d+$/.test(baseUrlPort)) {
    throw new Error("Base URL port configuration must be numeric");
  }

  const numericPort = Number(baseUrlPort);
  if (numericPort < 1 || numericPort > 65535) {
    throw new Error("Base URL port configuration is out of range");
  }

  return baseUrlPort;
}

function parseBaseUrlProtocol(rawBaseUrlProtocol) {
  if (rawBaseUrlProtocol === undefined || rawBaseUrlProtocol === null) {
    throw new Error("Missing base URL protocol configuration");
  }

  const baseUrlProtocol = String(rawBaseUrlProtocol).trim().toLowerCase();
  if (!baseUrlProtocol) {
    throw new Error("Missing base URL protocol configuration");
  }

  if (baseUrlProtocol !== "https" && baseUrlProtocol !== "http") {
    throw new Error("Base URL protocol configuration must be http or https");
  }

  return baseUrlProtocol;
}

function parseBooleanFlag(rawFlag, defaultValue = false) {
  if (rawFlag === undefined || rawFlag === null || String(rawFlag).trim() === "") {
    return defaultValue;
  }

  const normalizedFlag = String(rawFlag).trim().toLowerCase();
  if (normalizedFlag === "true") {
    return true;
  }

  if (normalizedFlag === "false") {
    return false;
  }

  throw new Error("Verbose logging flag must be true or false");
}

function parseNonNegativeIntegerFlag(rawValue, defaultValue, flagName, minimum = 0) {
  if (rawValue === undefined || rawValue === null || String(rawValue).trim() === "") {
    return defaultValue;
  }

  const normalizedValue = String(rawValue).trim();
  if (!/^\d+$/.test(normalizedValue)) {
    throw new Error(`${flagName} must be a non-negative integer`);
  }

  const numericValue = Number(normalizedValue);
  if (numericValue < minimum) {
    throw new Error(`${flagName} must be greater than or equal to ${minimum}`);
  }

  return numericValue;
}

function parseRuntimeConfig(env) {
  const trustedHeaders = parseTrustedHeaders(env.RADD_PRIVATE_PROXY_TRUSTED_HEADERS);
  const backendBaseUrl = env.RADD_PRIVATE_PROXY_BACKEND_BASE_URL;

  if (!backendBaseUrl) {
    throw new Error("Missing backend base URL configuration");
  }

  return {
    allowedPathPrefixes: parseAllowedPathPrefixes(env.RADD_PRIVATE_PROXY_ALLOWED_PATH_PREFIX),
    backendBaseUrl: backendBaseUrl.replace(/\/$/, ""),
    backendRequestTimeoutMillis: parseNonNegativeIntegerFlag(
      env.RADD_PRIVATE_PROXY_BACKEND_REQUEST_TIMEOUT_MILLIS,
      2000,
      "Backend request timeout flag",
      1
    ),
    backendRetryDelayMillis: parseNonNegativeIntegerFlag(
      env.RADD_PRIVATE_PROXY_BACKEND_RETRY_DELAY_MILLIS,
      500,
      "Backend retry delay flag"
    ),
    backendRetryMaxAttempts: parseNonNegativeIntegerFlag(
      env.RADD_PRIVATE_PROXY_BACKEND_RETRY_MAX_ATTEMPTS,
      3,
      "Backend retry max attempts flag",
      1
    ),
    requestPayloadLoggingEnabled: parseBooleanFlag(env.RADD_PRIVATE_PROXY_REQUEST_PAYLOAD_LOGGING_ENABLED, false),
    responsePayloadLoggingEnabled: parseBooleanFlag(env.RADD_PRIVATE_PROXY_RESPONSE_PAYLOAD_LOGGING_ENABLED, false),
    baseUrlPort: parseBaseUrlPort(env.RADD_PRIVATE_PROXY_EXTERNAL_PORT),
    baseUrlProtocol: parseBaseUrlProtocol(env.RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL),
    trustedHeaders
  };
}

module.exports = {
  parseAllowedPathPrefixes,
  parseBaseUrlPort,
  parseBaseUrlProtocol,
  parseBooleanFlag,
  parseRuntimeConfig,
  parseTrustedHeaders
};
