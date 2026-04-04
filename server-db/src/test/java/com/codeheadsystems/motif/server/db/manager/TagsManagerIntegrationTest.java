package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Tag;
import java.util.List;
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
class TagsManagerIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;
  private TagsManager tagsManager;
  private Identifier identifier;

  @BeforeAll
  static void setupJdbi() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());

    Flyway.configure().dataSource(ds).load().migrate();

    jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());
  }

  @BeforeEach
  void setUp() {
    identifier = new Identifier();
    jdbi.useHandle(handle -> handle.execute("DELETE FROM tags"));
    TagsDao tagsDao = jdbi.onDemand(TagsDao.class);
    tagsManager = new TagsManager(tagsDao);
  }

  @Test
  void tagsForReturnsEmptyWhenNoTags() {
    assertThat(tagsManager.tagsFor(identifier)).isEmpty();
  }

  @Test
  void addTagsAndRetrieve() {
    tagsManager.addTags(identifier, List.of(new Tag("ALPHA"), new Tag("BETA")));

    assertThat(tagsManager.tagsFor(identifier))
        .containsExactlyInAnyOrder(new Tag("ALPHA"), new Tag("BETA"));
  }

  @Test
  void addTagsIsIdempotent() {
    tagsManager.addTags(identifier, List.of(new Tag("DUPLICATE")));
    tagsManager.addTags(identifier, List.of(new Tag("DUPLICATE")));

    assertThat(tagsManager.tagsFor(identifier)).containsExactly(new Tag("DUPLICATE"));
  }

  @Test
  void removeTagsReturnsTrueWhenTagsRemoved() {
    tagsManager.addTags(identifier, List.of(new Tag("A"), new Tag("B"), new Tag("C")));

    assertThat(tagsManager.removeTags(identifier, List.of(new Tag("A"), new Tag("C")))).isTrue();
    assertThat(tagsManager.tagsFor(identifier)).containsExactly(new Tag("B"));
  }

  @Test
  void removeTagsReturnsFalseWhenNoTagsRemoved() {
    assertThat(tagsManager.removeTags(identifier, List.of(new Tag("NONEXISTENT")))).isFalse();
  }

  @Test
  void tagsAreIsolatedByIdentifier() {
    Identifier id1 = new Identifier();
    Identifier id2 = new Identifier();

    tagsManager.addTags(id1, List.of(new Tag("SHARED"), new Tag("ONLY1")));
    tagsManager.addTags(id2, List.of(new Tag("SHARED"), new Tag("ONLY2")));

    assertThat(tagsManager.tagsFor(id1))
        .containsExactlyInAnyOrder(new Tag("SHARED"), new Tag("ONLY1"));
    assertThat(tagsManager.tagsFor(id2))
        .containsExactlyInAnyOrder(new Tag("SHARED"), new Tag("ONLY2"));
  }
}
