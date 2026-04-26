package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.TestCategories;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import com.codeheadsystems.motif.server.db.model.PeriodClassification;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatternDaoTest extends DatabaseTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");

  private OwnerDao ownerDao;
  private CategoryDao categoryDao;
  private SubjectDao subjectDao;
  private PatternDao patternDao;
  private Subject subject;

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
    patternDao = jdbi.onDemand(PatternDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    Category category = TestCategories.of(OWNER.identifier(), "test");
    categoryDao.upsert(category.identifier().uuid(), category.ownerIdentifier().uuid(),
        category.name(), category.color(), category.icon());
    subject = new Subject(OWNER.identifier(), category.identifier(), "Jade Plant");
    subjectDao.upsert(subject.identifier().uuid(), subject.ownerIdentifier().uuid(),
        subject.categoryIdentifier().uuid(), subject.value());
  }

  private Pattern weeklyPattern(String eventValue, double score) {
    Instant now = Instant.parse("2026-04-25T00:00:00Z");
    return Pattern.builder()
        .owner(OWNER).subject(subject).eventValue(eventValue)
        .period(PeriodClassification.WEEKLY).intervalMeanSeconds(7L * 86400)
        .occurrenceCount(4).confidence(0.95)
        .lastSeenAt(new Timestamp(now)).nextExpectedAt(new Timestamp(now.plusSeconds(7L * 86400)))
        .score(score).detectedAt(new Timestamp(now))
        .build();
  }

  private void store(Pattern p) {
    patternDao.upsert(p.identifier().uuid(), p.ownerIdentifier().uuid(), p.subjectIdentifier().uuid(),
        p.eventValue(), p.period().name(), p.intervalMeanSeconds(), p.occurrenceCount(),
        p.confidence(), p.lastSeenAt().toOffsetDateTime(), p.nextExpectedAt().toOffsetDateTime(),
        p.score(), p.detectedAt().toOffsetDateTime());
  }

  @Test
  void upsertAndFind() {
    Pattern pattern = weeklyPattern("watered", 3.8);
    store(pattern);

    List<Pattern> results = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().eventValue()).isEqualTo("watered");
    assertThat(results.getFirst().period()).isEqualTo(PeriodClassification.WEEKLY);
    assertThat(results.getFirst().confidence()).isEqualTo(0.95);
  }

  @Test
  void upsertReplacesByUniqueKey() {
    Pattern original = weeklyPattern("watered", 3.0);
    store(original);

    Pattern updated = Pattern.from(original).score(5.0).confidence(0.99).build();
    store(updated);

    List<Pattern> results = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().score()).isEqualTo(5.0);
  }

  @Test
  void findByOwnerOrdersByScoreDescending() {
    store(weeklyPattern("watered", 3.0));
    store(weeklyPattern("pruned", 7.0));
    store(weeklyPattern("fertilized", 1.5));

    List<Pattern> results = patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0);
    assertThat(results).extracting(Pattern::eventValue).containsExactly("pruned", "watered", "fertilized");
  }

  @Test
  void deleteByOwnerWipesAllForOwner() {
    store(weeklyPattern("watered", 3.0));
    store(weeklyPattern("pruned", 5.0));
    assertThat(patternDao.deleteByOwner(OWNER.identifier().uuid())).isEqualTo(2);
    assertThat(patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0)).isEmpty();
  }

  @Test
  void patternsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);
    Category otherCat = TestCategories.of(other.identifier(), "test");
    categoryDao.upsert(otherCat.identifier().uuid(), otherCat.ownerIdentifier().uuid(),
        otherCat.name(), otherCat.color(), otherCat.icon());
    Subject otherSubject = new Subject(other.identifier(), otherCat.identifier(), "Other Plant");
    subjectDao.upsert(otherSubject.identifier().uuid(), otherSubject.ownerIdentifier().uuid(),
        otherSubject.categoryIdentifier().uuid(), otherSubject.value());

    store(weeklyPattern("watered", 3.0));
    Pattern otherPattern = Pattern.builder()
        .owner(other).subject(otherSubject).eventValue("watered")
        .period(PeriodClassification.WEEKLY).intervalMeanSeconds(7L * 86400)
        .occurrenceCount(3).confidence(0.9)
        .lastSeenAt(new Timestamp()).nextExpectedAt(new Timestamp(Instant.now().plusSeconds(7L * 86400)))
        .score(2.0).detectedAt(new Timestamp())
        .build();
    store(otherPattern);

    assertThat(patternDao.findByOwner(OWNER.identifier().uuid(), 10, 0)).hasSize(1);
    assertThat(patternDao.findByOwner(other.identifier().uuid(), 10, 0)).hasSize(1);
  }
}
