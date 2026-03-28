package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubjectTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("topic");

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullOwner() {
    assertThatThrownBy(() -> new Subject(null, CATEGORY, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("owner cannot be null");
  }

  @Test
  void constructorRejectsNullCategory() {
    assertThatThrownBy(() -> new Subject(OWNER, null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Subject(OWNER, CATEGORY, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    assertThatThrownBy(() -> new Subject(OWNER, CATEGORY, "   \t   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan128Characters() {
    String tooLong = "x".repeat(129);

    assertThatThrownBy(() -> new Subject(OWNER, CATEGORY, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 128 characters");
  }

  @Test
  void constructorAcceptsValueAt128Characters() {
    String maxLength = "x".repeat(128);

    Subject subject = new Subject(OWNER, CATEGORY, maxLength);

    assertThat(subject.owner()).isEqualTo(OWNER);
    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo(maxLength);
    assertThat(subject.value()).hasSize(128);
  }

  @Test
  void constructorStripsLeadingAndTrailingWhitespace() {
    Subject subject = new Subject(OWNER, CATEGORY, "  deploy checklist  ");

    assertThat(subject.value()).isEqualTo("deploy checklist");
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Subject subject = new Subject(OWNER, CATEGORY, "test");

    assertThat(subject.identifier()).isNotNull();
    assertThat(subject.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Subject subject = new Subject(OWNER, CATEGORY, "test", id);

    assertThat(subject.identifier()).isSameAs(id);
  }

  // --- Builder.from tests ---

  @Test
  void fromRejectsNull() {
    assertThatThrownBy(() -> Subject.from(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void fromCopiesAllFields() {
    Identifier id = new Identifier();
    Subject original = new Subject(OWNER, CATEGORY, "original", id);

    Subject copy = Subject.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingIdentifier() {
    Subject original = new Subject(OWNER, CATEGORY, "test");
    Identifier newId = new Identifier();

    Subject modified = Subject.from(original)
        .identifier(newId)
        .build();

    assertThat(modified.owner()).isEqualTo(original.owner());
    assertThat(modified.category()).isEqualTo(original.category());
    assertThat(modified.value()).isEqualTo(original.value());
    assertThat(modified.identifier()).isSameAs(newId);
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullOwner() {
    assertThatThrownBy(() -> Subject.builder(null, CATEGORY, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("owner cannot be null");
  }

  @Test
  void builderRejectsNullCategory() {
    assertThatThrownBy(() -> Subject.builder(OWNER, null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void builderRejectsNullValue() {
    assertThatThrownBy(() -> Subject.builder(OWNER, CATEGORY, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void builderWithDefaults() {
    Subject subject = Subject.builder(OWNER, CATEGORY, "test").build();

    assertThat(subject.owner()).isEqualTo(OWNER);
    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("test");
    assertThat(subject.identifier()).isNotNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();

    Subject subject = Subject.builder(OWNER, CATEGORY, "test")
        .identifier(id)
        .build();

    assertThat(subject.owner()).isEqualTo(OWNER);
    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("test");
    assertThat(subject.identifier()).isSameAs(id);
  }
}
