package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubjectTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Identifier OWNER_ID = OWNER.identifier();
  private static final Category CATEGORY = new Category("topic");

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> new Subject(null, CATEGORY, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void constructorRejectsNullCategory() {
    assertThatThrownBy(() -> new Subject(OWNER_ID, null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Subject(OWNER_ID, CATEGORY, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    assertThatThrownBy(() -> new Subject(OWNER_ID, CATEGORY, "   \t   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan128Characters() {
    String tooLong = "x".repeat(129);

    assertThatThrownBy(() -> new Subject(OWNER_ID, CATEGORY, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 128 characters");
  }

  @Test
  void constructorAcceptsValueAt128Characters() {
    String maxLength = "x".repeat(128);

    Subject subject = new Subject(OWNER_ID, CATEGORY, maxLength);

    assertThat(subject.ownerIdentifier()).isEqualTo(OWNER_ID);
    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo(maxLength);
    assertThat(subject.value()).hasSize(128);
  }

  @Test
  void constructorStripsLeadingAndTrailingWhitespace() {
    Subject subject = new Subject(OWNER_ID, CATEGORY, "  deploy checklist  ");

    assertThat(subject.value()).isEqualTo("deploy checklist");
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Subject subject = new Subject(OWNER_ID, CATEGORY, "test");

    assertThat(subject.identifier()).isNotNull();
    assertThat(subject.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Subject subject = new Subject(OWNER_ID, CATEGORY, "test", id);

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
    Subject original = new Subject(OWNER_ID, CATEGORY, "original", id);

    Subject copy = Subject.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingIdentifier() {
    Subject original = new Subject(OWNER_ID, CATEGORY, "test");
    Identifier newId = new Identifier();

    Subject modified = Subject.from(original)
        .identifier(newId)
        .build();

    assertThat(modified.ownerIdentifier()).isEqualTo(original.ownerIdentifier());
    assertThat(modified.category()).isEqualTo(original.category());
    assertThat(modified.value()).isEqualTo(original.value());
    assertThat(modified.identifier()).isSameAs(newId);
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> Subject.builder().category(CATEGORY).value("value").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void builderRejectsNullCategory() {
    assertThatThrownBy(() -> Subject.builder().ownerIdentifier(OWNER_ID).value("value").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void builderRejectsNullValue() {
    assertThatThrownBy(() -> Subject.builder().ownerIdentifier(OWNER_ID).category(CATEGORY).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void builderWithDefaults() {
    Subject subject = Subject.builder()
        .ownerIdentifier(OWNER_ID).category(CATEGORY).value("test").build();

    assertThat(subject.ownerIdentifier()).isEqualTo(OWNER_ID);
    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("test");
    assertThat(subject.identifier()).isNotNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();

    Subject subject = Subject.builder()
        .ownerIdentifier(OWNER_ID).category(CATEGORY).value("test")
        .identifier(id)
        .build();

    assertThat(subject.ownerIdentifier()).isEqualTo(OWNER_ID);
    assertThat(subject.category()).isEqualTo(CATEGORY);
    assertThat(subject.value()).isEqualTo("test");
    assertThat(subject.identifier()).isSameAs(id);
  }
}
