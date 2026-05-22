const assert = require("node:assert/strict");
const { test } = require("node:test");
const {
  buildAlbResponse,
  buildForwardHeaders,
  buildQueryString,
  buildRequestBody,
  collectHeaders,
  filterResponseHeaders,
  isTextualResponse
} = require("../app/http");

test("collectHeaders merges headers and gives precedence to multiValueHeaders", () => {
  const headers = collectHeaders({
    headers: {
      Host: "example.com",
      "X-Test": "single"
    },
    multiValueHeaders: {
      "X-Test": ["first", "second"],
      "X-Multi": ["a", "b"]
    }
  });

  assert.deepEqual(headers, {
    host: "example.com",
    "x-test": "first,second",
    "x-multi": "a,b"
  });
});

test("collectHeaders joins multi-value cookies with cookie separator", () => {
  const headers = collectHeaders({
    multiValueHeaders: {
      Cookie: ["a=1", "b=2"]
    }
  });

  assert.deepEqual(headers, {
    cookie: "a=1; b=2"
  });
});

test("buildForwardHeaders removes hop-by-hop and spoofed trusted headers", () => {
  const forwardHeaders = buildForwardHeaders(
    {
      host: "vpce.example:8080",
      connection: "keep-alive",
      "content-length": "123",
      "x-pagopa-pn-src-ch": "SPOOFED",
      "x-extra-header": "extra"
    },
    {
      trustedHeaders: {
        "x-pagopa-pn-src-ch": "RADD",
        uid: "RADD_cf_97103880585"
      }
    },
    "https://vpce.example:8443"
  );

  assert.deepEqual(forwardHeaders, {
    "x-extra-header": "extra",
    "x-pagopa-pn-src-ch": "RADD",
    uid: "RADD_cf_97103880585",
    "x-pagopa-pn-base-url": "https://vpce.example:8443"
  });
});

test("buildQueryString serializes multi-value query string parameters", () => {
  const queryString = buildQueryString({
    multiValueQueryStringParameters: {
      a: ["1", "2"],
      b: ["3"]
    }
  });

  assert.equal(queryString, "?a=1&a=2&b=3");
});

test("buildQueryString preserves already encoded query parameter values", () => {
  const queryString = buildQueryString({
    multiValueQueryStringParameters: {
      fileKey: ["a%2Fb"],
      q: ["hello%20world"]
    }
  });

  assert.equal(queryString, "?fileKey=a%2Fb&q=hello%20world");
});

test("buildRequestBody returns undefined for GET and decodes base64 bodies", () => {
  assert.equal(
    buildRequestBody({ body: "ignored", isBase64Encoded: false }, "GET"),
    undefined
  );

  const binaryBody = buildRequestBody({ body: Buffer.from("hello").toString("base64"), isBase64Encoded: true }, "POST");
  assert.equal(Buffer.isBuffer(binaryBody), true);
  assert.equal(binaryBody.toString("utf8"), "hello");
});

test("filterResponseHeaders removes non-forwardable headers", () => {
  const responseHeaders = new Headers({
    connection: "keep-alive",
    "content-type": "application/json",
    "x-custom": "ok"
  });

  assert.deepEqual(filterResponseHeaders(responseHeaders), {
    "content-type": ["application/json"],
    "x-custom": ["ok"]
  });
});

test("filterResponseHeaders preserves multiple set-cookie headers", () => {
  const responseHeaders = new Headers({
    "content-type": "application/json"
  });
  responseHeaders.getSetCookie = () => ["a=1; Secure", "b=2; Secure"];

  assert.deepEqual(filterResponseHeaders(responseHeaders), {
    "content-type": ["application/json"],
    "set-cookie": ["a=1; Secure", "b=2; Secure"]
  });
});

test("buildAlbResponse and isTextualResponse follow ALB response expectations", () => {
  assert.equal(isTextualResponse("application/json; charset=utf-8"), true);
  assert.equal(isTextualResponse("application/octet-stream"), false);

  assert.deepEqual(
    buildAlbResponse(200, "body", { "content-type": "application/json" }, false, "OK"),
    {
      statusCode: 200,
      statusDescription: "200 OK",
      isBase64Encoded: false,
      multiValueHeaders: { "content-type": ["application/json"] },
      body: "body"
    }
  );
});
