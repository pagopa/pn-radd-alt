const NON_FORWARDABLE_REQUEST_HEADERS = new Set([
  "connection",
  "transfer-encoding",
  "keep-alive",
  "upgrade",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "host",
  "content-length"
]);

const BASE_URL_HEADER = "x-pagopa-pn-base-url";

function collectHeaders(event) {
  const headers = {};

  for (const [name, value] of Object.entries(event.headers || {})) {
    if (value !== undefined && value !== null) {
      headers[name.toLowerCase()] = String(value);
    }
  }

  for (const [name, values] of Object.entries(event.multiValueHeaders || {})) {
    if (Array.isArray(values) && values.length > 0) {
      headers[name.toLowerCase()] = values.map(String).join(",");
    }
  }

  return headers;
}

function buildForwardHeaders(incomingHeaders, config, baseUrl) {
  const trustedHeaderNames = new Set([...Object.keys(config.trustedHeaders), BASE_URL_HEADER]);
  const forwardHeaders = {};

  for (const [name, value] of Object.entries(incomingHeaders)) {
    const lowerName = name.toLowerCase();
    if (NON_FORWARDABLE_REQUEST_HEADERS.has(lowerName) || trustedHeaderNames.has(lowerName)) {
      continue;
    }
    forwardHeaders[lowerName] = value;
  }

  for (const [name, value] of Object.entries(config.trustedHeaders)) {
    forwardHeaders[name] = value;
  }
  forwardHeaders[BASE_URL_HEADER] = baseUrl;

  return forwardHeaders;
}

function buildQueryString(event) {
  const params = new URLSearchParams();

  if (event.multiValueQueryStringParameters) {
    for (const [name, values] of Object.entries(event.multiValueQueryStringParameters)) {
      for (const value of values || []) {
        if (value !== undefined && value !== null) {
          params.append(name, String(value));
        }
      }
    }
  } else if (event.queryStringParameters) {
    for (const [name, value] of Object.entries(event.queryStringParameters)) {
      if (value !== undefined && value !== null) {
        params.append(name, String(value));
      }
    }
  }

  const serialized = params.toString();
  return serialized ? `?${serialized}` : "";
}

function buildRequestBody(event, method) {
  if (["GET", "HEAD"].includes(method.toUpperCase()) || event.body === undefined || event.body === null) {
    return undefined;
  }

  if (event.isBase64Encoded) {
    return Buffer.from(event.body, "base64");
  }

  return event.body;
}

function isTextualResponse(contentType) {
  const normalized = (contentType || "").toLowerCase();
  return normalized.startsWith("text/") ||
    normalized.includes("json") ||
    normalized.includes("xml") ||
    normalized.includes("x-www-form-urlencoded");
}

function buildAlbResponse(statusCode, body, headers = {}, isBase64Encoded = false, statusText = "") {
  return {
    statusCode,
    statusDescription: `${statusCode}${statusText ? ` ${statusText}` : ""}`,
    isBase64Encoded,
    headers,
    body
  };
}

function filterResponseHeaders(responseHeaders) {
  const headers = {};
  responseHeaders.forEach((value, name) => {
    const lowerName = name.toLowerCase();
    if (!NON_FORWARDABLE_REQUEST_HEADERS.has(lowerName)) {
      headers[lowerName] = value;
    }
  });
  return headers;
}

module.exports = {
  buildAlbResponse,
  buildForwardHeaders,
  buildQueryString,
  buildRequestBody,
  collectHeaders,
  filterResponseHeaders,
  isTextualResponse
};
