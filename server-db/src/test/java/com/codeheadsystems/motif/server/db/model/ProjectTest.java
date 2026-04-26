package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProjectTest {

  private static final Identifier OWNER_ID = new Identifier();

  private Project.Builder validBuilder() {
    return Project.builder()
        .ownerIdentifier(OWNER_ID).name("Kitchen Renovation")
        .description("Down to the studs by spring")
        .status(ProjectStatus.ACTIVE);
  }

  @Test
  void rejectsNullOwner() {
    assertThatThrownBy(() -> Project.builder().name("X").status(ProjectStatus.ACTIVE).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> validBuilder().name("   ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name cannot be empty");
  }

  @Test
  void rejectsNameLongerThan128Chars() {
    String tooLong = "x".repeat(129);
    assertThatThrownBy(() -> validBuilder().name(tooLong).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name cannot be longer than 128 characters");
  }

  @Test
  void rejectsDescriptionLongerThan2048Chars() {
    String tooLong = "x".repeat(2049);
    assertThatThrownBy(() -> validBuilder().description(tooLong).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("description cannot be longer than 2048 characters");
  }

  @Test
  void blankDescriptionBecomesNull() {
    Project p = validBuilder().description("   ").build();
    assertThat(p.description()).isNull();
  }

  @Test
  void buildAutoGeneratesIdentifierAndTimestamps() {
    Project p = validBuilder().build();
    assertThat(p.identifier()).isNotNull();
    assertThat(p.createdAt()).isNotNull();
    assertThat(p.updatedAt()).isNotNull();
  }

  @Test
  void fromCopiesAllFields() {
    Project original = validBuilder().build();
    Project copy = Project.from(original).build();
    assertThat(copy).isEqualTo(original);
  }
}
