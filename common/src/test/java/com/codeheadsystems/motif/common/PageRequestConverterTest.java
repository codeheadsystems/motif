package com.codeheadsystems.motif.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageRequestConverterTest {

  @Test
  void roundTripPreservesValues() {
    PageRequest original = new PageRequest(3, 25);
    String token = PageRequestConverter.encode(original);
    PageRequest decoded = PageRequestConverter.decode(token);

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void roundTripFirstPage() {
    PageRequest original = PageRequest.first();
    String token = PageRequestConverter.encode(original);
    PageRequest decoded = PageRequestConverter.decode(token);

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void encodedTokenIsBase64() {
    String token = PageRequestConverter.encode(PageRequest.first(10));
    // URL-safe base64 contains only alphanumerics, hyphens, and underscores
    assertThat(token).matches("[A-Za-z0-9_-]+");
  }

  @Test
  void decodeInvalidTokenThrows() {
    assertThatThrownBy(() -> PageRequestConverter.decode("not-valid-base64!!!"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decodeMalformedPayloadThrows() {
    // Valid base64 but wrong content
    String token = java.util.Base64.getUrlEncoder().withoutPadding()
        .encodeToString("garbage".getBytes());
    assertThatThrownBy(() -> PageRequestConverter.decode(token))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
