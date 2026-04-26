package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WorkflowStepTest {

  @Test
  void rejectsPositionBelowOne() {
    assertThatThrownBy(() -> new WorkflowStep(null, 0, "x", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("position must be >= 1");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> new WorkflowStep(null, 1, "  ", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name cannot be empty");
  }

  @Test
  void rejectsNonPositiveDuration() {
    assertThatThrownBy(() -> new WorkflowStep(null, 1, "x", 0L, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("expectedDurationSeconds must be positive when present");
  }

  @Test
  void blankNotesBecomeNull() {
    WorkflowStep step = new WorkflowStep(null, 1, "x", null, "   ");
    assertThat(step.notes()).isNull();
  }

  @Test
  void rejectsNotesLongerThan2048Chars() {
    String tooLong = "x".repeat(2049);
    assertThatThrownBy(() -> new WorkflowStep(null, 1, "x", null, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("notes cannot be longer than 2048 characters");
  }

  @Test
  void autoGeneratesIdentifier() {
    WorkflowStep step = new WorkflowStep(null, 1, "x", null, null);
    assertThat(step.identifier()).isNotNull();
  }
}
