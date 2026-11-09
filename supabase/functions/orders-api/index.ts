// Optional public facade over the same RLS-protected PostgREST endpoints used
// by the Android app. It never reads a service/secret key. A publishable key is
// public information, not an abuse barrier: the database validates every order
// and applies its own rate limit and permissions.

const PUBLIC_SETTING_KEYS = new Set(['storeName', 'whatsapp', 'currency', 'footerNote'])
const ORDER_ERRORS = new Set([
  'invalid_customer', 'invalid_phone', 'invalid_notes', 'invalid_location',
  'invalid_items', 'rate_limited',
])
const MAX_BODY_BYTES = 32 * 1024
const CLIENT_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const RESPONSE_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'apikey, content-type',
  'Cache-Control': 'no-store',
  'X-Content-Type-Options': 'nosniff',
}

type Options = { baseUrl: string; fetcher?: typeof fetch }

function respond(data: unknown, status = 200): Response {
  return Response.json(data, { status, headers: RESPONSE_HEADERS })
}

/** Never return database details, which could contain customer information. */
export function orderErrorName(message: unknown): string {
  if (typeof message !== 'string') return 'order_failed'
  const name = message.split(':', 1)[0].trim()
  return ORDER_ERRORS.has(name) ? name : 'order_failed'
}

/** Keep only explicitly public settings from the small allowlist. */
export function publicSettings(rows: unknown): Record<string, string> {
  const result: Record<string, string> = {}
  if (!Array.isArray(rows)) return result
  for (const row of rows) {
    if (row && row.is_public === true && typeof row.key === 'string' &&
        PUBLIC_SETTING_KEYS.has(row.key) && typeof row.value === 'string') {
      result[row.key] = row.value
    }
  }
  return result
}

/** The fetcher is injectable for tests; the deployed function uses global fetch. */
export async function handleRequest(request: Request, options: Options): Promise<Response> {
  if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: RESPONSE_HEADERS })
  const key = request.headers.get('apikey') ?? ''
  if (!key.startsWith('sb_publishable_')) return respond({ error: 'publishable_key_required' }, 401)
  if (!options.baseUrl.startsWith('https://')) return respond({ error: 'server_unavailable' }, 503)
  const segments = new URL(request.url).pathname.split('/').filter(Boolean)
  const action = segments.at(-1)
  if (segments.at(-2) !== 'orders-api') return respond({ error: 'not_found' }, 404)
  const fetcher = options.fetcher ?? fetch
  // Do not forward Authorization: this is an anonymous endpoint, not an admin
  // proxy. The supplied public key is checked by the upstream project.
  const headers = { apikey: key, 'Content-Type': 'application/json' }

  if (request.method === 'GET' && action === 'settings') {
    const target = new URL('/rest/v1/settings', options.baseUrl)
    target.searchParams.set('select', 'key,value,is_public')
    target.searchParams.set('is_public', 'eq.true')
    target.searchParams.set('key', 'in.(storeName,whatsapp,currency,footerNote)')
    try {
      const upstream = await fetcher(target, { headers })
      if (upstream.status === 401 || upstream.status === 403) return respond({ error: 'unauthorized' }, 401)
      if (!upstream.ok) return respond({ error: 'settings_unavailable' }, 502)
      const rows: unknown = await upstream.json()
      if (!Array.isArray(rows)) return respond({ error: 'settings_unavailable' }, 502)
      return respond({ settings: publicSettings(rows) })
    } catch (_) {
      return respond({ error: 'settings_unavailable' }, 503)
    }
  }

  if (request.method === 'POST' && action === 'order') {
    const announced = Number(request.headers.get('content-length') ?? '0')
    if (announced > MAX_BODY_BYTES) return respond({ error: 'body_too_large' }, 413)
    let body: any
    try {
      const text = await request.text()
      if (new TextEncoder().encode(text).byteLength > MAX_BODY_BYTES) return respond({ error: 'body_too_large' }, 413)
      body = JSON.parse(text)
    } catch (_) {
      return respond({ error: 'invalid_json' }, 400)
    }
    if (!body || typeof body !== 'object' || Array.isArray(body)) return respond({ error: 'invalid_body' }, 400)
    // A stable ID is required so a lost response can be retried without
    // creating a duplicate order. All other validation belongs to place_order.
    if (typeof body.clientId !== 'string' || !CLIENT_ID.test(body.clientId)) {
      return respond({ error: 'invalid_client_id' }, 400)
    }
    const payload = {
      p_customer: body.customer ?? null,
      p_phone: body.phone ?? null,
      p_notes: body.notes ?? '',
      p_location: body.location ?? '',
      p_items: body.items ?? [],
      p_client_id: body.clientId,
    }
    try {
      const upstream = await fetcher(new URL('/rest/v1/rpc/place_order', options.baseUrl), {
        method: 'POST', headers, body: JSON.stringify(payload),
      })
      if (upstream.status === 401 || upstream.status === 403) return respond({ error: 'unauthorized' }, 401)
      const data: any = await upstream.json().catch(() => null)
      if (!upstream.ok) {
        const error = orderErrorName(data?.message)
        return respond({ error }, error === 'order_failed' ? 502 : 409)
      }
      if (typeof data !== 'number' || !Number.isSafeInteger(data) || data < 1) {
        return respond({ error: 'order_failed' }, 502)
      }
      return respond({ number: data })
    } catch (_) {
      return respond({ error: 'order_unavailable' }, 503)
    }
  }
  return respond({ error: 'not_found' }, 404)
}

// Supabase supplies the project URL. No elevated credential is requested.
if (typeof Deno !== 'undefined') {
  const baseUrl = Deno.env.get('SUPABASE_URL') ?? ''
  Deno.serve((request: Request) => handleRequest(request, { baseUrl }))
}
