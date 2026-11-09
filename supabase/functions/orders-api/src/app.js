/**
 * The shop's order endpoint.
 *
 * Why this exists: the customer app held the WhatsApp number as a compiled-in
 * constant and sent orders straight to the `orders` table with the publishable
 * key. Both are wrong. The number is the owner's own contact detail, so changing
 * it needed a new APK; and letting an anonymous client insert rows puts the whole
 * anti-abuse design on one database function with no way to see, per caller, who
 * is hammering it.
 *
 * With this endpoint the app asks for the number at checkout, and the order is
 * placed here with the secret key. The app no longer writes to `orders` at all.
 *
 * `auth: 'publishable'` is the right gate: the caller is not a user (the shop has
 * no customer accounts), but the request must carry this project's publishable
 * key, so a random script cannot drive the endpoint. RLS stays the source of
 * truth; the secret key is used only where the database deliberately grants
 * nothing to `anon`.
 */
import { withSupabase } from '@supabase/server'

/** The only settings a checkout screen has any use for. */
export const PUBLIC_SETTING_KEYS = ['storeName', 'whatsapp', 'currency', 'footerNote']

/**
 * The settings query, as data rather than as a sentence inside the handler.
 *
 * A whitelist, not a "not in" list: a newly added private row stays invisible by
 * default instead of being exposed by forgetfulness.
 */
export function publicSettingsQuery() {
  return {
    columns: 'key,value,is_public',
    build: (client) =>
      client.from('settings').select('key,value,is_public').in('key', PUBLIC_SETTING_KEYS),
  }
}

/**
 * Turns rows into the map the app reads.
 *
 * RLS already hides private rows from an anonymous client, but the `is_public`
 * flag is checked here too: two independent reasons for a private value to stay
 * private is the correct number.
 */
export function toSettingsMap(rows) {
  const map = {}
  for (const row of rows ?? []) {
    if (row?.is_public === false) continue
    // A row that is not a string is dropped rather than turned into
    // "[object Object]" on the checkout screen.
    if (typeof row?.key === 'string' && typeof row?.value === 'string') {
      map[row.key] = row.value
    }
  }
  return map
}

/**
 * The database raises a named exception per rule (`invalid_phone`,
 * `too_many_orders`, ...). That name is the only thing the app acts on, so it is
 * the only thing passed back: a Postgres detail string can carry row data.
 */
export function orderErrorName(message) {
  const named = String(message ?? '').match(/\b([a-z][a-z_]{2,39})\b/)
  const known = [
    'invalid_customer', 'invalid_phone', 'invalid_notes', 'invalid_location',
    'invalid_items', 'too_many_orders', 'empty_items', 'product_unavailable',
    'invalid_quantity',
  ]
  return known.includes(named?.[1]) ? named[1] : 'order_failed'
}

export default {
  fetch: withSupabase({ auth: 'publishable' }, async (request, ctx) => {
    const url = new URL(request.url)
    const action = url.pathname.split('/').filter(Boolean).pop() ?? ''

    // GET .../orders-api/settings -- what the checkout screen shows.
    if (request.method === 'GET' && action === 'settings') {
      const { build } = publicSettingsQuery()
      const { data, error } = await build(ctx.supabase)
      if (error) {
        return Response.json({ error: 'settings_unavailable' }, { status: 502 })
      }
      return Response.json({ settings: toSettingsMap(data) })
    }

    // POST .../orders-api/order -- place one order.
    if (request.method === 'POST' && action === 'order') {
      let body
      try {
        body = await request.json()
      } catch {
        return Response.json({ error: 'invalid_json' }, { status: 400 })
      }
      if (!body || typeof body !== 'object' || Array.isArray(body)) {
        return Response.json({ error: 'invalid_body' }, { status: 400 })
      }
      // The database validates every field; this only forwards. Re-validating
      // here would drift from the check that actually guards the table.
      const { data, error } = await ctx.supabaseAdmin.rpc('place_order', {
        p_customer: body.customer ?? null,
        p_phone: body.phone ?? null,
        p_notes: body.notes ?? '',
        p_location: body.location ?? '',
        p_items: body.items ?? [],
        p_client_id: body.clientId ?? null,
      })
      if (error) {
        return Response.json({ error: orderErrorName(error.message) }, { status: 409 })
      }
      return Response.json({ number: data })
    }

    return Response.json({ error: 'not_found' }, { status: 404 })
  }),
}
