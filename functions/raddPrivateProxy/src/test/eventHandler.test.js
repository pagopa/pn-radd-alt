const assert = require("node:assert/strict");
const { test } = require("node:test");
const { createHandler } = require("../app/eventHandler");

const trustedHeaders = {
  "x-pagopa-pn-src-ch": "RADD",
  "x-pagopa-pn-src-ch-details": "NONINTEROP",
  "x-pagopa-pn-cx-type": "RADD",
  "x-pagopa-pn-cx-role": "RADD_UPLOADER",
  "x-pagopa-pn-cx-id": "97103880585",
  "uid": "RADD_cf_97103880585",
  "x-pagopa-pn-uid": "RADD_cf_97103880585",
  "x-pagopa-pn-cx-groups": ""
};

const baseEnv = {
  AWS_REGION: "eu-south-1",
  RADD_PRIVATE_PROXY_ALLOWED_PATH_PREFIX: "/radd-net/api/v1/act/",
  RADD_PRIVATE_PROXY_BACKEND_BASE_URL: "http://internal-alb:8080",
  RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL: "https",
  RADD_PRIVATE_PROXY_EXTERNAL_PORT: "8443",
  RADD_PRIVATE_PROXY_TRUSTED_HEADERS: JSON.stringify(trustedHeaders)
};

function buildEvent(overrides = {}) {
  return {
    httpMethod: "POST",
    path: "/radd-net/api/v1/act/inquiry",
    headers: {
      host: "vpce-0159b66963cb51025-1t51z9qh.vpce-svc-0a08e48564915d1c3.eu-south-1.vpce.amazonaws.com:8080",
      "content-type": "application/json",
      "x-pagopa-pn-src-ch": "SPOOFED"
    },
    queryStringParameters: {
      iun: "ABC"
    },
    body: JSON.stringify({ test: true }),
    isBase64Encoded: false,
    ...overrides
  };
}

test("overwrites trusted headers and derives base URL from the technical VPCE host", async () => {
  let capturedRequest;
  const handler = createHandler({
    env: baseEnv,
    fetchImpl: async (url, options) => {
      capturedRequest = { url, options };
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        statusText: "OK",
        headers: { "content-type": "application/json" }
      });
    }
  });

  const response = await handler(buildEvent());

  assert.equal(response.statusCode, 200);
  assert.equal(capturedRequest.url, "http://internal-alb:8080/radd-net/api/v1/act/inquiry?iun=ABC");
  assert.equal(capturedRequest.options.headers["x-pagopa-pn-src-ch"], "RADD");
  assert.equal(capturedRequest.options.headers["x-pagopa-pn-cx-id"], "97103880585");
  assert.equal(capturedRequest.options.headers.uid, "RADD_cf_97103880585");
  assert.equal(
    capturedRequest.options.headers["x-pagopa-pn-base-url"],
    "https://vpce-0159b66963cb51025-1t51z9qh.vpce-svc-0a08e48564915d1c3.eu-south-1.vpce.amazonaws.com:8443"
  );
  assert.equal(capturedRequest.options.headers.host, undefined);
});

test("fails initialization when external base URL port is not configured", () => {
  const { RADD_PRIVATE_PROXY_EXTERNAL_PORT, ...envWithoutExternalPort } = baseEnv;

  assert.throws(
    () => createHandler({ env: envWithoutExternalPort, fetchImpl: async () => new Response("unexpected") }),
    /Missing base URL port configuration/
  );
});

test("fails initialization when external base URL protocol is not configured", () => {
  const { RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL, ...envWithoutExternalProtocol } = baseEnv;

  assert.throws(
    () => createHandler({ env: envWithoutExternalProtocol, fetchImpl: async () => new Response("unexpected") }),
    /Missing base URL protocol configuration/
  );
});

test("omits the port from base URL when external base URL port is 443", async () => {
  let capturedRequest;
  const handler = createHandler({
    env: { ...baseEnv, RADD_PRIVATE_PROXY_EXTERNAL_PORT: "443" },
    fetchImpl: async (url, options) => {
      capturedRequest = { url, options };
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        statusText: "OK",
        headers: { "content-type": "application/json" }
      });
    }
  });

  const response = await handler(buildEvent());

  assert.equal(response.statusCode, 200);
  assert.equal(
    capturedRequest.options.headers["x-pagopa-pn-base-url"],
    "https://vpce-0159b66963cb51025-1t51z9qh.vpce-svc-0a08e48564915d1c3.eu-south-1.vpce.amazonaws.com"
  );
});

test("uses configured external base URL protocol", async () => {
  let capturedRequest;
  const handler = createHandler({
    env: { ...baseEnv, RADD_PRIVATE_PROXY_EXTERNAL_PROTOCOL: "http", RADD_PRIVATE_PROXY_EXTERNAL_PORT: "80" },
    fetchImpl: async (url, options) => {
      capturedRequest = { url, options };
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        statusText: "OK",
        headers: { "content-type": "application/json" }
      });
    }
  });

  const response = await handler(buildEvent());

  assert.equal(response.statusCode, 200);
  assert.equal(
    capturedRequest.options.headers["x-pagopa-pn-base-url"],
    "http://vpce-0159b66963cb51025-1t51z9qh.vpce-svc-0a08e48564915d1c3.eu-south-1.vpce.amazonaws.com"
  );
});

test("logs full inbound payload only when verbose logging is enabled", async () => {
  const logs = [];
  const originalConsoleLog = console.log;
  console.log = (...args) => logs.push(args);

  try {
    const handler = createHandler({
      env: { ...baseEnv, RADD_PRIVATE_PROXY_VERBOSE_LOGGING: "true" },
      fetchImpl: async () => new Response(JSON.stringify({ ok: true }), {
        status: 200,
        statusText: "OK",
        headers: { "content-type": "application/json" }
      })
    });

    const response = await handler(buildEvent());

    assert.equal(response.statusCode, 200);
    assert.equal(
      logs.some(([message, payload]) =>
        message === "RADD private proxy inbound request payload" &&
        payload.body === JSON.stringify({ test: true }) &&
        payload.headers.host === "vpce-0159b66963cb51025-1t51z9qh.vpce-svc-0a08e48564915d1c3.eu-south-1.vpce.amazonaws.com:8080"
      ),
      true
    );
  } finally {
    console.log = originalConsoleLog;
  }
});

test("rejects paths outside the configured allowlist without forwarding", async () => {
  let called = false;
  const handler = createHandler({
    env: baseEnv,
    fetchImpl: async () => {
      called = true;
      return new Response("unexpected");
    }
  });

  const response = await handler(buildEvent({ path: "/radd-net/api/v1/download/ACT/123" }));

  assert.equal(response.statusCode, 403);
  assert.equal(called, false);
});

test("passes through backend application status codes", async () => {
  const handler = createHandler({
    env: baseEnv,
    fetchImpl: async () => new Response(JSON.stringify({ error: "bad request" }), {
      status: 400,
      statusText: "Bad Request",
      headers: { "content-type": "application/json" }
    })
  });

  const response = await handler(buildEvent());

  assert.equal(response.statusCode, 400);
  assert.deepEqual(JSON.parse(response.body), { error: "bad request" });
});
