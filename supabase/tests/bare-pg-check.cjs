const { PGlite } = require('@electric-sql/pglite');
const fs = require('fs');
(async () => {
  const db = await new PGlite();
  await db.exec(`
    create schema auth;
    create table auth.users (id uuid primary key);
    create function auth.uid() returns uuid language sql stable as $$ select null::uuid $$;
    create role anon nologin;
    create role authenticated nologin;
    grant usage on schema auth to anon, authenticated;
    grant execute on function auth.uid() to anon, authenticated;
  `);
  const schema = fs.readFileSync(require('path').join(__dirname,'..','schema.sql'), 'utf8');
  try {
    await db.exec(schema);
    console.log('SCHEMA: applied on a bare cluster');
  } catch (e) {
    console.log('SCHEMA FAILED:', e.message);
    process.exit(1);
  }
  const s = await db.query("select key,value from public.settings order by key");
  console.log('settings rows =', s.rows.length, JSON.stringify(s.rows));
  const t = await db.query("select count(*)::int n from pg_tables where schemaname='public'");
  console.log('public tables =', t.rows[0].n);
  const pol = await db.query("select count(*)::int n from pg_policies where schemaname='public'");
  console.log('public policies =', pol.rows[0].n);
  await db.close();
})();
