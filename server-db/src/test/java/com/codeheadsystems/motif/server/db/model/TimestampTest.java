package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class TimestampTest {

  @Test
  void constructorWithNullDefaultsToNow() {
    Instant before = Instant.now();
    Timestamp timestamp = new Timestamp((Instant) null);
    Instant after = Instant.now();

    assertThat(timestamp.timestamp()).isBetween(before, after);
  }

  @Test
  void toIsoProducesIso8601String() {
    Instant instant = Instant.parse("2026-03-28T12:30:00Z");
    Timestamp timestamp = new Timestamp(instant);

    assertThat(timestamp.toIso()).isEqualTo("2026-03-28T12:30:00Z");
  }

  @Test
  void fromIsoCreatesTimestamp() {
    Timestamp timestamp = new Timestamp("2026-03-28T12:30:00Z");

    assertThat(timestamp.timestamp()).isEqualTo(Instant.parse("2026-03-28T12:30:00Z"));
  }

  @Test
  void roundTripPreservesValue() {
    Instant original = Instant.parse("2025-01-15T08:45:30.123456789Z");
    Timestamp timestamp = new Timestamp(original);

    Timestamp roundTripped = new Timestamp(timestamp.toIso());

    assertThat(roundTripped).isEqualTo(timestamp);
  }

  @Test
  void fromIsoRejectsNull() {
    assertThatThrownBy(() -> new Timestamp((String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("iso string cannot be null");
  }

  @Test
  void fromIsoRejectsInvalidFormat() {
    assertThatThrownBy(() -> new Timestamp("not-a-timestamp"))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void fromIsoHandlesOffsetFormat() {
    Timestamp timestamp = new Timestamp("2026-03-28T12:30:00.000Z");

    assertThat(timestamp.timestamp()).isEqualTo(Instant.parse("2026-03-28T12:30:00Z"));
  }

  @Test
  void compareToOrdersByInstant() {
    Timestamp earlier = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    Timestamp later = new Timestamp(Instant.parse("2026-06-01T00:00:00Z"));

    assertThat(earlier).isLessThan(later);
    assertThat(later).isGreaterThan(earlier);
    assertThat(earlier.compareTo(new Timestamp(earlier.timestamp()))).isZero();
  }
}
