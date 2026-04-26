package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.PatternDao;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import com.codeheadsystems.motif.server.db.model.PeriodClassification;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * v1 frequency-based pattern detector. For each owner, groups their events by
 * (subject, normalized event-value) and flags groups whose inter-arrival intervals
 * are statistically consistent (low coefficient of variation). See
 * docs/technical_architecture.md → "Pattern Detection Algorithm" for the spec.
 *
 * <p>Re-detection is replace-on-rerun: a single transaction deletes the owner's
 * existing patterns and inserts the freshly computed set. A pattern that no longer
 * meets the thresholds simply disappears — no fading or history.
 *
 * <p>Thread-safety: stateless. Safe to invoke concurrently for different owners.
 */
@Singleton
public class PatternDetectionManager {

  private static final Logger log = LoggerFactory.getLogger(PatternDetectionManager.class);
  private static final Logger AUDIT = LoggerFactory.getLogger("audit.pattern");

  // Tunables (matching docs/technical_architecture.md):
  static final int MIN_OCCURRENCES = 3;
  static final double MAX_COEFFICIENT_OF_VARIATION = 0.3;
  static final double MIN_CYCLE_COVERAGE = 2.0;

  private final Jdbi jdbi;
  private final OwnerDao ownerDao;
  private final Clock clock;

  @Inject
  public PatternDetectionManager(final Jdbi jdbi, final OwnerDao ownerDao) {
    this(jdbi, ownerDao, Clock.systemUTC());
  }

  /** Test-only constructor that lets tests inject a fixed clock. */
  PatternDetectionManager(final Jdbi jdbi, final OwnerDao ownerDao, final Clock clock) {
    this.jdbi = jdbi;
    this.ownerDao = ownerDao;
    this.clock = clock;
  }

  /**
   * Runs the detector for every active owner, swallowing per-owner exceptions so one
   * misbehaving owner doesn't poison the whole sweep. Counts are returned for logging.
   */
  public DetectorRunResult detectForAllOwners() {
    Instant start = clock.instant();
    List<Owner> owners = ownerDao.findAllActive();
    int succeeded = 0;
    int failed = 0;
    int patternsWritten = 0;
    for (Owner owner : owners) {
      try {
        patternsWritten += detectForOwner(owner);
        succeeded++;
      } catch (RuntimeException e) {
        failed++;
        log.error("Pattern detection failed for owner {}: {}",
            owner.identifier().uuid(), e.getMessage(), e);
      }
    }
    Duration elapsed = Duration.between(start, clock.instant());
    log.info("pattern-detection-sweep owners={} succeeded={} failed={} patterns={} elapsedMs={}",
        owners.size(), succeeded, failed, patternsWritten, elapsed.toMillis());
    return new DetectorRunResult(owners.size(), succeeded, failed, patternsWritten, elapsed);
  }

  /**
   * Runs the detector for a single owner. Returns the number of patterns written.
   * Idempotent: a second call with no new events produces the same patterns.
   */
  public int detectForOwner(Owner owner) {
    List<Pattern> patterns = computePatternsFor(owner);
    return jdbi.inTransaction(handle -> {
      PatternDao txDao = handle.attach(PatternDao.class);
      txDao.deleteByOwner(owner.identifier().uuid());
      for (Pattern p : patterns) {
        txDao.upsert(
            p.identifier().uuid(),
            p.ownerIdentifier().uuid(),
            p.subjectIdentifier().uuid(),
            p.eventValue(),
            p.period().name(),
            p.intervalMeanSeconds(),
            p.occurrenceCount(),
            p.confidence(),
            p.lastSeenAt().toOffsetDateTime(),
            p.nextExpectedAt().toOffsetDateTime(),
            p.score(),
            p.detectedAt().toOffsetDateTime());
      }
      AUDIT.info("pattern.detected owner={} patterns={}",
          owner.value(), patterns.size());
      return patterns.size();
    });
  }

