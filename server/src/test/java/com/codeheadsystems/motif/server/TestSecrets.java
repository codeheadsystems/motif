package com.codeheadsystems.motif.server;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Per-JVM random OPAQUE/JWT secrets for integration tests. Generated once at class load so
 * that all tests in a single run share a stable key set (registrations made in one test
 * remain valid for subsequent tests within the same JVM).
 */
final class TestSecrets {

  static final String SERVER_KEY_SEED_HEX = randomHex32();
  static final String OPRF_SEED_HEX = randomHex32();
  static final String OPRF_MASTER_KEY_HEX = randomHex32();
  static final String JWT_SECRET_HEX = randomHex32();

  private TestSecrets() {}

  private static String randomHex32() {
    byte[] value = new byte[32];
    new SecureRandom().nextBytes(value);
    return HexFormat.of().formatHex(value);
  }
}
