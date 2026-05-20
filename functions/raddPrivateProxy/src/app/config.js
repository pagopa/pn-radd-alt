const DEFAULT_ALLOWED_PATH_PREFIX = "/radd-net/";

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

function parseRuntimeConfig(env) {
  const trustedHeaders = parseTrustedHeaders(env.RADD_PRIVATE_PROXY_TRUSTED_HEADERS);
  const backendBaseUrl = env.RADD_PRIVATE_PROXY_BACKEND_BASE_URL;

  if (!backendBaseUrl) {
    throw new Error("Missing backend base URL configuration");
  }

  return {
    allowedPathPrefix: env.RADD_PRIVATE_PROXY_ALLOWED_PATH_PREFIX || DEFAULT_ALLOWED_PATH_PREFIX,
    backendBaseUrl: backendBaseUrl.replace(/\/$/, ""),
    baseUrlPort: parseBaseUrlPort(env.RADD_PRIVATE_PROXY_EXTERNAL_PORT),
    baseUrlProtocol: parseBaseUrlProtocol(env.RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL),
    region: env.AWS_REGION || env.AWS_DEFAULT_REGION || "eu-south-1",
    trustedHeaders,
    verboseLogging: parseBooleanFlag(env.RADD_PRIVATE_PROXY_VERBOSE_LOGGING, false)
  };
}

module.exports = {
  DEFAULT_ALLOWED_PATH_PREFIX,
  parseBaseUrlPort,
  parseBaseUrlProtocol,
  parseBooleanFlag,
  parseRuntimeConfig,
  parseTrustedHeaders
};
