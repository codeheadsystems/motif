package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PatternTest {

  private static final Identifier OWNER_ID = new Identifier();
  private static final Identifier SUBJECT_ID = new Identifier();
  private static final Timestamp NOW = new Timestamp(Instant.parse("2026-04-25T00:00:00Z"));
  private static final Timestamp NEXT = new Timestamp(Instant.parse("2026-05-02T00:00:00Z"));

  private Pattern.Builder validBuilder() {
    return Pattern.builder()
        .ownerIdentifier(OWNER_ID).subjectIdentifier(SUBJECT_ID).eventValue("watered")
        .period(PeriodClassification.WEEKLY).intervalMeanSeconds(7L * 86400)
        .occurrenceCount(4).confidence(0.95)
        .lastSeenAt(NOW).nextExpectedAt(NEXT).score(3.8).detectedAt(NOW);
  }

  @Test
  void rejectsNullOwner() {
    assertThatThrownBy(() -> validBuilder().ownerIdentifier(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void rejectsBlankEventValue() {
    assertThatThrownBy(() -> validBuilder().eventValue("   ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("eventValue cannot be empty");
  }

  @Test
  void rejectsNonPositiveInterval() {
    assertThatThrownBy(() -> validBuilder().intervalMeanSeconds(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("intervalMeanSeconds must be positive");
  }

  @Test
  void rejectsBelowMinOccurrences() {
    assertThatThrownBy(() -> validBuilder().occurrenceCount(2).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("occurrenceCount must be >= 3 (detector threshold)");
  }

  @Test
  void rejectsConfidenceOutOfRange() {
    assertThatThrownBy(() -> validBuilder().confidence(1.5).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("confidence must be in [0,1]");
  }

  @Test
  void buildsAndAutoGeneratesIdentifier() {
    Pattern pattern = validBuilder().build();
    assertThat(pattern.identifier()).isNotNull();
    assertThat(pattern.identifier().uuid()).isNotNull();
  }

  @Test
  void fromCopiesAllFields() {
    Pattern original = validBuilder().build();
    Pattern copy = Pattern.from(original).build();
    assertThat(copy).isEqualTo(original);
  }

  @Test
  void periodClassificationFromMeanInterval() {
    assertThat(PeriodClassification.fromMeanInterval(java.time.Duration.ofDays(1)))
        .isEqualTo(PeriodClassification.DAILY);
    assertThat(PeriodClassification.fromMeanInterval(java.time.Duration.ofDays(7)))
        .isEqualTo(PeriodClassification.WEEKLY);
    assertThat(PeriodClassification.fromMeanInterval(java.time.Duration.ofDays(30)))
        .isEqualTo(PeriodClassification.MONTHLY);
    assertThat(PeriodClassification.fromMeanInterval(java.time.Duration.ofDays(100)))
        .isEqualTo(PeriodClassification.OTHER);
  }
}
