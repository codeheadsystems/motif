package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubjectTest {

  @Test
  void constructorRejectsNullCategory() {
    assertThatThrownBy(() -> new Subject(null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    Category category = new Category("topic");

    assertThatThrownBy(() -> new Subject(category, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    Category category = new Category("topic");

    assertThatThrownBy(() -> new Subject(category, "   \t   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan128Characters() {
    Category category = new Category("topic");
    String tooLong = "x".repeat(129);

    assertThatThrownBy(() -> new Subject(category, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 128 characters");
  }

  @Test
  void constructorAcceptsValueAt128Characters() {
    Category category = new Category("topic");
    String maxLength = "x".repeat(128);

    Subject subject = new Subject(category, maxLength);

    assertThat(subject.category()).isEqualTo(category);
    assertThat(subject.value()).isEqualTo(maxLength);
    assertThat(subject.value()).hasSize(128);
  }

  @Test
  void constructorStripsLeadingAndTrailingWhitespace() {
    Category category = new Category("topic");

    Subject subject = new Subject(category, "  deploy checklist  ");

    assertThat(subject.category()).isEqualTo(category);
    assertThat(subject.value()).isEqualTo("deploy checklist");
  }
}

