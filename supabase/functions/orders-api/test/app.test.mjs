import assert from 'node:assert/strict'
import test from 'node:test'
import { handleRequest, orderErrorName, publicSettings } from '../index.ts'

// Synthetic keys exist only in this isolated test; never use a real credential.
const key = 'sb_' + 'publishable_' + 'test_for_endpoint'
const baseUrl = 'https://project.example.invalid'
const clientId = 'abababab-1234-4567-8901-123456789012'
const endpoint = 'https://edge.example.invalid/functions/v1/orders-api'
const request = (action, init = {}) => new Request(`${endpoint}/${action}`, {
  ...init, headers: { apikey: key, ...init.headers },
})
const upstreamJson = (body, status = 200) => Response.json(body, { status })

function withFetcher(fetcher) { return { baseUrl, fetcher } }

test('only allowed public string settings leave the function', () => {
  assert.deepEqual(publicSettings([
    { key: 'storeName', value: 'متجر', is_public: true },
    { key: 'whatsapp', value: '2137', is_public: false },
    { key: 'unknown', value: 'hidden', is_public: true },
    { key: 'currency', value: { unexpected: true }, is_public: true },
    { key: 'footerNote', value: 'hidden', is_public: null },
  ]), { storeName: 'متجر' })
})

test('settings request uses anonymous key, RLS, and a public allowlist', async () => {
  let called = false
  const response = await handleRequest(request('settings', { headers: { Authorization: 'Bearer ignored' } }), withFetcher(async (url, init) => {
    called = true
    assert.equal(url.origin, baseUrl)
    assert.equal(url.pathname, '/rest/v1/settings')
    assert.equal(url.searchParams.get('is_public'), 'eq.true')
    assert.equal(init.headers.apikey, key)
    assert.equal(init.headers.Authorization, undefined)
    return upstreamJson([
      { key: 'storeName', value: 'متجر', is_public: true },
      { key: 'private', value: 'do not return', is_public: false },
    ])
  }))
  assert.ok(called)
  assert.equal(response.status, 200)
  assert.deepEqual(await response.json(), { settings: { storeName: 'متجر' } })
})

test('orders call the database RPC with a stable client ID and no client price', async () => {
  const order = { customer: 'زبون', phone: '0555000000', items: [{ product_id: 'id', qty: 2 }], clientId }
  const response = await handleRequest(request('order', { method: 'POST', body: JSON.stringify(order) }), withFetcher(async (url, init) => {
    assert.equal(url.pathname, '/rest/v1/rpc/place_order')
    assert.equal(init.method, 'POST')
    assert.deepEqual(JSON.parse(init.body), {
      p_customer: order.customer, p_phone: order.phone, p_notes: '', p_location: '',
      p_items: order.items, p_client_id: clientId,
    })
    return upstreamJson(12)
  }))
  assert.equal(response.status, 200)
  assert.deepEqual(await response.json(), { number: 12 })
})

test('the key, JSON, client ID, size and route are checked before forwarding', async () => {
  let requests = 0
  const opts = withFetcher(async () => { requests++; return upstreamJson({}) })
  assert.equal((await handleRequest(new Request(`${endpoint}/settings`), opts)).status, 401)
  assert.equal((await handleRequest(request('order', { method: 'POST', body: '{' }), opts)).status, 400)
  assert.equal((await handleRequest(request('order', { method: 'POST', body: '{}' }), opts)).status, 400)
  assert.equal((await handleRequest(request('order', { method: 'POST', headers: { 'Content-Length': '40000' }, body: '{}' }), opts)).status, 413)
  assert.equal((await handleRequest(request('other'), opts)).status, 404)
  assert.equal(requests, 0)
})

test('known errors are classified but private server messages are redacted', async () => {
  assert.equal(orderErrorName('rate_limited'), 'rate_limited')
  assert.equal(orderErrorName('invalid_phone: invalid value'), 'invalid_phone')
  assert.equal(orderErrorName('duplicate key (phone=0555...)'), 'order_failed')
  const order = JSON.stringify({ clientId })
  const known = await handleRequest(request('order', { method: 'POST', body: order }), withFetcher(async () => upstreamJson({ message: 'invalid_phone' }, 400)))
  assert.equal(known.status, 409)
  assert.deepEqual(await known.json(), { error: 'invalid_phone' })
  const unknown = await handleRequest(request('order', { method: 'POST', body: order }), withFetcher(async () => upstreamJson({ message: 'private phone 0555...' }, 500)))
  assert.equal(unknown.status, 502)
  assert.deepEqual(await unknown.json(), { error: 'order_failed' })
})

test('failed upstream access and unexpected results are handled safely', async () => {
  const order = JSON.stringify({ clientId })
  const denied = await handleRequest(request('settings'), withFetcher(async () => upstreamJson({}, 401)))
  assert.equal(denied.status, 401)
  const malformed = await handleRequest(request('order', { method: 'POST', body: order }), withFetcher(async () => upstreamJson({ unexpected: 'result' })))
  assert.equal(malformed.status, 502)
  const offline = await handleRequest(request('settings'), withFetcher(async () => { throw new Error('network details') }))
  assert.deepEqual(await offline.json(), { error: 'settings_unavailable' })
})

test('browser preflight has CORS headers without requiring a key', async () => {
  const res = await handleRequest(new Request(`${endpoint}/order`, { method: 'OPTIONS' }), withFetcher(async () => { throw new Error('unexpected') }))
  assert.equal(res.status, 204)
  assert.equal(res.headers.get('access-control-allow-origin'), '*')
})
