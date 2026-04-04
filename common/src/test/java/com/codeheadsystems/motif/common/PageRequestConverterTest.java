package com.codeheadsystems.motif.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageRequestConverterTest {

  private SecretKey key;
  private PageRequestConverter converter;

  @BeforeEach
  void setUp() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
    keyGen.init(256);
    key = keyGen.generateKey();
    converter = new PageRequestConverter(() -> key);
  }

  @Test
  void roundTripPreservesValues() {
    PageRequest original = new PageRequest(3, 25);
    String token = converter.encode(original);
    PageRequest decoded = converter.decode(token);

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void roundTripFirstPage() {
    PageRequest original = PageRequest.first();
    String token = converter.encode(original);
    PageRequest decoded = converter.decode(token);

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void encodedTokenIsJwtFormat() {
    String token = converter.encode(PageRequest.first(10));
    // JWTs have three dot-separated base64url segments
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  void decodeInvalidTokenThrows() {
    assertThatThrownBy(() -> converter.decode("not-a-valid-jwt"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decodeTamperedTokenThrows() {
    String token = converter.encode(PageRequest.first(10));
    // Flip a character in the signature (third segment)
    String[] parts = token.split("\\.");
    char[] sig = parts[2].toCharArray();
    sig[0] = sig[0] == 'A' ? 'B' : 'A';
    parts[2] = new String(sig);
    String tampered = String.join(".", parts);

    assertThatThrownBy(() -> converter.decode(tampered))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("signature verification failed");
  }

  @Test
  void decodeWithWrongKeyThrows() throws Exception {
    String token = converter.encode(new PageRequest(1, 20));

    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
    keyGen.init(256);
    SecretKey otherKey = keyGen.generateKey();
    PageRequestConverter otherConverter = new PageRequestConverter(() -> otherKey);

    assertThatThrownBy(() -> otherConverter.decode(token))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("signature verification failed");
  }
}
