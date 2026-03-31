package com.codeheadsystems.motif.server.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CategoryTest {

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Category(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    assertThatThrownBy(() -> new Category("   \t   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan128Characters() {
    String tooLong = "x".repeat(129);

    assertThatThrownBy(() -> new Category(tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 128 characters");
  }

  @Test
  void constructorAcceptsValueAt128Characters() {
    String maxLength = "x".repeat(128);

    Category category = new Category(maxLength);

    assertThat(category.value()).isEqualTo(maxLength);
    assertThat(category.value()).hasSize(128);
  }

  @Test
  void constructorStripsLeadingAndTrailingWhitespace() {
    Category category = new Category("  important topic  ");

    assertThat(category.value()).isEqualTo("important topic");
  }
}

