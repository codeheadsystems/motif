package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentifierTest {

  @Test
  void canonicalConstructorStoresUuid() {
    UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    Identifier identifier = new Identifier(uuid);

    assertThat(identifier.uuid()).isEqualTo(uuid);
  }

  @Test
  void canonicalConstructorRejectsNullUuid() {
    assertThatThrownBy(() -> new Identifier(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("uuid cannot be null");
  }

  @Test
  void defaultConstructorGeneratesUuid() {
    Identifier identifier = new Identifier();

    assertThat(identifier.uuid()).isNotNull();
  }

  @Test
  void equalityAndHashCodeDependOnUuid() {
    UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    Identifier left = new Identifier(uuid);
    Identifier same = new Identifier(uuid);
    Identifier different = new Identifier(
        UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));

    assertThat(left)
        .isEqualTo(same)
        .hasSameHashCodeAs(same)
        .isNotEqualTo(different);
  }
}
