package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CategoryTest {

  private static final Identifier OWNER_ID = new Identifier();

  @Test
  void rejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> Category.builder().name("Home").color("#3B82F6").icon("house").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void rejectsNullName() {
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).color("#3B82F6").icon("house").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("name cannot be null");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).name("   ").color("#3B82F6").icon("house").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name cannot be empty");
  }

  @Test
  void rejectsNameLongerThan128Chars() {
    String tooLong = "x".repeat(129);
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).name(tooLong).color("#3B82F6").icon("house").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name cannot be longer than 128 characters");
  }

  @Test
  void rejectsMalformedHexColor() {
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("blue").icon("house").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("color must be #RRGGBB hex");
  }

  @Test
  void rejectsShortHexColor() {
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#FFF").icon("house").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("color must be #RRGGBB hex");
  }

  @Test
  void acceptsValidHexColor() {
    Category category = Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#3B82F6").icon("house").build();
    assertThat(category.color()).isEqualTo("#3B82F6");
  }

  @Test
  void acceptsLowercaseHexColor() {
    Category category = Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#abcdef").icon("house").build();
    assertThat(category.color()).isEqualTo("#abcdef");
  }

  @Test
  void rejectsBlankIcon() {
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#3B82F6").icon(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("icon cannot be empty");
  }

  @Test
  void rejectsIconLongerThan64Chars() {
    String tooLong = "x".repeat(65);
    assertThatThrownBy(() -> Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#3B82F6").icon(tooLong).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("icon cannot be longer than 64 characters");
  }

  @Test
  void stripsNameAndIcon() {
    Category category = Category.builder().ownerIdentifier(OWNER_ID).name("  Home  ").color("#3B82F6").icon(" house ").build();
    assertThat(category.name()).isEqualTo("Home");
    assertThat(category.icon()).isEqualTo("house");
  }

  @Test
  void defaultsIdentifierWhenAbsent() {
    Category category = Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#3B82F6").icon("house").build();
    assertThat(category.identifier()).isNotNull();
  }

  @Test
  void preservesProvidedIdentifier() {
    Identifier id = new Identifier();
    Category category = Category.builder().ownerIdentifier(OWNER_ID).identifier(id).name("Home").color("#3B82F6").icon("house").build();
    assertThat(category.identifier()).isSameAs(id);
  }

  @Test
  void fromCopiesAllFields() {
    Category original = Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#3B82F6").icon("house").build();
    Category copy = Category.from(original).build();
    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsFieldOverride() {
    Category original = Category.builder().ownerIdentifier(OWNER_ID).name("Home").color("#3B82F6").icon("house").build();
    Category modified = Category.from(original).color("#FF0000").build();
    assertThat(modified.color()).isEqualTo("#FF0000");
    assertThat(modified.name()).isEqualTo(original.name());
    assertThat(modified.identifier()).isEqualTo(original.identifier());
  }
}
