package com.codeheadsystems.motif.server.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OwnerTest {

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Owner(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    assertThatThrownBy(() -> new Owner("   \t   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan256Characters() {
    String tooLong = "x".repeat(257);

    assertThatThrownBy(() -> new Owner(tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 256 characters");
  }

  @Test
  void constructorAcceptsValueAt256Characters() {
    String maxLength = "x".repeat(256);

    Owner owner = new Owner(maxLength);

    assertThat(owner.value()).hasSize(256);
  }

  @Test
  void constructorForcesUpperCase() {
    Owner owner = new Owner("lowercase");

    assertThat(owner.value()).isEqualTo("LOWERCASE");
  }

  @Test
  void constructorStripsWhitespace() {
    Owner owner = new Owner("  test  ");

    assertThat(owner.value()).isEqualTo("TEST");
  }

  @Test
  void constructorDefaultsIdentifier() {
    Owner owner = new Owner("test");

    assertThat(owner.identifier()).isNotNull();
    assertThat(owner.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Owner owner = new Owner("test", id, false);

    assertThat(owner.identifier()).isSameAs(id);
  }

  // --- Builder.from tests ---

  @Test
  void fromRejectsNull() {
    assertThatThrownBy(() -> Owner.from(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("owner cannot be null");
  }

  @Test
  void fromCopiesAllFields() {
    Identifier id = new Identifier();
    Owner original = new Owner("ORIGINAL", id, false);

    Owner copy = Owner.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingIdentifier() {
    Owner original = new Owner("test");
    Identifier newId = new Identifier();

    Owner modified = Owner.from(original)
        .identifier(newId)
        .build();

    assertThat(modified.value()).isEqualTo(original.value());
    assertThat(modified.identifier()).isSameAs(newId);
  }

  // --- Builder tests ---

  @Test
  void builderWithDefaults() {
    Owner owner = Owner.builder().value("test").build();

    assertThat(owner.value()).isEqualTo("TEST");
    assertThat(owner.identifier()).isNotNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();

    Owner owner = Owner.builder()
        .value("test")
        .identifier(id)
        .build();

    assertThat(owner.value()).isEqualTo("TEST");
    assertThat(owner.identifier()).isSameAs(id);
  }
}
