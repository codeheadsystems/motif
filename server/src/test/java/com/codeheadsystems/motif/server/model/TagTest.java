package com.codeheadsystems.motif.server.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TagTest {

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Tag(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsBlankValueAfterStrip() {
    assertThatThrownBy(() -> new Tag("   \n   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan32Characters() {
    String tooLong = "x".repeat(33);

    assertThatThrownBy(() -> new Tag(tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 32 characters");
  }

  @Test
  void constructorAcceptsValueAt32Characters() {
    String maxLengthLowercase = "x".repeat(32);

    Tag tag = new Tag(maxLengthLowercase);

    assertThat(tag.value()).isEqualTo("X".repeat(32));
    assertThat(tag.value()).hasSize(32);
  }

  @Test
  void constructorStripsWhitespaceAndUppercasesValue() {
    Tag tag = new Tag("  sprint-priority  ");

    assertThat(tag.value()).isEqualTo("SPRINT-PRIORITY");
  }
}

