#!/usr/bin/env node
/**
 * Measures the queries the two applications actually send, on a real
 * PostgreSQL, with the schema in `supabase/schema.sql`.
 *
 * The numbers in `docs/performance.md` come from this script. Running it again
 * after a schema change is how a regression in a plan is caught.
 *
 * Usage: node scripts/measure-performance.cjs [orders] [products]
 */
const fs = require('fs');
const path = require('path');

const ORDERS = Number(process.argv[2] || 200000);
const PRODUCTS = Number(process.argv[3] || 5000);

const tests = path.join(__dirname, '..', 'supabase', 'tests');
const { PGlite } = require(path.join(tests, 'node_modules', '@electric-sql/pglite'));

const schema = fs.readFileSync(path.join(__dirname, '..', 'supabase', 'schema.sql'), 'utf8');

async function main() {
  const db = await new PGlite();

  // The parts of a Supabase project that the schema depends on.
  await db.exec(`
    create schema auth;
    create table auth.users (id uuid primary key);
    create function auth.uid() returns uuid language sql stable as $$ select null::uuid $$;
    create role anon nologin;
    create role authenticated nologin;
    grant usage on schema auth to anon, authenticated;
    grant execute on function auth.uid() to anon, authenticated;
  `);

  await db.exec(schema);

  process.stdout.write(`seeding ${ORDERS} orders and ${PRODUCTS} products ...\n`);
  await db.exec(`
    insert into public.orders (customer, phone, status, details, total, created_at)
    select 'customer ' || g,
           '05' || lpad((g % 20000)::text, 8, '0'),
           (case
              when g % 100 < 70 then 'مُسلَّم'
              when g % 100 < 85 then 'جاهز'
              when g % 100 < 93 then 'قيد التحضير'
              when g % 100 < 99 then 'جديد'
              else 'ملغي'
            end),
           '[]'::jsonb, 100,
           now() - ((g % 57600) || ' seconds')::interval
    from generate_series(1, ${ORDERS}) g;

    insert into public.products (name, price, available, category, sort_order)
    select 'p' || g, 10, (g % 10 <> 0), 'cat' || (g % 20), g
    from generate_series(1, ${PRODUCTS}) g;
  `);
  await db.exec('analyze');

  const counts = await db.query(`
    select (select count(*)::int from public.orders) o,
           (select count(*)::int from public.orders
             where created_at > now() - interval '1 hour') h,
           (select count(*)::int from public.products) p
  `);
  const { o, h, p } = counts.rows[0];
  process.stdout.write(`orders=${o} orders_last_hour=${h} products=${p}\n\n`);

  const measure = async (label, sql, params) => {
    const plan = (await db.query(`explain (analyze) ${sql}`, params))
      .rows.map((row) => row['QUERY PLAN'])
      .join('\n');
    const time = Number((plan.match(/Execution Time: ([\d.]+) ms/) || [])[1] || NaN);
    const usedIndex = /Index Scan|Bitmap Index Scan/.test(plan);
    const sorted = /Sort Method|Sort Key/.test(plan);
    process.stdout.write(
      `${label.padEnd(46)} ${String(time.toFixed(3)).padStart(9)} ms   ` +
        `${usedIndex ? 'index' : 'sequential'}${sorted ? ' + sort' : ''}\n`,
    );
    return { time, plan };
  };

  process.stdout.write('== the rate limit inside place_order ==\n');
  await measure(
    'orders for one phone in the last hour',
    `select count(*) from public.orders
     where regexp_replace(phone, '[^0-9]', '', 'g') = $1
       and created_at > now() - interval '1 hour'`,
    ['0555000123'],
  );

  process.stdout.write('\n== the management order list ==\n');
  await measure(
    'newest first',
    'select id, number from public.orders order by created_at desc limit 100',
  );
  await measure(
    'one status, newest first (common)',
    "select id, number from public.orders where status = 'جديد' order by created_at desc limit 100",
  );
  await measure(
    'one status, newest first (rare)',
    "select id, number from public.orders where status = 'ملغي' order by created_at desc limit 100",
  );

  process.stdout.write('\n== the customer catalog ==\n');
  await measure(
    'available products in sort order',
    'select id, name from public.products where available order by sort_order asc, created_at desc limit 5000',
  );
  await measure(
    'the public settings',
    'select key, value from public.settings where is_public order by key',
  );

  // The value of the status index is only visible without it, so the same
  // query is measured twice in one run: once with the index and once without.
  process.stdout.write('\n== the same query, with and without the status index ==\n');
  const rareSql =
    "select id, number from public.orders where status = 'ملغي' order by created_at desc limit 100";
  await measure('a rare status WITH the index', rareSql);
  await db.exec('drop index if exists public.orders_status_created_at_idx');
  await db.exec('analyze public.orders');
  await measure('a rare status WITHOUT the index', rareSql);
  await db.exec(
    'create index if not exists orders_status_created_at_idx ' +
      'on public.orders (status, created_at desc)',
  );
  await db.exec('analyze public.orders');

  process.stdout.write('\n== the worst case, when no index can be used at all ==\n');
  await db.exec('set enable_indexscan = off; set enable_bitmapscan = off;');
  await measure(
    'status filter with no index usable',
    "select id from public.orders where status = 'ملغي' order by created_at desc limit 100",
  );
  await db.exec('set enable_indexscan = on; set enable_bitmapscan = on;');

  await db.close();
  process.stdout.write('\ndone.\n');
}

main().catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exit(1);
});
