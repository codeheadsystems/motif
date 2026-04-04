package com.codeheadsystems.motif.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Converts a string into a {@link SecretKey} suitable for HMAC-SHA256 signing.
 * The input string is hashed with SHA-256 to produce a consistent 256-bit key
 * regardless of the input length.
 */
@Singleton
public final class SecretKeyFactory {

  @Inject
  public SecretKeyFactory() {
  }

  /**
   * Derives an HMAC-SHA256 {@link SecretKey} from the given string.
   *
   * @param secret the secret string (must not be null or blank).
   * @return a 256-bit SecretKey.
   */
  public SecretKey hmacS256FromString(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("Secret must not be null or blank");
    }
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256")
          .digest(secret.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(hash, "HmacSHA256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
