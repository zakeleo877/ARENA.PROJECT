const fs = require('fs');
const path = require('path');
const { PGlite } = require('@electric-sql/pglite');
const schema = fs.readFileSync(path.join(__dirname, '..', 'schema.sql'), 'utf8');
const adminId = '00000000-0000-4000-8000-000000000001';
const otherId = '00000000-0000-4000-8000-000000000002';
const productId = '10000000-0000-4000-8000-000000000001';
const hiddenId = '10000000-0000-4000-8000-000000000002';
const orderId = '20000000-0000-4000-8000-000000000001';
let passed = 0;
function check(condition, message) { if (!condition) throw new Error('FAIL: ' + message); console.log('PASS:', message); passed++; }
async function expectError(action, expected, message) {
  try { await action(); throw new Error('Expected an error'); }
  catch (e) { if (!String(e.message).includes(expected)) throw new Error(message + ': expected ' + expected + ', got ' + e.message); console.log('PASS:', message); passed++; }
}
(async () => {
  const db = new PGlite();
  await db.exec(`
    create role anon nologin;
    create role authenticated nologin;
    create schema auth;
    create table auth.users (id uuid primary key, email text);
    create function auth.uid() returns uuid language sql stable as $$
      select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid
    $$;
    grant usage on schema auth to anon, authenticated;
    grant execute on function auth.uid() to anon, authenticated;
    create schema storage;
    create table storage.buckets (id text primary key, name text, public boolean, file_size_limit bigint, allowed_mime_types text[]);
    create table storage.objects (id uuid primary key default gen_random_uuid(), bucket_id text not null, name text not null);
    alter table storage.objects enable row level security;
    grant usage on schema storage to anon, authenticated;
    grant select, insert, update, delete on storage.objects to anon, authenticated;
    create publication supabase_realtime;
    insert into auth.users(id,email) values ('${adminId}','owner@example.invalid'), ('${otherId}','other@example.invalid');
  `);
  await db.exec(schema); console.log('PASS: schema applied once'); passed++;
  await db.exec("create policy legacy_insecure on public.orders for select to anon using (true); grant select on public.orders to anon;");
  await db.exec(schema); console.log('PASS: schema applied twice (idempotent)'); passed++;
  const rogue = await db.query("select count(*)::int as n from pg_policies where schemaname='public' and tablename='orders' and policyname='legacy_insecure'");
  check(rogue.rows[0].n === 0, 'unsafe legacy policy was removed on re-run');
  let before = await db.query('select count(*)::int as n from public.products');
  check(before.rows[0].n === 0, 'no sample products seeded');
  await db.exec(`insert into public.products(id,name,price,available) values
    ('${productId}','منتج اختبار',125.50,true), ('${hiddenId}','منتج مخفي',70,false)`);
  async function as(role, uid, fn) {
    await db.exec('reset role');
    await db.query("select set_config('request.jwt.claim.sub', $1, false)", [uid || '']);
    await db.exec('set role ' + role);
    try { return await fn(); } finally { await db.exec('reset role'); }
  }
  await as('anon', '', async () => {
    const visible = await db.query('select id from public.products');
    check(visible.rows.length === 1 && visible.rows[0].id === productId, 'anonymous sees only available products');
    check(!visible.rows.some(x => x.id === hiddenId), 'anonymous cannot read hidden products');
    await expectError(() => db.query('select * from public.orders'), 'permission denied', 'anonymous cannot read orders directly');
    await expectError(() => db.query("insert into public.orders(customer,phone) values ('a','1')"), 'permission denied', 'anonymous cannot insert orders directly');
    await expectError(() => db.query("insert into public.products(name) values ('unauthorized')"), 'permission denied', 'anonymous cannot insert products');
    await expectError(() => db.query('select public.claim_first_admin()'), 'permission denied', 'anonymous cannot claim admin');
  });
  await as('authenticated', adminId, async () => {
    const r = await db.query('select public.claim_first_admin() as claimed');
    check(r.rows[0].claimed === true, 'first authenticated user can claim admin');
    const again = await db.query('select public.claim_first_admin() as claimed');
    check(again.rows[0].claimed === false, 'admin claim cannot repeat');
    const visible = await db.query('select id from public.products');
    check(visible.rows.length === 2, 'admin can read all products');

    // The discount and rating columns are optional, and a card shows a badge
    // only when a real reduction exists, so the database has to refuse the
    // combinations that would let a card lie about the price.
    // 125.50 is the seeded price, so the old price has to be above it.
    await db.query("update public.products set original_price = 199.00, rating = 4.5 where id = '10000000-0000-4000-8000-000000000001'")
    const stored = await db.query("select original_price, rating from public.products where id = '10000000-0000-4000-8000-000000000001'");
    check(Number(stored.rows[0].original_price) === 199 && Number(stored.rows[0].rating) === 4.5,
      'a valid old price and rating are stored');
    await db.query("update public.products set original_price = null, rating = null where id = '10000000-0000-4000-8000-000000000001'");
    const cleared = await db.query("select original_price, rating from public.products where id = '10000000-0000-4000-8000-000000000001'");
    check(cleared.rows[0].original_price === null && cleared.rows[0].rating === null,
      'a missing discount and rating stay null rather than becoming zero');
    await expectError(
      () => db.query("update public.products set original_price = 1.00 where id = '10000000-0000-4000-8000-000000000001'"),
      'products_original_price_above_price',
      'an old price below the current price is rejected');
    await expectError(
      () => db.query("update public.products set rating = 7.5 where id = '10000000-0000-4000-8000-000000000001'"),
      'products_rating_range',
      'a rating above five is rejected');
  });
  await as('authenticated', otherId, async () => {
    const r = await db.query('select public.claim_first_admin() as claimed');
    check(r.rows[0].claimed === false, 'later non-admin cannot claim admin');
    const orders = await db.query('select id from public.orders');
    check(orders.rows.length === 0, 'non-admin cannot read orders');
    await expectError(() => db.query("insert into public.products(name) values ('unauthorized')"), 'row-level security', 'non-admin cannot insert products');
    const hiddenVisible = await db.query('select id from public.products where id = $1', [hiddenId]);
    check(hiddenVisible.rows.length === 0, 'non-admin cannot read hidden products');
  });
  const items = JSON.stringify([{product_id:productId, qty:2, name:'forged', price:0}]);
  const call = (phone, id, values=items) => db.query('select public.place_order($1,$2,$3,$4,$5::jsonb,$6::uuid) as n', ['اسم تجريبي',phone,'','',values,id]);
  await as('anon', '', async () => {
    const first = await call('0555555511', orderId);
    const retry = await call('0555555511', orderId);
    check(first.rows[0].n === retry.rows[0].n, 'same client ID returns same order number');
    await expectError(() => call('0555555522', '20000000-0000-4000-8000-000000000099', JSON.stringify([{product_id: hiddenId, qty:1}])), 'invalid_items', 'unavailable product is rejected by order RPC');
    await expectError(() => call('0555555522', '20000000-0000-4000-8000-000000000098', JSON.stringify([{product_id: productId, qty:0}])), 'invalid_items', 'zero quantity is rejected');
    await expectError(() => call('0555555522', '20000000-0000-4000-8000-000000000097', JSON.stringify([{product_id: productId, qty:100}])), 'invalid_items', 'quantity 100 is rejected');
    await expectError(() => call('0555555522', '20000000-0000-4000-8000-000000000096', JSON.stringify([{product_id:'bad-uuid',qty:1}])), 'invalid_items', 'malformed product ID is classified');
    await expectError(() => db.query('select public.place_order($1,$2,$3,$4,$5::jsonb,$6::uuid)', ['اسم تجريبي','0555555522','x'.repeat(501),'',items,'20000000-0000-4000-8000-000000000094']), 'invalid_notes', 'oversized notes are classified');
    await expectError(() => db.query('select public.place_order($1,$2,$3,$4,$5::jsonb,$6::uuid)', ['اسم تجريبي','0555555522','','x'.repeat(201),items,'20000000-0000-4000-8000-000000000093']), 'invalid_location', 'oversized location is classified');
    await expectError(() => call('++++++++', '20000000-0000-4000-8000-000000000095'), 'invalid_phone', 'phone must contain digits');
    for(let n=2;n<=5;n++) await call('0555555511', `20000000-0000-4000-8000-${String(n).padStart(12,'0')}`);
    await expectError(() => call('0555 555 511', '20000000-0000-4000-8000-000000000006'), 'rate_limited', 'sixth order is blocked despite formatting changes');
    const afterLimitRetry = await call('0555555511', orderId);
    check(afterLimitRetry.rows[0].n === first.rows[0].n, 'retry of original order still works after limit');
  });
  const totals = await db.query('select total::text as total, jsonb_array_length(details) as n from public.orders where client_id = $1', [orderId]);
  check(totals.rows.length === 1 && totals.rows[0].total === '251.00' && totals.rows[0].n === 1, 'server computes correct total and stores exactly one row');
  await db.exec("insert into storage.objects(bucket_id,name) values ('product-images','example.webp')");
  await as('authenticated', adminId, async () => {
    const rows = await db.query("select name from storage.objects where bucket_id = 'product-images'");
    check(rows.rows.length === 1, 'admin has SELECT policy required for removing images');
    const removed = await db.query("delete from storage.objects where bucket_id='product-images' returning id");
    check(removed.rows.length === 1, 'admin can delete images');
    const orders = await db.query('select id from public.orders');
    check(orders.rows.length === 5, 'admin can read orders');
    const manual = await db.query("insert into public.orders(customer,phone,channel) values ('اختبار','0555555599','يدوي') returning number");
    check(manual.rows.length === 1 && manual.rows[0].number != null, 'admin can insert a manual order with an identity number');
  });
  console.log(`\n${passed} local SQL checks passed. Supabase Auth/REST/Storage APIs were mocked, not live-tested.`);
  await db.close();
})().catch(e => { console.error(e.stack || e.message); process.exit(1); });
