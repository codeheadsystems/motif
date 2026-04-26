package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.TestCategories;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = TestCategories.of(OWNER.identifier(), "test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY.identifier(), "test-subject");

  @Mock
  private Jdbi jdbi;
  @Mock
  private Handle handle;
  @Mock
  private SubjectDao subjectDao;

  private SubjectManager subjectManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    lenient().doAnswer(invocation -> invocation.getArgument(0, HandleCallback.class).withHandle(handle))
        .when(jdbi).inTransaction(any(HandleCallback.class));
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, HandleConsumer.class).useHandle(handle);
      return null;
    }).when(jdbi).useTransaction(any(HandleConsumer.class));
    lenient().when(handle.attach(SubjectDao.class)).thenReturn(subjectDao);
    subjectManager = new SubjectManager(jdbi, subjectDao);
  }

  // --- store ---

  @Test
  void storeCallsUpsertWithCorrectArgs() {
    subjectManager.store(SUBJECT);

    verify(subjectDao).upsert(
        SUBJECT.identifier().uuid(),
        SUBJECT.ownerIdentifier().uuid(),
        SUBJECT.categoryIdentifier().uuid(),
        SUBJECT.value());
  }

  // --- get by identifier ---

  @Test
  void getByIdentifierDelegatesToDao() {
    when(subjectDao.findByIdentifier(SUBJECT.identifier().uuid()))
        .thenReturn(Optional.of(SUBJECT));

    Optional<Subject> result = subjectManager.get(SUBJECT.identifier());

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void getByIdentifierReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(subjectDao.findByIdentifier(unknown.uuid())).thenReturn(Optional.empty());

    assertThat(subjectManager.get(unknown)).isEmpty();
  }

  // --- get by owner and identifier ---

  @Test
  void getByOwnerAndIdentifierDelegatesToDao() {
    when(subjectDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(Optional.of(SUBJECT));

    Optional<Subject> result = subjectManager.get(OWNER, SUBJECT.identifier());

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void getByOwnerAndIdentifierReturnsEmptyForWrongOwner() {
    Owner other = new Owner("OTHER-OWNER");
    when(subjectDao.findByOwnerAndIdentifier(other.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThat(subjectManager.get(other, SUBJECT.identifier())).isEmpty();
  }

  // --- findByCategory ---

  @Test
  void findByCategoryDelegatesToDao() {
    PageRequest pr = PageRequest.first(10);
    when(subjectDao.findByOwnerAndCategory(OWNER.identifier().uuid(), CATEGORY.identifier().uuid(), 11, 0))
        .thenReturn(List.of(SUBJECT));

    Page<Subject> results = subjectManager.findByCategory(OWNER, CATEGORY, pr);

    assertThat(results.items()).containsExactly(SUBJECT);
    assertThat(results.hasMore()).isFalse();
  }

  // --- find by owner, category, and value ---

  @Test
  void findDelegatesToDao() {
    when(subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), CATEGORY.identifier().uuid(), "test-subject"))
        .thenReturn(Optional.of(SUBJECT));

    Optional<Subject> result = subjectManager.find(OWNER, CATEGORY, "test-subject");

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void findReturnsEmptyWhenNotFound() {
    when(subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), CATEGORY.identifier().uuid(), "nonexistent"))
        .thenReturn(Optional.empty());

    assertThat(subjectManager.find(OWNER, CATEGORY, "nonexistent")).isEmpty();
  }

  // --- update ---

  @Test
  void updateStoresWhenSubjectExists() {
    when(subjectDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(Optional.of(SUBJECT));

    Subject updated = Subject.from(SUBJECT).value("updated").build();
    subjectManager.update(updated);

    verify(subjectDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.categoryIdentifier().uuid(),
        "updated");
  }

  @Test
  void updateThrowsWhenSubjectDoesNotExist() {
    when(subjectDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> subjectManager.update(SUBJECT))
        .isInstanceOf(NotFoundException.class);
    verifyNoMoreInteractions(subjectDao);
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenRowDeleted() {
    when(subjectDao.deleteByOwnerAndIdentifier(
        SUBJECT.ownerIdentifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(1);

    assertThat(subjectManager.delete(SUBJECT)).isTrue();
  }

  @Test
  void deleteReturnsFalseWhenNoRowDeleted() {
    when(subjectDao.deleteByOwnerAndIdentifier(
        SUBJECT.ownerIdentifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(0);

    assertThat(subjectManager.delete(SUBJECT)).isFalse();
  }
}
