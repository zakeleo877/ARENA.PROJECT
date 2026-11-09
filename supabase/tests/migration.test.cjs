// Exercise the additive migration on an isolated old-format PostgreSQL table.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { PGlite } = require('@electric-sql/pglite');

const sql = fs.readFileSync(path.join(__dirname, '..', 'migrations',
  '20261109060816_add_optional_catalog_display_fields.sql'), 'utf8');

test('catalog upgrade keeps existing data, is repeatable and guards optional fields', async () => {
  const db = new PGlite();
  try {
    await db.exec(`CREATE TABLE public.products (
      id integer PRIMARY KEY, name text NOT NULL, price numeric(12,2) NOT NULL
    ); INSERT INTO public.products VALUES (1, 'existing product', 1200.50);`);
    await db.exec(sql);
    await db.exec(sql);
    const { rows } = await db.query('SELECT id, name, price::text, original_price, rating FROM public.products');
    assert.deepEqual(rows, [{ id: 1, name: 'existing product', price: '1200.50', original_price: null, rating: null }]);
    await assert.rejects(db.exec('UPDATE public.products SET original_price = 1000 WHERE id = 1'));
    await assert.rejects(db.exec('UPDATE public.products SET rating = 5.1 WHERE id = 1'));
    await db.exec('UPDATE public.products SET original_price = 1400, rating = 4.5 WHERE id = 1');
    const { rows: after } = await db.query('SELECT original_price::text, rating::text FROM public.products');
    assert.deepEqual(after, [{ original_price: '1400.00', rating: '4.5' }]);
  } finally {
    await db.close();
  }
});
