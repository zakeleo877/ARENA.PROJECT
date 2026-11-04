/**
 * Starts the isolated backend and prints its address on stdout.
 *
 * The Gradle test task starts this process, reads the address and the public
 * key, and passes them to the test JVM through environment variables. The
 * backend is bound to the loopback interface and uses dummy data only.
 *
 * Usage: node run-isolated-backend.cjs
 * Output: SAEED_TEST_BACKEND_URL=<url> SAEED_TEST_BACKEND_KEY=<key>
 */
const { IsolatedServer } = require('./isolated-server.cjs');

(async () => {
  const server = new IsolatedServer();
  await server.start();
  process.stdout.write(
    `SAEED_TEST_BACKEND_URL=${server.baseUrl} SAEED_TEST_BACKEND_KEY=${server.publicKey}\n`,
  );
  const stop = async () => {
    try {
      await server.stop();
    } finally {
      process.exit(0);
    }
  };
  process.on('SIGTERM', stop);
  process.on('SIGINT', stop);
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
