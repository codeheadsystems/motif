package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.model.Identifier;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class TagsDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;

  private TagsDao dao;
  private UUID uuid;

  @BeforeAll
  static void setupJdbi() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());

    Flyway.configure()
        .dataSource(ds)
        .load()
        .migrate();

    jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());
  }

  @BeforeEach
  void setUp() {
    uuid = new Identifier().uuid();
    jdbi.useHandle(handle -> handle.execute("DELETE FROM tags"));
    dao = jdbi.onDemand(TagsDao.class);
  }

  @Test
  void tagValuesForReturnsEmptyListWhenNoTags() {
    assertThat(dao.tagValuesFor(uuid)).isEmpty();
  }

  @Test
  void insertTagAndRetrieve() {
    dao.insertTag(uuid, "ALPHA");
    dao.insertTag(uuid, "BETA");

    assertThat(dao.tagValuesFor(uuid))
        .containsExactlyInAnyOrder("ALPHA", "BETA");
  }

  @Test
  void insertTagIsIdempotent() {
    dao.insertTag(uuid, "DUPLICATE");
    dao.insertTag(uuid, "DUPLICATE");

    assertThat(dao.tagValuesFor(uuid)).containsExactly("DUPLICATE");
  }

  @Test
  void deleteTagReturnsOneWhenTagExists() {
    dao.insertTag(uuid, "A");
    dao.insertTag(uuid, "B");
    dao.insertTag(uuid, "C");

    assertThat(dao.deleteTag(uuid, "A")).isEqualTo(1);
    assertThat(dao.deleteTag(uuid, "C")).isEqualTo(1);
    assertThat(dao.tagValuesFor(uuid)).containsExactly("B");
  }

  @Test
  void deleteTagReturnsZeroWhenTagDoesNotExist() {
    assertThat(dao.deleteTag(uuid, "NONEXISTENT")).isEqualTo(0);
  }

  @Test
  void tagsAreIsolatedByUuid() {
    UUID uuid1 = new Identifier().uuid();
    UUID uuid2 = new Identifier().uuid();

    dao.insertTag(uuid1, "SHARED");
    dao.insertTag(uuid1, "ONLY1");
    dao.insertTag(uuid2, "SHARED");
    dao.insertTag(uuid2, "ONLY2");

    assertThat(dao.tagValuesFor(uuid1))
        .containsExactlyInAnyOrder("SHARED", "ONLY1");
    assertThat(dao.tagValuesFor(uuid2))
        .containsExactlyInAnyOrder("SHARED", "ONLY2");
  }

  @Test
  void deleteTagDoesNotAffectOtherUuids() {
    UUID uuid1 = new Identifier().uuid();
    UUID uuid2 = new Identifier().uuid();

    dao.insertTag(uuid1, "TAG");
    dao.insertTag(uuid2, "TAG");

    dao.deleteTag(uuid1, "TAG");

    assertThat(dao.tagValuesFor(uuid1)).isEmpty();
    assertThat(dao.tagValuesFor(uuid2)).containsExactly("TAG");
  }

  // --- batch ---

  @Test
  void tagValuesForBatchReturnsEntriesForMultipleUuids() {
    UUID uuid1 = new Identifier().uuid();
    UUID uuid2 = new Identifier().uuid();
    UUID uuid3 = new Identifier().uuid();

    dao.insertTag(uuid1, "A");
    dao.insertTag(uuid1, "B");
    dao.insertTag(uuid2, "C");
    // uuid3 has no tags

    List<TagEntry> result = dao.tagValuesForBatch(new UUID[]{uuid1, uuid2, uuid3});

    assertThat(result).hasSize(3);
    assertThat(result).extracting(TagEntry::uuid)
        .containsOnly(uuid1, uuid2);
    assertThat(result).extracting(TagEntry::tagValue)
        .containsExactlyInAnyOrder("A", "B", "C");
  }

  @Test
  void tagValuesForBatchReturnsEmptyForEmptyArray() {
    List<TagEntry> result = dao.tagValuesForBatch(new UUID[]{});

    assertThat(result).isEmpty();
  }
}
