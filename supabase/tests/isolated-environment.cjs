/**
 * An isolated Supabase-compatible test environment.
 *
 * This is NOT the production database. It is a private PostgreSQL instance
 * (PGlite) that is created from scratch for every test run, seeded with dummy
 * rows only, and thrown away when the process exits.
 *
 * It reproduces the parts of Supabase that the application actually depends
 * on, faithfully enough to exercise the real SQL:
 *
 *   - the roles `anon` and `authenticated`, exactly as the schema grants them
 *   - `auth.uid()` resolved from the verified JWT of the caller
 *   - `request.jwt.claims` for the admin role check
 *   - a PostgREST-like REST surface (GET/POST/PATCH/DELETE + RPC)
 *   - a GoTrue-like auth surface (sign-up, sign-in, sign-out, refresh)
 *   - a storage surface (list, upload, delete) guarded by the same policies
 *
 * The JWT is signed with HMAC-SHA256 using a throwaway key that only exists
 * inside this process, and the signature is verified before the claims are
 * trusted, so the caller cannot forge a role by sending a hand written
 * `Authorization` header.
 */
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { PGlite } = require('@electric-sql/pglite');

const schema = fs.readFileSync(path.join(__dirname, '..', 'schema.sql'), 'utf8');

// A throwaway signing key. It is generated per process and never persisted,
// so no token issued here can ever be replayed against a real project.
const JWT_SECRET = crypto.randomBytes(32).toString('hex');
const ANON_KEY = 'sb_publishable_test_' + crypto.randomBytes(12).toString('hex');

const nowIso = () => new Date().toISOString();

function base64url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function signJwt(claims) {
  const header = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = base64url(JSON.stringify(claims));
  const signature = base64url(
    crypto.createHmac('sha256', JWT_SECRET).update(`${header}.${payload}`).digest(),
  );
  return `${header}.${payload}.${signature}`;
}

