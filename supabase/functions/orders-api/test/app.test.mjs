/**
 * The rules that have to be right here are narrow and testable without a
 * network: a private setting must never leave, and a database refusal must
 * reach the app as a name it can act on rather than as a wall of Postgres text.
 */
import assert from 'node:assert/strict'
import test from 'node:test'

import {
  PUBLIC_SETTING_KEYS,
  orderErrorName,
  publicSettingsQuery,
  toSettingsMap,
} from '../src/app.js'

test('only the four public settings are requested', () => {
  const { columns } = publicSettingsQuery()
  assert.equal(columns, 'key,value,is_public')
  assert.deepEqual(PUBLIC_SETTING_KEYS, ['storeName', 'whatsapp', 'currency', 'footerNote'])
})

test('the query is built against the client it is given, not a global', () => {
  const calls = []
  const fakeClient = {
    from: (table) => {
      calls.push(['from', table])
      return {
        select: (columns) => {
          calls.push(['select', columns])
          return {
            in: async (column, values) => {
              calls.push(['in', column, values])
              return { data: [], error: null }
            },
          }
        },
      }
    },
  }
  publicSettingsQuery().build(fakeClient)
  assert.deepEqual(calls, [
    ['from', 'settings'],
    ['select', 'key,value,is_public'],
    ['in', 'key', PUBLIC_SETTING_KEYS],
  ])
})

test('a row flagged private is dropped even if RLS let it through', () => {
  const map = toSettingsMap([
    { key: 'storeName', value: 'محلات السعيد', is_public: true },
    { key: 'whatsapp', value: '2137', is_public: true },
    // A misconfigured policy must not be enough on its own.
    { key: 'adminNote', value: 'سري', is_public: false },
  ])
  assert.deepEqual(map, { storeName: 'محلات السعيد', whatsapp: '2137' })
})

test('a settings row that is not a string is dropped, not stringified', () => {
  const map = toSettingsMap([
    { key: 'storeName', value: 'محلات السعيد', is_public: true },
    { key: 'broken', value: { nested: true }, is_public: true },
    { value: 'no key at all', is_public: true },
    null,
  ])
  assert.deepEqual(map, { storeName: 'محلات السعيد' })
})

test('an empty answer is an empty map, not a crash', () => {
  assert.deepEqual(toSettingsMap(null), {})
  assert.deepEqual(toSettingsMap([]), {})
})

test('a known database refusal keeps its name', () => {
  assert.equal(orderErrorName('invalid_phone'), 'invalid_phone')
  assert.equal(orderErrorName('too_many_orders'), 'too_many_orders')
  // The message PostgREST actually produces carries the name first.
  assert.equal(
    orderErrorName('product_unavailable: one of the products is no longer available'),
    'product_unavailable',
  )
})

test('an unknown refusal never leaks the server text', () => {
  const leaked = 'duplicate key value violates unique constraint "orders_phone_key" (phone=0555...)'
  const name = orderErrorName(leaked)
  assert.equal(name, 'order_failed')
  assert.ok(!name.includes('0555'))
  assert.equal(orderErrorName(''), 'order_failed')
  assert.equal(orderErrorName(undefined), 'order_failed')
})
