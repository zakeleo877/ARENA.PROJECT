/**
 * A real HTTP server in front of the isolated environment.
 *
 * The application talks plain HTTP to a Supabase project: the REST surface,
 * the RPC surface, the auth surface and the storage surface. This server
 * exposes the isolated environment over a real socket so the application code
 * can be driven against it exactly as it would be against a hosted project,
 * without any production project and without any production key.
 *
 * It is only ever bound to the loopback interface.
 */
const http = require('http');
const { IsolatedEnvironment } = require('./isolated-environment.cjs');
const { seed } = require('./seed-isolated-backend.cjs');

/** Reads a request body and parses it as JSON when possible. */
function readBody(request) {
  return new Promise((resolve) => {
    const chunks = [];
    request.on('data', (chunk) => chunks.push(chunk));
    request.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8');
      if (!raw) {
        resolve(null);
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch {
        resolve(raw);
      }
    });
  });
}

class IsolatedServer {
  constructor() {
    this.environment = new IsolatedEnvironment();
    this.server = null;
    this.port = 0;
  }

  async start() {
    await this.environment.start();
    await seed(this.environment);
    this.server = http.createServer((request, response) => {
      this.handle(request, response).catch((error) => {
        response.writeHead(500, { 'content-type': 'application/json' });
        response.end(JSON.stringify({ message: String(error && error.message) }));
      });
    });
    await new Promise((resolve) => {
      this.server.listen(0, '127.0.0.1', resolve);
    });
    this.port = this.server.address().port;
    return this;
  }

  get baseUrl() {
    return `http://127.0.0.1:${this.port}`;
  }

  get publicKey() {
    return this.environment.anonKey;
  }

  async stop() {
    if (this.server) {
      await new Promise((resolve) => this.server.close(resolve));
      this.server = null;
    }
    await this.environment.stop();
  }

  async handle(request, response) {
    const url = new URL(request.url, this.baseUrl);
    const headers = {};
    for (const [name, value] of Object.entries(request.headers)) {
      headers[name] = Array.isArray(value) ? value.join(',') : value;
    }
    const body = await readBody(request);

    const send = (status, payload) => {
      if (status === 204) {
        response.writeHead(204);
        response.end();
        return;
      }
      const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
      response.writeHead(status, { 'content-type': 'application/json' });
      response.end(text);
    };

    // ------------------------------------------------------------ auth ---
    if (url.pathname === '/auth/v1/signup' && request.method === 'POST') {
      const result = await this.environment.signUp(body.email, body.password);
      send(result.status, result.body);
      return;
    }
    if (url.pathname === '/auth/v1/token' && request.method === 'POST') {
      const grant = url.searchParams.get('grant_type');
      if (grant === 'password') {
        const result = await this.environment.signIn(body.email, body.password);
        send(result.status, result.body);
        return;
      }
      if (grant === 'refresh_token') {
        const result = await this.environment.refresh(body.refresh_token);
        send(result.status, result.body);
        return;
      }
      send(400, { error: 'unsupported_grant_type' });
      return;
    }
    if (url.pathname === '/auth/v1/logout' && request.method === 'POST') {
      const result = await this.environment.signOut(body && body.refresh_token);
      send(result.status, result.body);
      return;
    }

    // ------------------------------------------------------------ rest ---
    if (url.pathname.startsWith('/rest/v1/')) {
      const table = url.pathname.slice('/rest/v1/'.length);
      const query = {};
      for (const [name, value] of url.searchParams.entries()) {
        query[name] = value;
      }
      if (table.startsWith('rpc/')) {
        const result = await this.environment.rpc(table.slice(4), body || {}, headers);
        send(result.status, result.body);
        return;
      }
      const result = await this.environment.rest(request.method, `/rest/v1/${table}`, {
        headers,
        body,
        query,
      });
      send(result.status, result.body);
      return;
    }

    // --------------------------------------------------------- storage ---
    if (url.pathname.startsWith('/storage/v1/object/')) {
      const parts = url.pathname.split('/');
      const bucket = parts[4];
      const name = decodeURIComponent(parts.slice(5).join('/'));
      const result = await this.environment.storage(request.method, bucket, name, {
        headers,
        body,
      });
      send(result.status, result.body);
      return;
    }

    send(404, { message: 'not found' });
  }
}

module.exports = { IsolatedServer };