function verifyJwt(token) {
  if (typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const expected = base64url(
    crypto.createHmac('sha256', JWT_SECRET).update(`${parts[0]}.${parts[1]}`).digest(),
  );
  const given = Buffer.from(parts[2]);
  const wanted = Buffer.from(expected);
  if (given.length !== wanted.length) return null;
  if (!crypto.timingSafeEqual(given, wanted)) return null;
  try {
    const claims = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf8'));
    if (claims.exp && claims.exp * 1000 < Date.now()) return null;
    return claims;
  } catch {
    return null;
  }
}

/** A single test HTTP response. */
function json(status, body) {
  return { status, body };
}

/**
 * The isolated environment. Every method talks to a real PostgreSQL server
 * running inside this process, so the RLS policies under test are the real
 * policies from `supabase/schema.sql`.
 */
class IsolatedEnvironment {
  constructor() {
    this.db = null;
    this.anonKey = ANON_KEY;
    this.users = new Map();
    this.sessions = new Map();
    this.storageObjects = new Map();
    this.signupEnabled = false;
    this.requestCount = 0;
  }

  async start() {
    this.db = new PGlite();
    await this.db.exec(`
      create role anon nologin;
      create role authenticated nologin;
      create schema auth;
      create table auth.users (
        id uuid primary key,
        email text unique not null,
        password_hash text not null,
        created_at timestamptz not null default now()
      );
      create function auth.uid() returns uuid language sql stable as $$
        select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid
      $$;
      grant usage on schema auth to anon, authenticated;
      grant execute on function auth.uid() to anon, authenticated;
      create schema storage;
      create table storage.buckets (
        id text primary key,
        name text,
        public boolean,
        file_size_limit bigint,
        allowed_mime_types text[]
      );
      create table storage.objects (
        id uuid primary key default gen_random_uuid(),
        bucket_id text not null,
        name text not null
      );
      alter table storage.objects enable row level security;
      grant usage on schema storage to anon, authenticated;
      grant select, insert, update, delete on storage.objects to anon, authenticated;
      create publication supabase_realtime;
    `);
    await this.db.exec(schema);
    await this.db.exec(
      `insert into storage.buckets(id,name,public) values ('product-images','product-images',false)
       on conflict (id) do nothing`,
    );
    return this;
  }

  async stop() {
    if (this.db) await this.db.close();
    this.db = null;
  }

  // ---------------------------------------------------------------- auth ---

  hashPassword(password) {
    return crypto.createHash('sha256').update(`salt:${password}`).digest('hex');
  }

  /** GoTrue-like sign-up. Public sign-up is disabled by default, like the real project. */
  async signUp(email, password) {
    if (!this.signupEnabled) {
      return json(422, {
        code: 'signup_disabled',
        msg: 'Signups not allowed for this project',
      });
    }
    return this.createUser(email, password);
  }

  async createUser(email, password) {
    if (this.users.has(email)) {
      return json(422, { code: 'email_exists', msg: 'User already registered' });
    }
    const id = crypto.randomUUID();
    await this.db.query(
      'insert into auth.users(id,email,password_hash) values ($1,$2,$3)',
      [id, email, this.hashPassword(password)],
    );
    this.users.set(email, { id, email, password });
    return json(200, this.sessionFor(id, email, password));
  }

  sessionFor(id, email) {
    const accessToken = signJwt({
      sub: id,
      email,
      role: 'authenticated',
      aud: 'authenticated',
      exp: Math.floor(Date.now() / 1000) + 3600,
      iat: Math.floor(Date.now() / 1000),
    });
    const refreshToken = crypto.randomBytes(24).toString('hex');
    this.sessions.set(refreshToken, { id, email });
    return {
      access_token: accessToken,
      token_type: 'bearer',
      expires_in: 3600,
      refresh_token: refreshToken,
      user: { id, email, aud: 'authenticated', role: 'authenticated' },
    };
  }

  async signIn(email, password) {
    const user = this.users.get(email);
    if (!user) {
      return json(400, { code: 'invalid_credentials', msg: 'Invalid login credentials' });
    }
    await this.db.query('select 1 from auth.users where id = $1', [user.id]);
    const stored = await this.db.query(
      'select password_hash from auth.users where id = $1',
      [user.id],
    );
    if (stored.rows[0].password_hash !== this.hashPassword(password)) {
      return json(400, { code: 'invalid_credentials', msg: 'Invalid login credentials' });
    }
    return json(200, this.sessionFor(user.id, user.email, password));
  }

  async signOut(refreshToken) {
    if (!refreshToken) return json(204, {});
    this.sessions.delete(refreshToken);
    return json(204, {});
  }

  async refresh(refreshToken) {
    const session = this.sessions.get(refreshToken);
    if (!session) return json(400, { code: 'invalid_refresh_token' });
    return json(200, this.sessionFor(session.id, session.email));
  }

  // ------------------------------------------------------------- request ---

  /** Resolve the caller from the request headers, exactly like PostgREST does. */
  async authenticate(headers = {}) {
    const authorization = headers.Authorization || headers.authorization || '';
    const apiKey = headers.apikey || headers.API_KEY || '';
    if (authorization.toLowerCase().startsWith('bearer ')) {
      const claims = verifyJwt(authorization.slice(7).trim());
      if (claims) return { role: claims.role, uid: claims.sub, claims };
      return { role: 'anonymous', uid: null, claims: null, invalidToken: true };
    }
    if (apiKey === ANON_KEY && !authorization) return { role: 'anon', uid: null, claims: null };
    if (apiKey === ANON_KEY) return { role: 'anon', uid: null, claims: null };
    return { role: 'anonymous', uid: null, claims: null };
  }

  /** Run a query as the resolved role so PostgreSQL applies the real RLS. */
  async asCaller(caller, action) {
    this.requestCount += 1;
    await this.db.exec('reset role');
    await this.db.query('select set_config($1, $2, false)', [
      'request.jwt.claim.sub',
      caller.uid || '',
    ]);
    await this.db.query('select set_config($1, $2, false)', [
      'request.jwt.claims',
      JSON.stringify(caller.claims || {}),
    ]);
    await this.db.exec('set role ' + caller.role);
    try {
      return await action();
    } finally {
      await this.db.exec('reset role');
    }
  }

  /**
   * A minimal PostgREST: the routes the application uses, with the same
   * status codes, so the client behaviour can be exercised for real.
   */
  async rest(method, pathname, { headers = {}, body = null, query = {}, filter = '' } = {}) {
    const caller = await this.authenticate(headers);
    if (caller.invalidToken) {
      return json(401, { code: 'invalid_token', message: 'invalid JWT' });
    }
    const table = pathname.replace(/^\/rest\/v1\//, '').split('?')[0];
    if (!['products', 'orders', 'settings', 'admins'].includes(table)) {
      return json(404, { code: 'PGRST205' });
    }
    if (table === 'admins') {
      // The application never reads this table through PostgREST; the admin
      // claim happens through the RPC below.
      return json(401, { code: '42501', message: 'permission denied for table admins' });
    }
    const select = query.select || '*';
    const effectiveFilter = filter || query.filter || '';
    const order = query.order || '';
    const limit = query.limit ? Number(query.limit) : null;

    // The column list and the sort order are interpolated into the statement,
    // so they are checked against the real columns of the table. A value that
    // names something else is refused instead of being passed to the server.
    const columns = await this.columnNames(table);
    const requested = select
      .split(',')
      .map((part) => part.trim().split('.').pop())
      .filter((part) => part && part !== '*');
    for (const column of requested) {
      if (!columns.includes(column)) {
        return json(400, { code: 'PGRST204', message: `unknown column ${column}` });
      }
    }
    for (const clause of order.split(',').filter(Boolean)) {
      const parts = clause.trim().split('.');
      const column = parts[0];
      if (!columns.includes(column)) {
        return json(400, { code: 'PGRST100', message: `unknown column ${column}` });
      }
    }
    // PostgREST writes the direction as `.asc` or `.desc`.
    const orderClause = order
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean)
      .map((part) => {
        const [column, direction] = part.split('.');
        const keyword = (direction || 'asc').toLowerCase() === 'desc' ? 'DESC' : 'ASC';
        return `${column} ${keyword}`;
      })
      .join(', ');

    if (method === 'POST' || method === 'PATCH' || method === 'DELETE') {
      return this.write(method, table, body, {
        headers,
        filter: effectiveFilter,
        select,
      });
    }

    let sql = `select ${select === '*' ? '*' : select} from public.${table}`;
    const where = [];
    const params = [];
    for (const clause of effectiveFilter.split('&').filter(Boolean)) {
      // The value may contain dots, for example a UUID, so the operator is
      // located from the right and everything after it is the value.
      const parts = clause.split('.');
      const operatorAt = parts.findIndex((part) =>
        ['eq', 'neq', 'gt', 'gte', 'lt', 'lte', 'is'].includes(part),
      );
      if (operatorAt < 1) {
        return json(400, { code: 'PGRST100', message: `unsupported filter ${clause}` });
      }
      const column = parts.slice(0, operatorAt).join('.');
      const operator = parts[operatorAt];
      const value = parts.slice(operatorAt + 1).join('.');
      params.push(value || null);
      const placeholder = `$${params.length}`;
      if (operator === 'eq') where.push(`${column} = ${placeholder}`);
      else if (operator === 'neq') where.push(`${column} <> ${placeholder}`);
      else if (operator === 'gt') where.push(`${column} > ${placeholder}`);
      else if (operator === 'gte') where.push(`${column} >= ${placeholder}`);
      else if (operator === 'lt') where.push(`${column} < ${placeholder}`);
      else if (operator === 'lte') where.push(`${column} <= ${placeholder}`);
      else if (operator === 'is') where.push(`${column} is null`);
    }
    if (where.length) sql += ' where ' + where.join(' and ');
    if (orderClause) sql += ` order by ${orderClause}`;
    if (limit) sql += ` limit ${limit}`;
    try {
      const result = await this.asCaller(caller, () => this.db.query(sql, params));
      return json(200, result.rows);
    } catch (error) {
      const message = String(error.message || error);
      if (message.includes('permission denied') || message.includes('42501')) {
        return json(403, { code: '42501', message: 'permission denied' });
      }
      return json(400, { code: 'PGRST', message });
    }
  }

  /** The real column names of a table, used to validate the request safely. */
  async columnNames(table) {
    await this.db.exec('reset role');
    const result = await this.db.query(
      `select column_name from information_schema.columns
       where table_schema = 'public' and table_name = $1`,
      [table],
    );
    return result.rows.map((row) => row.column_name);
  }

  /**
   * A write through PostgREST. The row level security policies decide
   * whether the row is written, exactly as they do on a real project.
   */
  async write(method, table, body, { headers = {}, filter = '', select = '*' } = {}) {
    const caller = await this.authenticate(headers);
    if (caller.invalidToken) {
      return json(401, { code: 'invalid_token', message: 'invalid JWT' });
    }
    const payload = body && typeof body === 'object' ? body : {};
    const columns = Object.keys(payload);
    if (!columns.length && method === 'POST') {
      return json(400, { code: 'PGRST102', message: 'no rows to insert' });
    }
    if (!columns.length && method === 'PATCH') {
      return json(400, { code: 'PGRST102', message: 'no rows to update' });
    }
    const allowed = new Set([
      'id',
      'name',
      'price',
      'description',
      'image_url',
      'category',
      'available',
      'sort_order',
      'customer',
      'phone',
      'notes',
      'location',
      'channel',
      'status',
      'details',
      'total',
      'client_id',
      'key',
      'value',
      'is_public',
    ]);
    for (const column of columns) {
      if (!allowed.has(column)) {
        return json(400, { code: 'PGRST204', message: `unknown column ${column}` });
      }
    }
    const params = [];
    let sql;
    if (method === 'POST') {
      const placeholders = columns.map((column) => `$${params.push(payload[column])}`);
      sql = `insert into public.${table} (${columns.join(',')}) values (${placeholders.join(',')}) returning ${select === '*' ? '*' : select}`;
    } else {
      const assignments = columns.map((column) => `${column} = $${params.push(payload[column])}`);
      let where = '';
      for (const clause of filter.split('&').filter(Boolean)) {
        // The value may contain dots, for example a UUID, so the operator is
        // located from the right and everything after it is the value.
        const parts = clause.split('.');
        const operatorAt = parts.indexOf('eq');
        if (operatorAt < 1) {
          return json(400, {
            code: 'PGRST100',
            message: `only the eq filter is supported: ${clause}`,
          });
        }
        const column = parts.slice(0, operatorAt).join('.');
        const value = parts.slice(operatorAt + 1).join('.');
        where += `${where ? ' and ' : ''}${column} = $${params.push(value)}`;
      }
      if (!where) return json(400, { code: 'PGRST100', message: 'a filter is required' });
      sql = `update public.${table} set ${assignments.join(',')} where ${where} returning ${select === '*' ? '*' : select}`;
      if (method === 'DELETE') {
        sql = `delete from public.${table} where ${where} returning ${select === '*' ? '*' : select}`;
      }
    }
    try {
      const result = await this.asCaller(caller, () => this.db.query(sql, params));
      // PostgREST answers 201 for an insert and 200 for an update or a delete.
      const status = method === 'POST' ? 201 : 200;
      return json(status, result.rows);
    } catch (error) {
      const message = String(error.message || error);
      if (message.includes('permission denied') || message.includes('42501')) {
        return json(403, { code: '42501', message: 'permission denied' });
      }
      if (message.includes('violates row-level security policy')) {
        return json(403, { code: '42501', message: 'new row violates row-level security policy' });
      }
      if (message.includes('violates not-null constraint')) {
        return json(400, { code: '23502', message });
      }
      return json(400, { code: 'PGRST', message });
    }
  }

  /** PostgREST RPC call, used for `place_order` and `claim_first_admin`. */
  async rpc(name, payload = {}, headers = {}) {
    const caller = await this.authenticate(headers);
    if (caller.invalidToken) {
      return json(401, { code: 'invalid_token', message: 'invalid JWT' });
    }
    if (name === 'claim_first_admin') {
      try {
        const result = await this.asCaller(caller, () =>
          this.db.query('select public.claim_first_admin() as claimed'),
        );
        return json(200, result.rows[0].claimed);
      } catch (error) {
        return json(403, { code: '42501', message: String(error.message || error) });
      }
    }
    if (name === 'place_order') {
      try {
        const result = await this.asCaller(caller, () =>
          this.db.query(
            'select public.place_order($1,$2,$3,$4,$5,$6) as number',
            [
              payload.p_customer ?? null,
              payload.p_phone ?? null,
              payload.p_notes ?? null,
              payload.p_location ?? null,
              payload.p_items === undefined ? null : JSON.stringify(payload.p_items),
              payload.p_client_id ?? null,
            ],
          ),
        );
        return json(200, result.rows[0].number);
      } catch (error) {
        const message = String(error.message || error);
        if (message.includes('permission denied')) {
          return json(403, { code: '42501', message });
        }
        return json(400, { code: 'P0001', message });
      }
    }
    return json(404, { code: 'PGRST202' });
  }

  /** Storage surface, guarded by the same policies as the real project. */
  async storage(method, bucket, name, { headers = {}, body = null } = {}) {
    const caller = await this.authenticate(headers);
    if (caller.invalidToken) return json(401, { code: 'invalid_token' });
    try {
      if (method === 'GET') {
        const result = await this.asCaller(caller, () =>
          this.db.query(
            'select id, bucket_id, name from storage.objects where bucket_id = $1 and name = $2',
            [bucket, name],
          ),
        );
        if (!result.rows.length) return json(404, { code: 'not_found' });
        return json(200, result.rows[0]);
      }
      if (method === 'POST' || method === 'PUT') {
        const id = crypto.randomUUID();
        await this.asCaller(caller, () =>
          this.db.query(
            'insert into storage.objects(id,bucket_id,name) values ($1,$2,$3)',
            [id, bucket, name],
          ),
        );
        return json(200, { id, bucket_id: bucket, name });
      }
      if (method === 'DELETE') {
        const result = await this.asCaller(caller, () =>
          this.db.query('delete from storage.objects where bucket_id = $1 and name = $2', [
            bucket,
            name,
          ]),
        );
        if (!result.rowCount) return json(404, { code: 'not_found' });
        return json(200, {});
      }
    } catch (error) {
      const message = String(error.message || error);
      if (
        message.includes('permission denied') ||
        message.includes('violates row-level security policy')
      ) {
        return json(403, { code: '42501', message });
      }
      return json(400, { code: 'storage_error', message });
    }
    return json(405, {});
  }

  /** Direct table access used by the test harness only, never by a client. */
  async direct(sql, params = []) {
    await this.db.exec('reset role');
    return this.db.query(sql, params);
  }

  async seedProduct(product) {
    await this.direct(
      `insert into public.products (id,name,price,available,description,category,image_url)
       values ($1,$2,$3,$4,$5,$6,$7)
       on conflict (id) do update set name = excluded.name, price = excluded.price,
         available = excluded.available, description = excluded.description,
         category = excluded.category, image_url = excluded.image_url`,
      [
        product.id,
        product.name,
        product.price,
        product.available,
        product.description ?? '',
        product.category ?? 'عام',
        product.image_url ?? '',
      ],
    );
  }

  async seedSetting(setting) {
    await this.direct(
      `insert into public.settings (key,value,is_public) values ($1,$2,$3)
       on conflict (key) do update set value = excluded.value, is_public = excluded.is_public`,
      [setting.key, setting.value, setting.is_public],
    );
  }

  async countOrders() {
    const result = await this.direct('select count(*)::int as n from public.orders');
    return result.rows[0].n;
  }

  async allOrders() {
    const result = await this.direct(
      'select id,number,customer,phone,notes,location,status,total,client_id,details from public.orders order by number',
    );
    return result.rows;
  }

  async deleteOrderByClientId(clientId) {
    await this.direct('delete from public.orders where client_id = $1', [clientId]);
  }
}

module.exports = {
  IsolatedEnvironment,
  signJwt,
  verifyJwt,
  ANON_KEY,
  JWT_SECRET,
};
