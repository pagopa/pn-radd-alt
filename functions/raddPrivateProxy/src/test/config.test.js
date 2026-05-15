const assert = require("node:assert/strict");
const { test } = require("node:test");
const {
  DEFAULT_ALLOWED_PATH_PREFIX,
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
    RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL: "HTTPS",
    RADD_PRIVATE_PROXY_EXTERNAL_PORT: "8443",
    RADD_PRIVATE_PROXY_TRUSTED_HEADERS: JSON.stringify({
      "X-PAGOPA-PN-SRC-CH": "RADD",
      uid: "RADD_cf_97103880585",
      empty: ""
    }),
    RADD_PRIVATE_PROXY_VERBOSE_LOGGING: "true"
  });

  assert.deepEqual(config, {
    allowedPathPrefix: DEFAULT_ALLOWED_PATH_PREFIX,
    backendBaseUrl: "http://internal-alb:8080",
    baseUrlPort: "8443",
    baseUrlProtocol: "https",
    region: "eu-south-1",
    trustedHeaders: {
      "x-pagopa-pn-src-ch": "RADD",
      uid: "RADD_cf_97103880585"
    },
    verboseLogging: true
  });
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