  /**
   * Pure computation half: pulls events, groups, applies thresholds, returns the
   * resulting Pattern objects (not persisted). Public for testing.
   */
  public List<Pattern> computePatternsFor(Owner owner) {
    EventDao eventDao = jdbi.onDemand(EventDao.class);
    List<Event> events = eventDao.findAllByOwnerOrdered(owner.identifier().uuid());
    if (events.size() < MIN_OCCURRENCES) {
      return List.of();
    }

    // Group by (subject_uuid, normalized event-value) preserving timestamp order.
    Map<GroupKey, List<Instant>> grouped = new HashMap<>();
    for (Event event : events) {
      GroupKey key = new GroupKey(event.subject().identifier(), normalize(event.value()));
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(event.timestamp().timestamp());
    }

    Instant now = clock.instant();
    Timestamp detectedAt = new Timestamp(now);
    List<Pattern> result = new ArrayList<>();

    for (Map.Entry<GroupKey, List<Instant>> entry : grouped.entrySet()) {
      List<Instant> times = entry.getValue();
      if (times.size() < MIN_OCCURRENCES) continue;

      double[] intervalSecs = new double[times.size() - 1];
      for (int i = 1; i < times.size(); i++) {
        intervalSecs[i - 1] = Duration.between(times.get(i - 1), times.get(i)).getSeconds();
      }
      double mean = mean(intervalSecs);
      if (mean <= 0) continue;
      double cv = coefficientOfVariation(intervalSecs, mean);
      if (cv > MAX_COEFFICIENT_OF_VARIATION) continue;

      Instant first = times.get(0);
      Instant last = times.get(times.size() - 1);
      double coverage = Duration.between(first, last).getSeconds() / mean;
      if (coverage < MIN_CYCLE_COVERAGE) continue;

      long meanSeconds = (long) Math.round(mean);
      Duration meanDuration = Duration.ofSeconds(meanSeconds);
      PeriodClassification period = PeriodClassification.fromMeanInterval(meanDuration);
      double confidence = 1.0 - cv;
      // Recency: events long since the last occurrence weight less. Half-life = 2 expected cycles.
      double recencyDecay = Math.exp(
          -Duration.between(last, now).getSeconds() / (2.0 * mean));
      double score = times.size() * confidence * recencyDecay;

      Instant nextExpected = last.plusSeconds(meanSeconds);

      result.add(Pattern.builder()
          .ownerIdentifier(owner.identifier())
          .subjectIdentifier(entry.getKey().subjectIdentifier())
          .eventValue(entry.getKey().normalizedEventValue())
          .period(period)
          .intervalMeanSeconds(meanSeconds)
          .occurrenceCount(times.size())
          .confidence(confidence)
          .lastSeenAt(new Timestamp(last))
          .nextExpectedAt(new Timestamp(nextExpected))
          .score(score)
          .detectedAt(detectedAt)
          .build());
    }

    result.sort(Comparator.comparingDouble(Pattern::score).reversed());
    return result;
  }

  static String normalize(String eventValue) {
    return eventValue.strip().toLowerCase();
  }

  private static double mean(double[] xs) {
    double sum = 0;
    for (double x : xs) sum += x;
    return sum / xs.length;
  }

  private static double coefficientOfVariation(double[] xs, double mean) {
    if (mean == 0) return Double.POSITIVE_INFINITY;
    double sqSum = 0;
    for (double x : xs) {
      double d = x - mean;
      sqSum += d * d;
    }
    double stddev = Math.sqrt(sqSum / xs.length);
    return stddev / mean;
  }

  public record DetectorRunResult(int ownersScanned, int succeeded, int failed,
                                  int patternsWritten, Duration elapsed) {}

  private record GroupKey(com.codeheadsystems.motif.server.db.model.Identifier subjectIdentifier,
                           String normalizedEventValue) {}
}
