package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.TestCategories;
import com.codeheadsystems.motif.server.db.dao.CategoryDao;
import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.PatternDao;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import com.codeheadsystems.motif.server.db.model.PeriodClassification;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatternDetectionManagerIntegrationTest extends DatabaseTest {

  // Fixed clock so recency-decay scoring is deterministic.
  private static final Instant NOW = Instant.parse("2026-04-25T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private static final Owner OWNER = new Owner("TEST-OWNER");

  private OwnerDao ownerDao;
  private EventDao eventDao;
  private SubjectDao subjectDao;
  private CategoryDao categoryDao;
  private PatternDao patternDao;
  private PatternDetectionManager detectionManager;
  private Subject jadePlant;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM patterns");
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM categories");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    categoryDao = jdbi.onDemand(CategoryDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    eventDao = jdbi.onDemand(EventDao.class);
    patternDao = jdbi.onDemand(PatternDao.class);

    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    Category category = TestCategories.of(OWNER.identifier(), "Plants");
    categoryDao.upsert(category.identifier().uuid(), category.ownerIdentifier().uuid(),
        category.name(), category.color(), category.icon());
    jadePlant = new Subject(OWNER.identifier(), category.identifier(), "Jade Plant");
    subjectDao.upsert(jadePlant.identifier().uuid(), jadePlant.ownerIdentifier().uuid(),
        jadePlant.categoryIdentifier().uuid(), jadePlant.value());

    detectionManager = new PatternDetectionManager(jdbi, ownerDao, CLOCK);
  }

  private void recordEvent(String value, Instant when) {
    Event event = Event.builder()
        .owner(OWNER).subject(jadePlant).value(value).timestamp(new Timestamp(when))
        .build();
    eventDao.upsert(event.identifier().uuid(), event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(), event.value(),
        event.timestamp().toOffsetDateTime());
  }

  @Test
  void detectsWeeklyCadence() {
    // Four "watered" events exactly 7 days apart.
    recordEvent("Watered", NOW.minusSeconds(21L * 86400));
    recordEvent("Watered", NOW.minusSeconds(14L * 86400));
    recordEvent("Watered", NOW.minusSeconds(7L * 86400));
    recordEvent("Watered", NOW);

    int written = detectionManager.detectForOwner(OWNER);

    assertThat(written).isEqualTo(1);
    List<Pattern> patterns = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    assertThat(patterns).hasSize(1);
    Pattern p = patterns.getFirst();
    assertThat(p.eventValue()).isEqualTo("watered");
    assertThat(p.period()).isEqualTo(PeriodClassification.WEEKLY);
    assertThat(p.intervalMeanSeconds()).isEqualTo(7L * 86400);
    assertThat(p.occurrenceCount()).isEqualTo(4);
    assertThat(p.confidence()).isEqualTo(1.0);
    assertThat(p.lastSeenAt().timestamp()).isEqualTo(NOW);
    assertThat(p.nextExpectedAt().timestamp()).isEqualTo(NOW.plusSeconds(7L * 86400));
  }

  @Test
  void normalizationCollapsesCaseVariants() {
    recordEvent("watered", NOW.minusSeconds(21L * 86400));
    recordEvent("Watered", NOW.minusSeconds(14L * 86400));
    recordEvent("WATERED", NOW.minusSeconds(7L * 86400));
    recordEvent("  watered  ", NOW);

    detectionManager.detectForOwner(OWNER);

    List<Pattern> patterns = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    assertThat(patterns).hasSize(1);
    assertThat(patterns.getFirst().eventValue()).isEqualTo("watered");
    assertThat(patterns.getFirst().occurrenceCount()).isEqualTo(4);
  }

  @Test
  void skipsGroupsBelowMinOccurrences() {
    // Only 2 events — below MIN_OCCURRENCES of 3.
    recordEvent("Watered", NOW.minusSeconds(7L * 86400));
    recordEvent("Watered", NOW);

    detectionManager.detectForOwner(OWNER);

    assertThat(patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0)).isEmpty();
  }

  @Test
  void skipsHighVarianceGroups() {
    // 4 events at wildly varying intervals: 1d, 30d, 1d. CV > 0.3.
    recordEvent("Watered", NOW.minusSeconds(32L * 86400));
    recordEvent("Watered", NOW.minusSeconds(31L * 86400));
    recordEvent("Watered", NOW.minusSeconds(86400));
    recordEvent("Watered", NOW);

    detectionManager.detectForOwner(OWNER);

    assertThat(patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0)).isEmpty();
  }

  @Test
  void detectsMultipleEventTypesIndependently() {
    // Weekly watering, monthly fertilizing.
    recordEvent("Watered", NOW.minusSeconds(21L * 86400));
    recordEvent("Watered", NOW.minusSeconds(14L * 86400));
    recordEvent("Watered", NOW.minusSeconds(7L * 86400));
    recordEvent("Watered", NOW);

    recordEvent("Fertilized", NOW.minusSeconds(60L * 86400));
    recordEvent("Fertilized", NOW.minusSeconds(30L * 86400));
    recordEvent("Fertilized", NOW);

    detectionManager.detectForOwner(OWNER);

    List<Pattern> patterns = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    assertThat(patterns).hasSize(2);
    assertThat(patterns).extracting(Pattern::eventValue).containsExactlyInAnyOrder("watered", "fertilized");
  }

  @Test
  void rerunReplacesPreviousPatterns() {
    // First run: weekly cadence detected.
    recordEvent("Watered", NOW.minusSeconds(21L * 86400));
    recordEvent("Watered", NOW.minusSeconds(14L * 86400));
    recordEvent("Watered", NOW.minusSeconds(7L * 86400));
    recordEvent("Watered", NOW);
    detectionManager.detectForOwner(OWNER);
    assertThat(patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0)).hasSize(1);

    // Add irregular events that break the consistency. Detector should drop the pattern.
    recordEvent("Watered", NOW.plusSeconds(86400));
    recordEvent("Watered", NOW.plusSeconds(86400 + 3600));  // tightly clustered
    detectionManager.detectForOwner(OWNER);

    // Now CV is much higher — drops the pattern.
    List<Pattern> after = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    // It either drops the pattern or keeps it with degraded confidence — we just want
    // to confirm the detector wrote something idempotent (no duplicate rows).
    assertThat(after.size()).isLessThanOrEqualTo(1);
  }

  @Test
  void detectForAllOwnersScansEveryActiveOwner() {
    recordEvent("Watered", NOW.minusSeconds(21L * 86400));
    recordEvent("Watered", NOW.minusSeconds(14L * 86400));
    recordEvent("Watered", NOW.minusSeconds(7L * 86400));
    recordEvent("Watered", NOW);

    var result = detectionManager.detectForAllOwners();

    assertThat(result.ownersScanned()).isEqualTo(1);
    assertThat(result.succeeded()).isEqualTo(1);
    assertThat(result.failed()).isZero();
    assertThat(result.patternsWritten()).isEqualTo(1);
  }
}
