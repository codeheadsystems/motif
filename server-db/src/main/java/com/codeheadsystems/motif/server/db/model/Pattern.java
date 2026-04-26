package com.codeheadsystems.motif.server.db.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A periodic pattern detected by PatternDetectionManager for a single owner: an event-value
 * (e.g. "watered") that recurs on a subject (e.g. "Jade Plant") with statistically consistent
 * intervals.
 *
 * <p>Lifecycle: detector runs replace the owner's pattern set (delete then insert), so a
 * Pattern row's {@code detectedAt} reflects the most recent run that confirmed it.
 *
 * @param ownerIdentifier      owner this pattern belongs to
 * @param subjectIdentifier    subject the pattern is on
 * @param eventValue           normalized event value (lowercase, stripped)
 * @param period               classification (DAILY/WEEKLY/MONTHLY/OTHER)
 * @param intervalMeanSeconds  mean inter-arrival interval in seconds
 * @param occurrenceCount      number of events that produced this pattern
 * @param confidence           0..1, derived from coefficient of variation
 * @param lastSeenAt           timestamp of the most recent occurrence
 * @param nextExpectedAt       lastSeenAt + intervalMean
 * @param score                composite ranking; higher = more prominent
 * @param detectedAt           when the detector run that produced this row completed
 * @param identifier           stable identifier (auto-generated if null)
 */
public record Pattern(
    Identifier ownerIdentifier,
    Identifier subjectIdentifier,
    String eventValue,
    PeriodClassification period,
    long intervalMeanSeconds,
    int occurrenceCount,
    double confidence,
    Timestamp lastSeenAt,
    Timestamp nextExpectedAt,
    double score,
    Timestamp detectedAt,
    @Nullable Identifier identifier) {

  public Pattern {
    Objects.requireNonNull(ownerIdentifier, "ownerIdentifier cannot be null");
    Objects.requireNonNull(subjectIdentifier, "subjectIdentifier cannot be null");
    eventValue = Objects.requireNonNull(eventValue, "eventValue cannot be null").strip();
    if (eventValue.isEmpty()) {
      throw new IllegalArgumentException("eventValue cannot be empty");
    }
    if (eventValue.length() > 256) {
      throw new IllegalArgumentException("eventValue cannot be longer than 256 characters");
    }
    Objects.requireNonNull(period, "period cannot be null");
    if (intervalMeanSeconds <= 0) {
      throw new IllegalArgumentException("intervalMeanSeconds must be positive");
    }
    if (occurrenceCount < 3) {
      throw new IllegalArgumentException("occurrenceCount must be >= 3 (detector threshold)");
    }
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be in [0,1]");
    }
    Objects.requireNonNull(lastSeenAt, "lastSeenAt cannot be null");
    Objects.requireNonNull(nextExpectedAt, "nextExpectedAt cannot be null");
    Objects.requireNonNull(detectedAt, "detectedAt cannot be null");
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
  }

  public static Builder from(Pattern pattern) {
    return Builder.from(pattern);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Identifier ownerIdentifier;
    private Identifier subjectIdentifier;
    private String eventValue;
    private PeriodClassification period;
    private long intervalMeanSeconds;
    private int occurrenceCount;
    private double confidence;
    private Timestamp lastSeenAt;
    private Timestamp nextExpectedAt;
    private double score;
    private Timestamp detectedAt;
    private Identifier identifier;

    private Builder() {}

    private static Builder from(Pattern pattern) {
      Objects.requireNonNull(pattern, "pattern cannot be null");
      Builder b = new Builder();
      b.ownerIdentifier = pattern.ownerIdentifier();
      b.subjectIdentifier = pattern.subjectIdentifier();
      b.eventValue = pattern.eventValue();
      b.period = pattern.period();
      b.intervalMeanSeconds = pattern.intervalMeanSeconds();
      b.occurrenceCount = pattern.occurrenceCount();
      b.confidence = pattern.confidence();
      b.lastSeenAt = pattern.lastSeenAt();
      b.nextExpectedAt = pattern.nextExpectedAt();
      b.score = pattern.score();
      b.detectedAt = pattern.detectedAt();
      b.identifier = pattern.identifier();
      return b;
    }

    public Builder ownerIdentifier(Identifier v) { this.ownerIdentifier = v; return this; }
    public Builder owner(Owner v) { return ownerIdentifier(v.identifier()); }
    public Builder subjectIdentifier(Identifier v) { this.subjectIdentifier = v; return this; }
    public Builder subject(Subject v) { return subjectIdentifier(v.identifier()); }
    public Builder eventValue(String v) { this.eventValue = v; return this; }
    public Builder period(PeriodClassification v) { this.period = v; return this; }
    public Builder intervalMeanSeconds(long v) { this.intervalMeanSeconds = v; return this; }
    public Builder occurrenceCount(int v) { this.occurrenceCount = v; return this; }
    public Builder confidence(double v) { this.confidence = v; return this; }
    public Builder lastSeenAt(Timestamp v) { this.lastSeenAt = v; return this; }
    public Builder nextExpectedAt(Timestamp v) { this.nextExpectedAt = v; return this; }
    public Builder score(double v) { this.score = v; return this; }
    public Builder detectedAt(Timestamp v) { this.detectedAt = v; return this; }
    public Builder identifier(Identifier v) { this.identifier = v; return this; }

    public Pattern build() {
      return new Pattern(ownerIdentifier, subjectIdentifier, eventValue, period,
          intervalMeanSeconds, occurrenceCount, confidence,
          lastSeenAt, nextExpectedAt, score, detectedAt, identifier);
    }
  }
}
