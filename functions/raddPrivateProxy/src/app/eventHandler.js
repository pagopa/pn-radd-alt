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
const {
  parseRuntimeConfig
} = require("./config");

let defaultHandler;

function logError(message, err, meta = {}) {
  console.error(message, {
    ...meta,
    message: err.message,
    stack: err.stack
  });
}

function selectInboundHeadersForLog(incomingHeaders) {
  return INBOUND_HEADER_LOG_NAMES.reduce((acc, name) => {
    if (incomingHeaders[name] !== undefined) {
      acc[name] = incomingHeaders[name];
    }
    return acc;
  }, {});
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
      logError("RADD private proxy failed Host validation", err, { path });
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
      logError("RADD private proxy backend forward failed", err, { path });
      return buildAlbResponse(502, JSON.stringify({ message: "Backend forward failed" }), {
        "content-type": "application/json"
      }, false, "Bad Gateway");
    }
  };
}

async function handleEvent(event, context) {
  if (!defaultHandler) {
    defaultHandler = createHandler();
  }
  return defaultHandler(event, context);
}

module.exports = {
  createHandler,
  deriveBaseUrlFromHost,
  handleEvent,
  isAllowedPath,
  selectInboundHeadersForLog
};
