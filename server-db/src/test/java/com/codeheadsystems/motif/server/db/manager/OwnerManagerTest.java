package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");

  @Mock
  private Jdbi jdbi;
  @Mock
  private Handle handle;
  @Mock
  private OwnerDao ownerDao;
  @Mock
  private TagsDao tagsDao;

  private OwnerManager ownerManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    lenient().doAnswer(invocation -> invocation.getArgument(0, HandleCallback.class).withHandle(handle))
        .when(jdbi).inTransaction(any(HandleCallback.class));
    lenient().when(handle.attach(OwnerDao.class)).thenReturn(ownerDao);
    lenient().when(handle.attach(TagsDao.class)).thenReturn(tagsDao);
    ownerManager = new OwnerManager(jdbi, ownerDao);
  }

  // --- store ---

  @Test
  void storeCallsUpsertWithCorrectArgs() {
    ownerManager.store(OWNER);

    verify(ownerDao).upsert(OWNER.identifier().uuid(), OWNER.value(), false, "FREE_SYNCED");
  }

  // --- get ---

  @Test
  void getDelegatesToDao() {
    when(ownerDao.findByIdentifier(OWNER.identifier().uuid()))
        .thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.get(OWNER.identifier());

    assertThat(result).contains(OWNER);
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(ownerDao.findByIdentifier(unknown.uuid())).thenReturn(Optional.empty());

    assertThat(ownerManager.get(unknown)).isEmpty();
  }

  @Test
  void getWithIncludeSoftDeletedTrue() {
    when(ownerDao.findByIdentifierIncludingDeleted(OWNER.identifier().uuid()))
        .thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.get(OWNER.identifier(), true);

    assertThat(result).contains(OWNER);
  }

  @Test
  void getWithIncludeSoftDeletedFalseDelegatesToDefault() {
    when(ownerDao.findByIdentifier(OWNER.identifier().uuid()))
        .thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.get(OWNER.identifier(), false);

    assertThat(result).contains(OWNER);
  }

  // --- find ---

  @Test
  void findDelegatesToDaoWithUpperCasedValue() {
    when(ownerDao.findByValue("FINDABLE")).thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.find("findable");

    assertThat(result).contains(OWNER);
  }

  @Test
  void findReturnsEmptyWhenNotFound() {
    when(ownerDao.findByValue("NONEXISTENT")).thenReturn(Optional.empty());

    assertThat(ownerManager.find("nonexistent")).isEmpty();
  }

  @Test
  void findWithIncludeSoftDeletedTrue() {
    when(ownerDao.findByValueIncludingDeleted("FINDABLE")).thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.find("findable", true);

    assertThat(result).contains(OWNER);
  }

  @Test
  void findWithIncludeSoftDeletedFalseDelegatesToDefault() {
    when(ownerDao.findByValue("FINDABLE")).thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.find("findable", false);

    assertThat(result).contains(OWNER);
  }

  // --- softDelete ---

  @Test
  void softDeleteReturnsTrueWhenRowUpdated() {
    when(ownerDao.softDelete(OWNER.identifier().uuid())).thenReturn(1);

    assertThat(ownerManager.softDelete(OWNER.identifier())).isTrue();
  }

  @Test
  void softDeleteReturnsFalseWhenNoRowUpdated() {
    when(ownerDao.softDelete(OWNER.identifier().uuid())).thenReturn(0);

    assertThat(ownerManager.softDelete(OWNER.identifier())).isFalse();
  }

  // --- hardDelete ---

  @Test
  void hardDeleteCleansTagsAndDeletesOwner() {
    when(ownerDao.deleteByIdentifier(OWNER.identifier().uuid())).thenReturn(1);

    assertThat(ownerManager.hardDelete(OWNER.identifier())).isTrue();

    verify(tagsDao).deleteTagsForOwnerNotes(OWNER.identifier().uuid());
    verify(tagsDao).deleteTagsForOwnerEvents(OWNER.identifier().uuid());
    verify(ownerDao).deleteByIdentifier(OWNER.identifier().uuid());
  }

  @Test
  void hardDeleteReturnsFalseWhenOwnerNotSoftDeleted() {
    when(ownerDao.deleteByIdentifier(OWNER.identifier().uuid())).thenReturn(0);

    assertThat(ownerManager.hardDelete(OWNER.identifier())).isFalse();
  }
}
