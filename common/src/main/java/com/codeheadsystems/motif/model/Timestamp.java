package com.codeheadsystems.motif.model;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * A timestamp backed by an Instant. Supports ISO-8601 export and import.
 *
 * @param timestamp The instant.
 */
public record Timestamp(Instant timestamp) implements Comparable<Timestamp> {

  public Timestamp {
    if (timestamp == null) {
      timestamp = Clock.systemUTC().instant();
    }
  }

  public Timestamp() {
    this((Instant) null);
  }

  /**
   * Creates a Timestamp from an ISO-8601 formatted string.
   *
   * @param iso the ISO-8601 string (e.g. "2026-03-28T12:30:00Z").
   * @throws DateTimeParseException   if the string is not valid ISO-8601.
   * @throws IllegalArgumentException if the string is null.
   */
  public Timestamp(String iso) {
    this(Instant.parse(Objects.requireNonNull(iso, "iso string cannot be null")));
  }

  /**
   * Exports this timestamp as an ISO-8601 formatted string.
   *
   * @return the ISO-8601 representation.
   */
  public String toIso() {
    return timestamp.toString();
  }

  /**
   * Returns this timestamp as an OffsetDateTime in UTC.
   *
   * @return the UTC OffsetDateTime representation.
   */
  public OffsetDateTime toOffsetDateTime() {
    return timestamp.atOffset(ZoneOffset.UTC);
  }

  @Override
  public int compareTo(Timestamp other) {
    return this.timestamp.compareTo(other.timestamp);
  }

}
