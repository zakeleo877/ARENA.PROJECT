/**
 * Acceptance checks 14-25 and the end-to-end application flows, executed
 * against an isolated throwaway Supabase-compatible environment.
 *
 * The environment is created from `supabase/schema.sql` on a private
 * PostgreSQL instance (PGlite) that lives only for the duration of this
 * process. No production project, no production URL and no production key is
 * ever contacted. The data is dummy data with `.invalid` email addresses, and
 * every order that the tests create is removed before the process exits.
 *
 * The checks run against the real SQL: the roles, the grants and the row
 * level security policies are the ones that ship in `supabase/schema.sql`.
 */
const { IsolatedEnvironment, ANON_KEY } = require('./isolated-environment.cjs');
const { seed, PRODUCTS, ACCOUNTS } = require('./seed-isolated-backend.cjs');

let passed = 0;
let failed = 0;
const failures = [];
const createdOrders = [];

function pass(message) {
  console.log('PASS:', message);
  passed += 1;
}

function fail(number, message, detail) {
  console.log(`FAIL: ${number}. ${message}`);
  if (detail) console.log('      detail:', detail);
  failures.push(`${number}. ${message}${detail ? ' -- ' + detail : ''}`);
  failed += 1;
}

function check(number, condition, message, detail) {
  if (condition) pass(`${number}. ${message}`);
  else fail(number, message, detail);
}

const anonHeaders = { apikey: ANON_KEY };

function authHeaders(session) {
  return { apikey: ANON_KEY, Authorization: `Bearer ${session.access_token}` };
}

const productA = PRODUCTS.availableA;
const productB = PRODUCTS.availableB;
const hiddenProduct = PRODUCTS.hidden;
const dummyEmail = ACCOUNTS.shopper.email;
const adminEmail = ACCOUNTS.owner.email;
const secondEmail = ACCOUNTS.second.email;

