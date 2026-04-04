package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OwnerDaoTest extends DatabaseTest {

  private OwnerDao dao;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM tags");
      handle.execute("DELETE FROM notes");
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM owners");
    });
    dao = jdbi.onDemand(OwnerDao.class);
  }

  @Test
  void upsertAndFindByIdentifier() {
    Owner owner = new Owner("TEST-OWNER");
    dao.upsert(owner.identifier().uuid(), owner.value(), false);

    Optional<Owner> result = dao.findByIdentifier(owner.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("TEST-OWNER");
    assertThat(result.get().deleted()).isFalse();
  }

  @Test
  void findByIdentifierReturnsEmptyWhenNotFound() {
    assertThat(dao.findByIdentifier(new Identifier().uuid())).isEmpty();
  }

  @Test
  void upsertUpdatesExistingOwner() {
    Owner original = new Owner("ORIGINAL");
    dao.upsert(original.identifier().uuid(), original.value(), false);

    Owner updated = new Owner("UPDATED", original.identifier(), false);
    dao.upsert(updated.identifier().uuid(), updated.value(), false);

    Optional<Owner> result = dao.findByIdentifier(original.identifier().uuid());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("UPDATED");
  }

  // --- soft delete ---

  @Test
  void softDeleteSetsDeletedFlag() {
    Owner owner = new Owner("TO-DELETE");
    dao.upsert(owner.identifier().uuid(), owner.value(), false);

    assertThat(dao.softDelete(owner.identifier().uuid())).isEqualTo(1);

    assertThat(dao.findByIdentifier(owner.identifier().uuid())).isEmpty();

    Optional<Owner> result = dao.findByIdentifierIncludingDeleted(owner.identifier().uuid());
    assertThat(result).isPresent();
    assertThat(result.get().deleted()).isTrue();
  }

  @Test
  void softDeleteReturnsZeroWhenAlreadyDeleted() {
    Owner owner = new Owner("TO-DELETE");
    dao.upsert(owner.identifier().uuid(), owner.value(), false);

    dao.softDelete(owner.identifier().uuid());
    assertThat(dao.softDelete(owner.identifier().uuid())).isEqualTo(0);
  }

  @Test
  void softDeleteReturnsZeroWhenNotFound() {
    assertThat(dao.softDelete(new Identifier().uuid())).isEqualTo(0);
  }

  // --- hard delete ---

  @Test
  void deleteByIdentifierOnlySoftDeletedOwner() {
    Owner owner = new Owner("TO-DELETE");
    dao.upsert(owner.identifier().uuid(), owner.value(), false);

    assertThat(dao.deleteByIdentifier(owner.identifier().uuid())).isEqualTo(0);

    dao.softDelete(owner.identifier().uuid());
    assertThat(dao.deleteByIdentifier(owner.identifier().uuid())).isEqualTo(1);
    assertThat(dao.findByIdentifierIncludingDeleted(owner.identifier().uuid())).isEmpty();
  }

  @Test
  void deleteReturnsZeroWhenOwnerDoesNotExist() {
    assertThat(dao.deleteByIdentifier(new Identifier().uuid())).isEqualTo(0);
  }

  // --- findByValue ---

  @Test
  void findByValueReturnsMatch() {
    Owner owner = new Owner("FINDABLE");
    dao.upsert(owner.identifier().uuid(), owner.value(), false);

    Optional<Owner> result = dao.findByValue("FINDABLE");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(owner.identifier().uuid());
  }

  @Test
  void findByValueExcludesSoftDeleted() {
    Owner owner = new Owner("FINDABLE");
    dao.upsert(owner.identifier().uuid(), owner.value(), false);
    dao.softDelete(owner.identifier().uuid());

    assertThat(dao.findByValue("FINDABLE")).isEmpty();
    assertThat(dao.findByValueIncludingDeleted("FINDABLE")).isPresent();
  }

  @Test
  void findByValueReturnsEmptyWhenNotFound() {
    assertThat(dao.findByValue("NONEXISTENT")).isEmpty();
  }
}
