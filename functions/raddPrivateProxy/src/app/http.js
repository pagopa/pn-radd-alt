const NON_FORWARDABLE_REQUEST_HEADERS = new Set([
  "connection",
  "transfer-encoding",
  "keep-alive",
  "upgrade",
  "te",
  "trailer",
  "content-length"
]);

const BASE_URL_HEADER = "x-pagopa-pn-base-url";

function serializeHeaderValues(name, values) {
  const normalizedValues = values
    .filter((value) => value !== undefined && value !== null)
    .map(String);

  if (name.toLowerCase() === "cookie") {
    return normalizedValues.join("; ");
  }

  return normalizedValues.join(",");
}

function appendHeaderValue(headers, name, value) {
  if (value === undefined || value === null) {
    return;
  }

  const lowerName = name.toLowerCase();
  if (!headers[lowerName]) {
    headers[lowerName] = [];
  }
  headers[lowerName].push(String(value));
}

function collectHeaders(event) {
  const headers = {};

  for (const [name, value] of Object.entries(event.headers || {})) {
    if (value !== undefined && value !== null) {
      headers[name.toLowerCase()] = String(value);
    }
  }

  for (const [name, values] of Object.entries(event.multiValueHeaders || {})) {
    if (Array.isArray(values) && values.length > 0) {
      headers[name.toLowerCase()] = serializeHeaderValues(name, values);
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
  const params = [];

  if (event.multiValueQueryStringParameters) {
    for (const [name, values] of Object.entries(event.multiValueQueryStringParameters)) {
      for (const value of values || []) {
        if (value !== undefined && value !== null) {
          params.push(`${String(name)}=${String(value)}`);
        }
      }
    }
  } else if (event.queryStringParameters) {
    for (const [name, value] of Object.entries(event.queryStringParameters)) {
      if (value !== undefined && value !== null) {
        params.push(`${String(name)}=${String(value)}`);
      }
    }
  }

  const serialized = params.join("&");
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
  const normalized = (Array.isArray(contentType) ? contentType[0] || "" : contentType || "").toLowerCase();
  return normalized.startsWith("text/") ||
    normalized.includes("json") ||
    normalized.includes("xml") ||
    normalized.includes("x-www-form-urlencoded");
}

function buildAlbResponse(statusCode, body, headers = {}, isBase64Encoded = false, statusText = "") {
  const multiValueHeaders = {};

  for (const [name, value] of Object.entries(headers)) {
    if (Array.isArray(value) && value.length > 0) {
      multiValueHeaders[name.toLowerCase()] = value.map(String);
    } else if (value !== undefined && value !== null) {
      multiValueHeaders[name.toLowerCase()] = [String(value)];
    }
  }

  return {
    statusCode,
    statusDescription: `${statusCode}${statusText ? ` ${statusText}` : ""}`,
    isBase64Encoded,
    multiValueHeaders,
    body
  };
}

function filterResponseHeaders(responseHeaders) {
  const headers = {};
  const setCookieValues = typeof responseHeaders.getSetCookie === "function"
    ? responseHeaders.getSetCookie()
    : [];

  responseHeaders.forEach((value, name) => {
    const lowerName = name.toLowerCase();
    if (lowerName === "set-cookie" && setCookieValues.length > 0) {
      return;
    }

    if (!NON_FORWARDABLE_REQUEST_HEADERS.has(lowerName)) {
      appendHeaderValue(headers, lowerName, value);
    }
  });

  for (const value of setCookieValues) {
    appendHeaderValue(headers, "set-cookie", value);
  }

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
