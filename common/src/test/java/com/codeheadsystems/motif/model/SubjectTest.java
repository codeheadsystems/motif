package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubjectTest {

  private static final Category CATEGORY = new Category("topic");

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullCategory() {
    assertThatThrownBy(() -> new Subject(null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Subject(CATEGORY, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    assertThatThrownBy(() -> new Subject(CATEGORY, "   \t   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan128Characters() {
    String tooLong = "x".repeat(129);

    assertThatThrownBy(() -> new Subject(CATEGORY, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 128 characters");
  }

  @Test
  void constructorAcceptsValueAt128Characters() {
    String maxLength = "x".repeat(128);

    Subject subject = new Subject(CATEGORY, maxLength);

    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo(maxLength);
    assertThat(subject.value()).hasSize(128);
  }

  @Test
  void constructorStripsLeadingAndTrailingWhitespace() {
    Subject subject = new Subject(CATEGORY, "  deploy checklist  ");

    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("deploy checklist");
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Subject subject = new Subject(CATEGORY, "test");

    assertThat(subject.identifier()).isNotNull();
    assertThat(subject.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Subject subject = new Subject(CATEGORY, "test", id);

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
    Subject original = new Subject(CATEGORY, "original", id);

    Subject copy = Subject.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingIdentifier() {
    Subject original = new Subject(CATEGORY, "test");
    Identifier newId = new Identifier();

    Subject modified = Subject.from(original)
        .identifier(newId)
        .build();

    assertThat(modified.category()).isEqualTo(original.category());
    assertThat(modified.value()).isEqualTo(original.value());
    assertThat(modified.identifier()).isSameAs(newId);
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullCategory() {
    assertThatThrownBy(() -> Subject.builder(null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void builderRejectsNullValue() {
    assertThatThrownBy(() -> Subject.builder(CATEGORY, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void builderWithDefaults() {
    Subject subject = Subject.builder(CATEGORY, "test").build();

    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("test");
    assertThat(subject.identifier()).isNotNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();

    Subject subject = Subject.builder(CATEGORY, "test")
        .identifier(id)
        .build();

    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("test");
    assertThat(subject.identifier()).isSameAs(id);
  }
}
