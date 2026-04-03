package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Tag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagsManagerTest {

  private static final Identifier ID = new Identifier();

  @Mock
  private TagsDao tagsDao;

  private TagsManager tagsManager;

  @BeforeEach
  void setUp() {
    tagsManager = new TagsManager(tagsDao);
  }

  // --- tagsFor ---

  @Test
  void tagsForConvertsStringsToTags() {
    when(tagsDao.tagValuesFor(ID.uuid())).thenReturn(List.of("ALPHA", "BETA"));

    List<Tag> result = tagsManager.tagsFor(ID);

    assertThat(result).containsExactly(new Tag("ALPHA"), new Tag("BETA"));
  }

  @Test
  void tagsForReturnsEmptyWhenNoTags() {
    when(tagsDao.tagValuesFor(ID.uuid())).thenReturn(List.of());

    assertThat(tagsManager.tagsFor(ID)).isEmpty();
  }

  // --- addTags ---

  @Test
  void addTagsCallsInsertTagForEach() {
    tagsManager.addTags(ID, List.of(new Tag("A"), new Tag("B")));

    verify(tagsDao).insertTag(ID.uuid(), "A");
    verify(tagsDao).insertTag(ID.uuid(), "B");
    verifyNoMoreInteractions(tagsDao);
  }

  @Test
  void addTagsWithEmptyListDoesNothing() {
    tagsManager.addTags(ID, List.of());

    verifyNoMoreInteractions(tagsDao);
  }

  // --- removeTags ---

  @Test
  void removeTagsReturnsTrueWhenTagsRemoved() {
    when(tagsDao.deleteTag(ID.uuid(), "A")).thenReturn(1);
    when(tagsDao.deleteTag(ID.uuid(), "C")).thenReturn(1);

    assertThat(tagsManager.removeTags(ID, List.of(new Tag("A"), new Tag("C")))).isTrue();
  }

  @Test
  void removeTagsReturnsFalseWhenNoTagsRemoved() {
    when(tagsDao.deleteTag(ID.uuid(), "NONEXISTENT")).thenReturn(0);

    assertThat(tagsManager.removeTags(ID, List.of(new Tag("NONEXISTENT")))).isFalse();
  }

  // --- deleteAllTags ---

  @Test
  void deleteAllTagsReturnsTrueWhenTagsExisted() {
    when(tagsDao.deleteAllTags(ID.uuid())).thenReturn(2);

    assertThat(tagsManager.deleteAllTags(ID)).isTrue();
  }

  @Test
  void deleteAllTagsReturnsFalseWhenNoTags() {
    when(tagsDao.deleteAllTags(ID.uuid())).thenReturn(0);

    assertThat(tagsManager.deleteAllTags(ID)).isFalse();
  }

  // --- syncTags ---

  @Test
  void syncTagsAddsNewAndRemovesOld() {
    when(tagsDao.tagValuesFor(ID.uuid())).thenReturn(List.of("A", "B"));

    tagsManager.syncTags(ID, List.of(new Tag("B"), new Tag("C")));

    verify(tagsDao).deleteTag(ID.uuid(), "A");
    verify(tagsDao).insertTag(ID.uuid(), "C");
    verifyNoMoreInteractions(tagsDao);
  }

  @Test
  void syncTagsWithEmptyDesiredRemovesAll() {
    when(tagsDao.tagValuesFor(ID.uuid())).thenReturn(List.of("A", "B"));

    tagsManager.syncTags(ID, List.of());

    verify(tagsDao).deleteTag(ID.uuid(), "A");
    verify(tagsDao).deleteTag(ID.uuid(), "B");
    verifyNoMoreInteractions(tagsDao);
  }

  @Test
  void syncTagsWithEmptyExistingAddsAll() {
    when(tagsDao.tagValuesFor(ID.uuid())).thenReturn(List.of());

    tagsManager.syncTags(ID, List.of(new Tag("A"), new Tag("B")));

    verify(tagsDao).insertTag(ID.uuid(), "A");
    verify(tagsDao).insertTag(ID.uuid(), "B");
    verifyNoMoreInteractions(tagsDao);
  }

  @Test
  void syncTagsNoChangesWhenAlreadyInSync() {
    when(tagsDao.tagValuesFor(ID.uuid())).thenReturn(List.of("A", "B"));

    tagsManager.syncTags(ID, List.of(new Tag("A"), new Tag("B")));

    verifyNoMoreInteractions(tagsDao);
  }
}
