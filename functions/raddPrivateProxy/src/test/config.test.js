const assert = require("node:assert/strict");
const { test } = require("node:test");
const {
  parseAllowedPathPrefixes,
  parseBaseUrlPort,
  parseBaseUrlProtocol,
  parseBooleanFlag,
  parseRuntimeConfig,
  parseTrustedHeaders
} = require("../app/config");

test("parseRuntimeConfig parses and normalizes runtime configuration", () => {
  const config = parseRuntimeConfig({
    AWS_REGION: "eu-south-1",
    RADD_PRIVATE_PROXY_BACKEND_BASE_URL: "http://internal-alb:8080/",
    RADD_PRIVATE_PROXY_BACKEND_REQUEST_TIMEOUT_MILLIS: "1500",
    RADD_PRIVATE_PROXY_BACKEND_RETRY_DELAY_MILLIS: "250",
    RADD_PRIVATE_PROXY_BACKEND_RETRY_MAX_ATTEMPTS: "4",
    RADD_PRIVATE_PROXY_REQUEST_PAYLOAD_LOGGING_ENABLED: "true",
    RADD_PRIVATE_PROXY_RESPONSE_PAYLOAD_LOGGING_ENABLED: "false",
    RADD_PRIVATE_PROXY_ALLOWED_PATH_PREFIX: "/radd-net/",
    RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL: "HTTPS",
    RADD_PRIVATE_PROXY_EXTERNAL_PORT: "8443",
    RADD_PRIVATE_PROXY_TRUSTED_HEADERS: JSON.stringify({
      "X-PAGOPA-PN-SRC-CH": "RADD",
      "x-pagopa-pn-uid": "RADD_cf_97103880585",
      empty: ""
    })
  });

  assert.deepEqual(config, {
    allowedPathPrefixes: ["/radd-net/"],
    backendBaseUrl: "http://internal-alb:8080",
    backendRequestTimeoutMillis: 1500,
    backendRetryDelayMillis: 250,
    backendRetryMaxAttempts: 4,
    requestPayloadLoggingEnabled: true,
    responsePayloadLoggingEnabled: false,
    baseUrlPort: "8443",
    baseUrlProtocol: "https",
    trustedHeaders: {
      "x-pagopa-pn-src-ch": "RADD",
      "x-pagopa-pn-uid": "RADD_cf_97103880585"
    }
  });
});

test("parseAllowedPathPrefixes supports a comma-separated list", () => {
  assert.deepEqual(parseAllowedPathPrefixes("/radd-net/, /radd-extra"), ["/radd-net/", "/radd-extra"]);
  assert.deepEqual(parseAllowedPathPrefixes(""), []);
  assert.deepEqual(parseAllowedPathPrefixes(undefined), []);
});

test("parseTrustedHeaders requires a JSON object", () => {
  assert.throws(() => parseTrustedHeaders(), /Missing trusted headers configuration/);
  assert.throws(() => parseTrustedHeaders("[]"), /Trusted headers configuration must be a JSON object/);
});

test("parseBaseUrlPort validates required numeric port range", () => {
  assert.equal(parseBaseUrlPort("443"), "443");
  assert.throws(() => parseBaseUrlPort(""), /Missing base URL port configuration/);
  assert.throws(() => parseBaseUrlPort("https"), /Base URL port configuration must be numeric/);
  assert.throws(() => parseBaseUrlPort("65536"), /Base URL port configuration is out of range/);
});

test("parseBaseUrlProtocol accepts only http and https", () => {
  assert.equal(parseBaseUrlProtocol("HTTPS"), "https");
  assert.equal(parseBaseUrlProtocol("http"), "http");
  assert.throws(() => parseBaseUrlProtocol(""), /Missing base URL protocol configuration/);
  assert.throws(() => parseBaseUrlProtocol("ftp"), /Base URL protocol configuration must be http or https/);
});

test("parseBooleanFlag parses optional true or false values", () => {
  assert.equal(parseBooleanFlag(undefined, true), true);
  assert.equal(parseBooleanFlag("true"), true);
  assert.equal(parseBooleanFlag("FALSE"), false);
  assert.throws(() => parseBooleanFlag("yes"), /Verbose logging flag must be true or false/);
});

test("parseRuntimeConfig validates backend retry settings", () => {
  assert.throws(
    () => parseRuntimeConfig({
      RADD_PRIVATE_PROXY_BACKEND_BASE_URL: "http://internal-alb:8080",
      RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL: "https",
      RADD_PRIVATE_PROXY_EXTERNAL_PORT: "8443",
      RADD_PRIVATE_PROXY_TRUSTED_HEADERS: JSON.stringify({
        "x-pagopa-pn-src-ch": "RADD"
      }),
      RADD_PRIVATE_PROXY_BACKEND_RETRY_MAX_ATTEMPTS: "0"
    }),
    /Backend retry max attempts flag must be greater than or equal to 1/
  );
});
