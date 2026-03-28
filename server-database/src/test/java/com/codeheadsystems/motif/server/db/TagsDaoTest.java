package com.codeheadsystems.motif.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Tag;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TagsDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;

  private TagsDao dao;
  private Identifier identifier;

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
    identifier = new Identifier();
    jdbi.useHandle(handle -> handle.execute("DELETE FROM tags"));
    dao = jdbi.onDemand(TagsDao.class);
  }

  @Test
  void tagsForReturnsEmptyListWhenNoTags() {
    List<Tag> result = dao.tagsFor(identifier);

    assertThat(result).isEmpty();
  }

  @Test
  void addTagsThenRetrieve() {
    List<Tag> tags = List.of(new Tag("ALPHA"), new Tag("BETA"));

    boolean added = dao.addTags(identifier, tags);

    assertThat(added).isTrue();
    assertThat(dao.tagsFor(identifier))
        .containsExactlyInAnyOrder(new Tag("ALPHA"), new Tag("BETA"));
  }

  @Test
  void addTagsIsIdempotent() {
    Tag tag = new Tag("DUPLICATE");
    dao.addTags(identifier, List.of(tag));
    dao.addTags(identifier, List.of(tag));

    assertThat(dao.tagsFor(identifier)).containsExactly(new Tag("DUPLICATE"));
  }

  @Test
  void removeTagsReturnsTrueWhenTagsRemoved() {
    dao.addTags(identifier, List.of(new Tag("A"), new Tag("B"), new Tag("C")));

    boolean removed = dao.removeTags(identifier, List.of(new Tag("A"), new Tag("C")));

    assertThat(removed).isTrue();
    assertThat(dao.tagsFor(identifier)).containsExactly(new Tag("B"));
  }

  @Test
  void removeTagsReturnsFalseWhenNoTagsRemoved() {
    boolean removed = dao.removeTags(identifier, List.of(new Tag("NONEXISTENT")));

    assertThat(removed).isFalse();
  }

  @Test
  void tagsAreIsolatedByIdentifier() {
    Identifier id1 = new Identifier();
    Identifier id2 = new Identifier();

    dao.addTags(id1, List.of(new Tag("SHARED"), new Tag("ONLY1")));
    dao.addTags(id2, List.of(new Tag("SHARED"), new Tag("ONLY2")));

    assertThat(dao.tagsFor(id1))
        .containsExactlyInAnyOrder(new Tag("SHARED"), new Tag("ONLY1"));
    assertThat(dao.tagsFor(id2))
        .containsExactlyInAnyOrder(new Tag("SHARED"), new Tag("ONLY2"));
  }

  @Test
  void removeTagsDoesNotAffectOtherIdentifiers() {
    Identifier id1 = new Identifier();
    Identifier id2 = new Identifier();

    dao.addTags(id1, List.of(new Tag("TAG")));
    dao.addTags(id2, List.of(new Tag("TAG")));

    dao.removeTags(id1, List.of(new Tag("TAG")));

    assertThat(dao.tagsFor(id1)).isEmpty();
    assertThat(dao.tagsFor(id2)).containsExactly(new Tag("TAG"));
  }
}
