# Optional orders API

The Android clients currently use PostgREST directly; this endpoint is optional.
It forwards requests with the caller's **publishable** key and never reads or
uses an elevated key. The key is public, so database RLS and `place_order` are
still the security and validation boundaries.

- `GET /functions/v1/orders-api/settings` returns only the four public settings.
- `POST /functions/v1/orders-api/order` accepts `customer`, `phone`, `notes`,
  `location`, `items` (`product_id` and `qty`), and a stable UUID `clientId`.
  It returns `{ "number": 123 }` or a sanitized error. The database calculates
  prices and prevents duplicate retries.
- `OPTIONS` supports a browser preflight.

Gateway JWT verification is disabled because the endpoint accepts a publishable
key, not a user JWT. That is not an authorization bypass: the upstream project
still validates the key and enforces the anonymous role's policies. This is
not an additional anti-abuse layer. Don't log request bodies or credentials.

Run `npm ci && npm test` with Node.js 22.18 or later. Tests use mock HTTP
responses and do not write to a real store. The CI read-only smoke check, when
available, compares the live endpoint's public settings with direct PostgREST;
it does not create an order.
