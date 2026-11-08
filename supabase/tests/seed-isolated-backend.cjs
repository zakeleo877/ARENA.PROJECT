/**
 * Seeds the isolated backend with the dummy data the integration tests use.
 *
 * The data is deliberately fake: the products are examples, the settings are
 * the public store settings, and the accounts use the reserved `.invalid`
 * domain so they can never reach a real inbox.
 */
const { IsolatedEnvironment } = require('./isolated-environment.cjs');

const PRODUCTS = {
  availableA: '30000000-0000-4000-8000-000000000001',
  availableB: '30000000-0000-4000-8000-000000000002',
  hidden: '30000000-0000-4000-8000-000000000003',
};

const ACCOUNTS = {
  owner: { email: 'owner@example.invalid', password: 'OwnerPass123' },
  shopper: { email: 'shopper@example.invalid', password: 'ShopperPass123' },
  second: { email: 'second@example.invalid', password: 'SecondPass123' },
};

async function seed(environment) {
  await environment.seedProduct({
    id: PRODUCTS.availableA,
    name: 'منتج متاح',
    price: 125.5,
    available: true,
    description: 'وصف منتج للاختبار',
    category: 'حلويات',
    image_url: 'https://example.invalid/a.png',
  });
  await environment.seedProduct({
    id: PRODUCTS.availableB,
    name: 'منتج ثانٍ',
    price: 80,
    available: true,
    description: 'وصف منتج للاختبار',
    category: 'مشروبات',
    image_url: 'https://example.invalid/b.png',
  });
  await environment.seedProduct({
    id: PRODUCTS.hidden,
    name: 'منتج مخفي',
    price: 50,
    available: false,
    description: 'منتج مخفي عن الزائر',
    category: 'حلويات',
    image_url: '',
  });

  // The public settings the customer application reads.
  await environment.seedSetting({ key: 'storeName', value: 'محلات السعيد', is_public: true });
  await environment.seedSetting({ key: 'whatsapp', value: '213776151686', is_public: true });
  await environment.seedSetting({ key: 'currency', value: 'دج', is_public: true });
  await environment.seedSetting({
    key: 'footerNote',
    value: 'شكراً لثقتكم بمحلات السعيد',
    is_public: true,
  });
  // A private setting, so the visibility of the settings can be checked.
  await environment.seedSetting({ key: 'secret_note', value: 'داخلي', is_public: false });

  // The accounts the authentication tests use.
  const owner = await environment.createUser(ACCOUNTS.owner.email, ACCOUNTS.owner.password);
  await environment.rpc(
    'claim_first_admin',
    {},
    { apikey: environment.anonKey, Authorization: `Bearer ${owner.body.access_token}` },
  );
  await environment.createUser(ACCOUNTS.shopper.email, ACCOUNTS.shopper.password);
  await environment.createUser(ACCOUNTS.second.email, ACCOUNTS.second.password);
}

module.exports = { seed, PRODUCTS, ACCOUNTS };

if (require.main === module) {
  (async () => {
    const environment = new IsolatedEnvironment();
    await environment.start();
    await seed(environment);
    await environment.stop();
    console.log('The isolated backend was seeded with dummy data.');
  })().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}