(async () => {
  const env = new IsolatedEnvironment();
  await env.start();

  // --------------------------------------------------------------- setup ---
  // The same seed the isolated backend uses, so the acceptance checks and the
  // Kotlin integration tests exercise one identical dataset.
  await seed(env);

  console.log('--- 14-25: database security on an isolated project ---');

  // ------------------------------------------------- 14: anonymous reads ---
  const anonOrders = await env.rest('GET', '/rest/v1/orders', { headers: anonHeaders });
  check(
    14,
    anonOrders.status === 403 || (anonOrders.status === 200 && !anonOrders.body.length),
    'anonymous read of the orders table returns no data',
    `status=${anonOrders.status}`,
  );

  // ------------------------------------------- 15: direct order insert ---
  const anonInsert = await env.rest('POST', '/rest/v1/orders', {
    headers: anonHeaders,
    body: { customer: 'probe', phone: '0550000000' },
  });
  check(
    15,
    anonInsert.status === 403 || anonInsert.status === 401,
    'a direct order insert without a signed-in account is refused',
    `status=${anonInsert.status}`,
  );

  // --------------------------------------------- 16: hidden product leak ---
  const anonProducts = await env.rest('GET', '/rest/v1/products', { headers: anonHeaders });
  const leaked = (anonProducts.body || []).some((p) => p.id === hiddenProduct);
  check(
    16,
    anonProducts.status === 200 && !leaked && anonProducts.body.length === 2,
    'anonymous visitors only see available products',
    `count=${(anonProducts.body || []).length}`,
  );

  // ------------------------------------------- 17: public sign-up closed ---
  const signup = await env.signUp('probe@example.invalid', 'Probe123456');
  check(
    17,
    signup.status === 400 || signup.status === 403 || signup.status === 422,
    'public sign-up is disabled',
    `status=${signup.status}`,
  );

  // --------------------------------------------- 18: sign-in / sign-out ---
  const signUpThroughAdmin = await env.createUser('fresh-account@example.invalid', 'FreshPass123');
  check(
    18,
    signUpThroughAdmin.status === 200 && !!signUpThroughAdmin.body.access_token,
    'an account can be created and receives a session',
    `status=${signUpThroughAdmin.status}`,
  );

  const wrongPassword = await env.signIn(adminEmail, 'WrongPassword123');
  check(
    19,
    wrongPassword.status === 400 || wrongPassword.status === 401,
    'sign-in with a wrong password is refused',
    `status=${wrongPassword.status}`,
  );

  const signIn = await env.signIn(adminEmail, 'OwnerPass123');
  check(
    20,
    signIn.status === 200 && !!signIn.body.access_token,
    'sign-in with the right password returns a session',
    `status=${signIn.status}`,
  );

  const shopper = await env.createUser(dummyEmail, 'ShopperPass123');
  const shopperSignIn = await env.signIn(dummyEmail, 'ShopperPass123');
  check(
    21,
    shopperSignIn.status === 200 && !!shopperSignIn.body.access_token,
    'a customer can sign in and get a usable token',
    `status=${shopperSignIn.status}`,
  );

  // ------------------------------------------------------- 22: admin claim ---
  const ownerIsAdmin = await env.rpc('claim_first_admin', {}, authHeaders(signIn.body));
  check(
    22,
    ownerIsAdmin.status === 200 && ownerIsAdmin.body === false,
    'the admin role is already claimed and cannot be claimed twice',
    `body=${JSON.stringify(ownerIsAdmin.body)}`,
  );

  const secondAccount = await env.createUser('later-account@example.invalid', 'LaterPass123');
  const secondClaim = await env.rpc('claim_first_admin', {}, authHeaders(secondAccount.body));
  check(
    23,
    secondClaim.status === 200 && secondClaim.body === false,
    'a later account cannot claim the admin role',
    `body=${JSON.stringify(secondClaim.body)}`,
  );

  const customerOrders = await env.rest('GET', '/rest/v1/orders', {
    headers: authHeaders(shopperSignIn.body),
  });
  check(
    24,
    customerOrders.status === 403 || (customerOrders.status === 200 && !customerOrders.body.length),
    'a signed-in customer cannot read the orders table directly',
    `status=${customerOrders.status}`,
  );

  const customerInsert = await env.rest('POST', '/rest/v1/orders', {
    headers: authHeaders(shopperSignIn.body),
    body: { customer: 'probe', phone: '0550000000' },
  });
  check(
    25,
    customerInsert.status === 403 || customerInsert.status === 401,
    'a signed-in customer cannot insert an order row directly',
    `status=${customerInsert.status}`,
  );

  console.log('\n--- Application flows: catalogue, cart, order, retry ---');

  // ------------------------------------------------ catalogue and categories ---
  const catalogue = await env.rest('GET', '/rest/v1/products', { headers: anonHeaders });
  const categories = [...new Set(catalogue.body.map((p) => p.category))].sort();
  check(
    '4',
    catalogue.status === 200 &&
      catalogue.body.length === 2 &&
      categories.join(',') === 'حلويات,مشروبات',
    'the catalogue shows the available products and their categories',
    `categories=${categories.join(',')}`,
  );

  const settings = await env.rest('GET', '/rest/v1/settings', { headers: anonHeaders });
  const publicKeys = (settings.body || []).map((s) => s.key);
  const adminSettings = await env.rest('GET', '/rest/v1/settings', {
    headers: authHeaders(signIn.body),
  });
  const adminKeys = (adminSettings.body || []).map((s) => s.key);
  check(
    '4b',
    settings.status === 200 &&
      publicKeys.includes('storeName') &&
      !publicKeys.includes('secret_note') &&
      adminKeys.includes('secret_note'),
    'only public settings are exposed to visitors, while the admin sees every setting',
    `visitor=${publicKeys.join(',')} admin=${adminKeys.join(',')}`,
  );

  // --------------------------------------------------- placing an order ---
  const clientId = crypto.randomUUID();
  const orderPayload = {
    p_customer: 'زبون اختبار',
    p_phone: '0550123456',
    p_notes: 'ملاحظة اختبار',
    p_location: 'https://maps.example.invalid/?q=1',
    p_items: [
      { product_id: productA, qty: 2 },
      { product_id: productB, qty: 1 },
    ],
    p_client_id: clientId,
  };
  const placed = await env.rpc('place_order', orderPayload, anonHeaders);
  createdOrders.push(clientId);
  check(
    '5',
    placed.status === 200 && typeof placed.body === 'number' && placed.body > 0,
    'an order is created through the place_order RPC',
    `status=${placed.status} body=${JSON.stringify(placed.body)}`,
  );

  const expectedTotal = 125.5 * 2 + 80;
  const stored = await env.allOrders();
  const myOrder = stored.find((o) => o.client_id === clientId);
  check(
    '5b',
    !!myOrder && Number(myOrder.total) === expectedTotal,
    'the server computes the order total from the catalogue prices',
    `total=${myOrder && myOrder.total} expected=${expectedTotal}`,
  );
  check(
    '5c',
    !!myOrder && myOrder.details && myOrder.details.length === 2,
    'the order keeps a snapshot of the items and their prices',
    `details=${myOrder && myOrder.details && myOrder.details.length}`,
  );
  check(
    '5d',
    !!myOrder && myOrder.location === 'https://maps.example.invalid/?q=1',
    'the order stores the customer location link as given',
    `location=${myOrder && myOrder.location}`,
  );

  // --------------------------------------------- 6: retry after a failure ---
  const retry = await env.rpc('place_order', orderPayload, anonHeaders);
  const afterRetry = await env.allOrders();
  const retryRows = afterRetry.filter((o) => o.client_id === clientId);
  check(
    '6',
    retry.status === 200 &&
      retry.body === placed.body &&
      retryRows.length === 1,
    'resending the same order after a lost connection does not duplicate it',
    `rows=${retryRows.length} numbers=${retryRows.map((r) => r.number).join(',')}`,
  );

  const otherClientId = crypto.randomUUID();
  const otherOrder = await env.rpc(
    'place_order',
    { ...orderPayload, p_client_id: otherClientId },
    anonHeaders,
  );
  createdOrders.push(otherClientId);
  const bothOrders = (await env.allOrders()).filter((o) => o.client_id !== null);
  check(
    '6b',
    otherOrder.status === 200 && otherOrder.body !== placed.body && bothOrders.length === 2,
    'a genuinely new order gets its own number',
    `numbers=${bothOrders.map((o) => o.number).join(',')}`,
  );

  // --------------------------------------------------- invalid input cases ---
  const invalidCases = [
    ['invalid_customer', { ...orderPayload, p_customer: 'x', p_client_id: crypto.randomUUID() }],
    ['invalid_phone', { ...orderPayload, p_phone: 'abc', p_client_id: crypto.randomUUID() }],
    ['invalid_notes', { ...orderPayload, p_notes: 'x'.repeat(501), p_client_id: crypto.randomUUID() }],
    ['invalid_location', { ...orderPayload, p_location: 'x'.repeat(201), p_client_id: crypto.randomUUID() }],
    ['invalid_items', { ...orderPayload, p_items: [], p_client_id: crypto.randomUUID() }],
    ['invalid_items', { ...orderPayload, p_items: null, p_client_id: crypto.randomUUID() }],
  ];
  const invalidResults = [];
  for (const [expected, payload] of invalidCases) {
    const response = await env.rpc('place_order', payload, anonHeaders);
    createdOrders.push(payload.p_client_id);
    invalidResults.push(
      response.status === 400 && String(response.body.message || '').includes(expected),
    );
  }
  check(
    '9',
    invalidResults.every(Boolean),
    'every malformed order is rejected with a clear error',
    `passed=${invalidResults.filter(Boolean).length}/${invalidResults.length}`,
  );

  const hiddenOrder = await env.rpc(
    'place_order',
    {
      ...orderPayload,
      p_items: [{ product_id: hiddenProduct, qty: 1 }],
      p_client_id: crypto.randomUUID(),
    },
    anonHeaders,
  );
  createdOrders.push(orderPayload.p_client_id);
  check(
    '9b',
    hiddenOrder.status === 400,
    'an order for an unavailable product is rejected',
    `status=${hiddenOrder.status}`,
  );

  // --------------------------------------------------- admin application ---
  const adminOrders = await env.rest('GET', '/rest/v1/orders', {
    headers: authHeaders(signIn.body),
  });
  check(
    '7',
    adminOrders.status === 200 && adminOrders.body.length >= 2,
    'the admin app can list the orders',
    `count=${adminOrders.body && adminOrders.body.length}`,
  );

  const adminProducts = await env.rest('GET', '/rest/v1/products', {
    headers: authHeaders(signIn.body),
  });
  const adminSeesHidden = (adminProducts.body || []).some((p) => p.id === hiddenProduct);
  check(
    '7b',
    adminProducts.status === 200 && adminSeesHidden,
    'the admin app can see the hidden products',
    `count=${(adminProducts.body || []).length}`,
  );

  const customerProducts = await env.rest('GET', '/rest/v1/products', {
    headers: authHeaders(shopperSignIn.body),
  });
  const customerSeesHidden = (customerProducts.body || []).some((p) => p.id === hiddenProduct);
  check(
    '7c',
    customerProducts.status === 200 && !customerSeesHidden,
    'a customer still cannot see the hidden products',
    `count=${(customerProducts.body || []).length}`,
  );

  // ------------------------------------------- storage policies for admins ---
  const adminUpload = await env.storage('POST', 'product-images', 'admin.png', {
    headers: authHeaders(signIn.body),
  });
  check(
    '7d',
    adminUpload.status === 200,
    'the admin can upload a product image',
    `status=${adminUpload.status}`,
  );

  const customerUpload = await env.storage('POST', 'product-images', 'customer.png', {
    headers: authHeaders(shopperSignIn.body),
  });
  check(
    '7e',
    customerUpload.status === 403,
    'a customer cannot upload a product image',
    `status=${customerUpload.status}`,
  );

  const customerRead = await env.storage('GET', 'product-images', 'admin.png', {
    headers: authHeaders(shopperSignIn.body),
  });
  const customerReadRows = await env.direct(
    'select count(*)::int as n from storage.objects where name = $1',
    ['admin.png'],
  );
  check(
    '7f',
    customerRead.status === 403 || customerRead.status === 404,
    'a customer cannot read a private product image',
    `status=${customerRead.status}`,
  );

  const adminDelete = await env.storage('DELETE', 'product-images', 'admin.png', {
    headers: authHeaders(signIn.body),
  });
  check(
    '7g',
    adminDelete.status === 200,
    'the admin can delete a product image',
    `status=${adminDelete.status}`,
  );

  // ------------------------------------------------------- sign-out ---
  const signOut = await env.signOut(shopperSignIn.body.refresh_token);
  const refreshAfterSignOut = await env.refresh(shopperSignIn.body.refresh_token);
  check(
    '3',
    signOut.status === 204 && refreshAfterSignOut.status === 400,
    'signing out invalidates the refresh token',
    `signOut=${signOut.status} refresh=${refreshAfterSignOut.status}`,
  );

  // ---------------------------------------------------- token forgery ---
  const forged = await env.rest('GET', '/rest/v1/orders', {
    headers: { apikey: ANON_KEY, Authorization: 'Bearer not.a.real.token' },
  });
  check(
    '9c',
    forged.status === 401,
    'a forged bearer token is rejected instead of being trusted',
    `status=${forged.status}`,
  );

  const orderToUpdate = (await env.allOrders()).find((o) => o.client_id === clientId);
  const adminStatusUpdate = await env.rest('PATCH', '/rest/v1/orders', {
    headers: authHeaders(signIn.body),
    filter: `id.eq.${orderToUpdate.id}`,
    body: { status: 'قيد التحضير' },
  });
  check(
    '7h',
    adminStatusUpdate.status === 200 && adminStatusUpdate.body.length === 1,
    'the admin app can change the status of an order',
    `status=${adminStatusUpdate.status} body=${JSON.stringify(adminStatusUpdate.body)}`,
  );

  const customerStatusUpdate = await env.rest('PATCH', '/rest/v1/orders', {
    headers: authHeaders(shopperSignIn.body),
    filter: `id.eq.${orderToUpdate.id}`,
    body: { status: 'ملغى' },
  });
  check(
    '7i',
    customerStatusUpdate.status === 403 || customerStatusUpdate.body.length === 0,
    'a customer cannot change the status of an order',
    `status=${customerStatusUpdate.status}`,
  );

  const adminProductWrite = await env.rest('PATCH', '/rest/v1/products', {
    headers: authHeaders(signIn.body),
    filter: `id.eq.${productA}`,
    body: { name: 'منتج محدَّث' },
  });
  check(
    '7j',
    adminProductWrite.status === 200 && adminProductWrite.body.length === 1,
    'the admin app can edit a product',
    `status=${adminProductWrite.status}`,
  );

  const customerProductWrite = await env.rest('PATCH', '/rest/v1/products', {
    headers: authHeaders(shopperSignIn.body),
    filter: `id.eq.${productA}`,
    body: { name: 'محاولة تعديل' },
  });
  check(
    '7k',
    customerProductWrite.status === 403 || customerProductWrite.body.length === 0,
    'a customer cannot edit a product',
    `status=${customerProductWrite.status}`,
  );

  const adminProductDelete = await env.rest('DELETE', '/rest/v1/products', {
    headers: authHeaders(signIn.body),
    filter: `id.eq.${hiddenProduct}`,
  });
  check(
    '7l',
    adminProductDelete.status === 200 && adminProductDelete.body.length === 1,
    'the admin app can delete a product',
    `status=${adminProductDelete.status}`,
  );

  const customerProductDelete = await env.rest('DELETE', '/rest/v1/products', {
    headers: authHeaders(shopperSignIn.body),
    filter: `id.eq.${productB}`,
  });
  check(
    '7m',
    customerProductDelete.status === 403 || customerProductDelete.body.length === 0,
    'a customer cannot delete a product',
    `status=${customerProductDelete.status}`,
  );

  // ----------------------------------------------------------- cleanup ---
  for (const id of createdOrders) {
    await env.deleteOrderByClientId(id);
  }
  const remaining = await env.countOrders();
  check(
    'cleanup',
    remaining === 0,
    'every order created by the tests was removed',
    `remaining=${remaining}`,
  );

  await env.stop();

  console.log('');
  console.log(`${passed} checks passed, ${failed} failed.`);
  if (failed) {
    console.log('Failures:');
    for (const item of failures) console.log('  - ' + item);
    process.exit(1);
  }
})().catch((error) => {
  console.error('The test run crashed:', error);
  process.exit(1);
});
