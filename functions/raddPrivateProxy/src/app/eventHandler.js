const DEFAULT_ALLOWED_PATH_PREFIX = "/radd-net/api/v1/act/";
const INBOUND_HEADER_LOG_NAMES = [
  "host",
  "x-forwarded-for",
  "x-forwarded-port",
  "x-forwarded-proto",
  "x-pagopa-pn-base-url",
  "x-pagopa-pn-src-ch",
  "x-pagopa-pn-src-ch-details"
];
const {
  buildAlbResponse,
  buildForwardHeaders,
  buildQueryString,
  buildRequestBody,
  collectHeaders,
  filterResponseHeaders,
  isTextualResponse
} = require("./http");

let defaultHandler;

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

function selectInboundHeadersForLog(incomingHeaders) {
  return INBOUND_HEADER_LOG_NAMES.reduce((acc, name) => {
    if (incomingHeaders[name] !== undefined) {
      acc[name] = incomingHeaders[name];
    }
    return acc;
  }, {});
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

function isAllowedPath(path, allowedPathPrefix) {
  const normalizedPrefix = allowedPathPrefix.endsWith("/")
    ? allowedPathPrefix
    : `${allowedPathPrefix}/`;
  const prefixWithoutTrailingSlash = normalizedPrefix.replace(/\/$/, "");

  return path === prefixWithoutTrailingSlash || path.startsWith(normalizedPrefix);
}

function deriveBaseUrlFromHost(hostHeader, config) {
  if (!hostHeader) {
    throw new Error("Missing Host header");
  }

  let parsedHost;
  try {
    parsedHost = new URL(`${config.baseUrlProtocol}://${hostHeader}`);
  } catch (err) {
    throw new Error("Invalid Host header");
  }

  const hostname = parsedHost.hostname.toLowerCase();

  if (config.baseUrlProtocol === "https" && config.baseUrlPort === "443") {
    return `${config.baseUrlProtocol}://${hostname}`;
  }

  if (config.baseUrlProtocol === "http" && config.baseUrlPort === "80") {
    return `${config.baseUrlProtocol}://${hostname}`;
  }

  return `${config.baseUrlProtocol}://${hostname}:${config.baseUrlPort}`;
}

function createHandler({ fetchImpl = globalThis.fetch, env = process.env } = {}) {
  if (!fetchImpl) {
    throw new Error("Missing fetch implementation");
  }

  const config = parseRuntimeConfig(env);

  return async function raddPrivateProxyHandler(event) {
    const method = event.httpMethod || "GET";
    const path = event.path || "/";
    const incomingHeaders = collectHeaders(event);

    console.log("RADD private proxy inbound headers", {
      method,
      path,
      headers: selectInboundHeadersForLog(incomingHeaders)
    });

    // Full ingress dump is opt-in for troubleshooting.
    if (config.verboseLogging) {
      console.log("RADD private proxy inbound request payload", {
        method,
        path,
        headers: incomingHeaders,
        queryStringParameters: event.queryStringParameters || null,
        body: event.body || null,
        isBase64Encoded: Boolean(event.isBase64Encoded)
      });
    }

    if (!isAllowedPath(path, config.allowedPathPrefix)) {
      console.warn("RADD private proxy rejected path", { method, path });
      return buildAlbResponse(403, JSON.stringify({ message: "Forbidden" }), {
        "content-type": "application/json"
      }, false, "Forbidden");
    }

    let baseUrl;
    try {
      baseUrl = deriveBaseUrlFromHost(incomingHeaders.host, config);
    } catch (err) {
      console.error("RADD private proxy failed Host validation", { message: err.message, path });
      return buildAlbResponse(500, JSON.stringify({ message: "Unable to derive trusted base URL" }), {
        "content-type": "application/json"
      }, false, "Internal Server Error");
    }

    const queryString = buildQueryString(event);
    const backendUrl = `${config.backendBaseUrl}${path}${queryString}`;
    const forwardHeaders = buildForwardHeaders(incomingHeaders, config, baseUrl);

    try {
      const backendResponse = await fetchImpl(backendUrl, {
        method,
        headers: forwardHeaders,
        body: buildRequestBody(event, method)
      });

      const responseHeaders = filterResponseHeaders(backendResponse.headers);
      const responseBuffer = Buffer.from(await backendResponse.arrayBuffer());
      const contentType = responseHeaders["content-type"] || "";
      const textualResponse = isTextualResponse(contentType);

      console.log("RADD private proxy forwarded request", {
        method,
        path,
        statusCode: backendResponse.status
      });

      return buildAlbResponse(
        backendResponse.status,
        textualResponse ? responseBuffer.toString("utf8") : responseBuffer.toString("base64"),
        responseHeaders,
        !textualResponse,
        backendResponse.statusText
      );
    } catch (err) {
      console.error("RADD private proxy backend forward failed", { message: err.message, path });
      return buildAlbResponse(502, JSON.stringify({ message: "Backend forward failed" }), {
        "content-type": "application/json"
      }, false, "Bad Gateway");
    }
  };
}

async function handleEvent(event, context) {
  try {
    if (!defaultHandler) {
      defaultHandler = createHandler();
    }
    return await defaultHandler(event, context);
  } catch (err) {
    console.error("RADD private proxy initialization failed", { message: err.message });
    return buildAlbResponse(500, JSON.stringify({ message: "Proxy initialization failed" }), {
      "content-type": "application/json"
    }, false, "Internal Server Error");
  }
}

module.exports = {
  createHandler,
  deriveBaseUrlFromHost,
  handleEvent,
  isAllowedPath,
  parseBooleanFlag,
  parseBaseUrlPort,
  parseBaseUrlProtocol,
  parseTrustedHeaders,
  selectInboundHeadersForLog
};
