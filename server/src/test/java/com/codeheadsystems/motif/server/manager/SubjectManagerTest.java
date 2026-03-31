package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.dao.SubjectDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

  @Mock
  private SubjectDao subjectDao;

  private SubjectManager subjectManager;

  @BeforeEach
  void setUp() {
    subjectManager = new SubjectManager(subjectDao);
  }

  // --- store ---

  @Test
  void storeCallsUpsertWithCorrectArgs() {
    subjectManager.store(SUBJECT);

    verify(subjectDao).upsert(
        SUBJECT.identifier().uuid(),
        SUBJECT.ownerIdentifier().uuid(),
        SUBJECT.category().value(),
        SUBJECT.value());
  }

  // --- getSubject by identifier ---

  @Test
  void getSubjectByIdentifierDelegatesToDao() {
    when(subjectDao.findByIdentifier(SUBJECT.identifier().uuid()))
        .thenReturn(Optional.of(SUBJECT));

    Optional<Subject> result = subjectManager.getSubject(SUBJECT.identifier());

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void getSubjectByIdentifierReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(subjectDao.findByIdentifier(unknown.uuid())).thenReturn(Optional.empty());

    assertThat(subjectManager.getSubject(unknown)).isEmpty();
  }

  // --- getSubject by owner and identifier ---

  @Test
  void getSubjectByOwnerAndIdentifierDelegatesToDao() {
    when(subjectDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(Optional.of(SUBJECT));

    Optional<Subject> result = subjectManager.getSubject(OWNER, SUBJECT.identifier());

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void getSubjectByOwnerAndIdentifierReturnsEmptyForWrongOwner() {
    Owner other = new Owner("OTHER-OWNER");
    when(subjectDao.findByOwnerAndIdentifier(other.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThat(subjectManager.getSubject(other, SUBJECT.identifier())).isEmpty();
  }

  // --- getSubject by value ---

  @Test
  void getSubjectByValueReturnsFirstMatch() {
    when(subjectDao.findByValue("test-subject")).thenReturn(List.of(SUBJECT));

    Optional<Subject> result = subjectManager.getSubject("test-subject");

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void getSubjectByValueReturnsEmptyWhenNotFound() {
    when(subjectDao.findByValue("nonexistent")).thenReturn(List.of());

    assertThat(subjectManager.getSubject("nonexistent")).isEmpty();
  }

  // --- findByCategory ---

  @Test
  void findByCategoryDelegatesToDao() {
    List<Subject> expected = List.of(SUBJECT);
    when(subjectDao.findByOwnerAndCategory(OWNER.identifier().uuid(), CATEGORY.value()))
        .thenReturn(expected);

    List<Subject> results = subjectManager.findByCategory(OWNER, CATEGORY);

    assertThat(results).isEqualTo(expected);
  }

  // --- find by owner, category, and value ---

  @Test
  void findDelegatesToDao() {
    when(subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), CATEGORY.value(), "test-subject"))
        .thenReturn(Optional.of(SUBJECT));

    Optional<Subject> result = subjectManager.find(OWNER, CATEGORY, "test-subject");

    assertThat(result).contains(SUBJECT);
  }

  @Test
  void findReturnsEmptyWhenNotFound() {
    when(subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), CATEGORY.value(), "nonexistent"))
        .thenReturn(Optional.empty());

    assertThat(subjectManager.find(OWNER, CATEGORY, "nonexistent")).isEmpty();
  }

  // --- update ---

  @Test
  void updateStoresWhenSubjectExists() {
    when(subjectDao.findByIdentifier(SUBJECT.identifier().uuid()))
        .thenReturn(Optional.of(SUBJECT));

    Subject updated = Subject.from(SUBJECT).value("updated").build();
    assertThat(subjectManager.update(updated)).isTrue();

    verify(subjectDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.category().value(),
        "updated");
  }

  @Test
  void updateReturnsFalseWhenSubjectDoesNotExist() {
    when(subjectDao.findByIdentifier(SUBJECT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThat(subjectManager.update(SUBJECT)).isFalse();
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
