package com.codeheadsystems.motif.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecretKeyFactoryTest {

  private SecretKeyFactory factory;

  @BeforeEach
  void setUp() {
    factory = new SecretKeyFactory();
  }

  @Test
  void hmacS256FromStringProducesConsistentKey() {
    SecretKey key1 = factory.hmacS256FromString("my-secret");
    SecretKey key2 = factory.hmacS256FromString("my-secret");

    assertThat(key1.getEncoded()).isEqualTo(key2.getEncoded());
  }

  @Test
  void hmacS256FromStringProduces256BitKey() {
    SecretKey key = factory.hmacS256FromString("any-input");

    assertThat(key.getEncoded()).hasSize(32); // 256 bits
    assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
  }

  @Test
  void differentInputsProduceDifferentKeys() {
    SecretKey key1 = factory.hmacS256FromString("secret-a");
    SecretKey key2 = factory.hmacS256FromString("secret-b");

    assertThat(key1.getEncoded()).isNotEqualTo(key2.getEncoded());
  }

  @Test
  void worksWithPageRequestConverter() {
    SecretKey key = factory.hmacS256FromString("my-signing-key");
    PageRequestConverter converter = new PageRequestConverter(() -> key);

    PageRequest original = new PageRequest(5, 30);
    PageRequest decoded = converter.decode(converter.encode(original));

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void nullSecretThrows() {
    assertThatThrownBy(() -> factory.hmacS256FromString(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void blankSecretThrows() {
    assertThatThrownBy(() -> factory.hmacS256FromString("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
