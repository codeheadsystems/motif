package com.codeheadsystems.motif.server.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoteTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Identifier OWNER_ID = OWNER.identifier();
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER_ID, CATEGORY, "test-subject");
  private static final Identifier SUBJECT_ID = SUBJECT.identifier();

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> new Note(null, SUBJECT_ID, "value", null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void constructorRejectsNullSubjectIdentifier() {
    assertThatThrownBy(() -> new Note(OWNER_ID, null, "value", null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subjectIdentifier cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Note(OWNER_ID, SUBJECT_ID, null, null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsEmptyValue() {
    assertThatThrownBy(() -> new Note(OWNER_ID, SUBJECT_ID, "", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsBlankValue() {
    assertThatThrownBy(() -> new Note(OWNER_ID, SUBJECT_ID, "   ", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan4096Characters() {
    String tooLong = "x".repeat(4097);

    assertThatThrownBy(() -> new Note(OWNER_ID, SUBJECT_ID, tooLong, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 4096 characters");
  }

  @Test
  void constructorAcceptsValueAt4096Characters() {
    String maxLength = "x".repeat(4096);

    Note note = new Note(OWNER_ID, SUBJECT_ID, maxLength, null, null, null, null);

    assertThat(note.value()).hasSize(4096);
  }

  @Test
  void constructorStripsValue() {
    Note note = new Note(OWNER_ID, SUBJECT_ID, "  hello world  ", null, null, null, null);

    assertThat(note.value()).isEqualTo("hello world");
  }

  @Test
  void constructorDefaultsTagsToEmptyListWhenNull() {
    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, null, null, null);

    assertThat(note.tags()).isNotNull().isEmpty();
  }

  @Test
  void constructorPreservesProvidedTags() {
    List<Tag> tags = List.of(new Tag("A"), new Tag("B"));

    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", tags, null, null, null);

    assertThat(note.tags()).containsExactly(new Tag("A"), new Tag("B"));
  }

  @Test
  void constructorMakesDefensiveCopyOfTags() {
    ArrayList<Tag> mutableTags = new ArrayList<>(List.of(new Tag("A")));

    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", mutableTags, null, null, null);
    mutableTags.add(new Tag("B"));

    assertThat(note.tags()).containsExactly(new Tag("A"));
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, null, null, null);

    assertThat(note.identifier()).isNotNull();
    assertThat(note.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, id, null, null);

    assertThat(note.identifier()).isSameAs(id);
  }

  @Test
  void constructorDefaultsTimestampWhenNull() {
    Instant before = Instant.now();
    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, null, null, null);
    Instant after = Instant.now();

    assertThat(note.timestamp()).isNotNull();
    assertThat(note.timestamp().timestamp()).isBetween(before, after);
  }

  @Test
  void constructorPreservesProvidedTimestamp() {
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));

    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, null, null, ts);

    assertThat(note.timestamp()).isSameAs(ts);
  }

  @Test
  void constructorAcceptsNullEventIdentifier() {
    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, null, null, null);

    assertThat(note.eventIdentifier()).isNull();
  }

  @Test
  void constructorPreservesProvidedEventIdentifier() {
    Identifier eventId = new Identifier();

    Note note = new Note(OWNER_ID, SUBJECT_ID, "value", null, null, eventId, null);

    assertThat(note.eventIdentifier()).isSameAs(eventId);
  }

  // --- Builder.from tests ---

  @Test
  void fromRejectsNull() {
    assertThatThrownBy(() -> Note.from(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("note cannot be null");
  }

  @Test
  void fromCopiesAllFields() {
    Identifier id = new Identifier();
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("A"));
    Identifier eventId = new Identifier();
    Note original = new Note(OWNER_ID, SUBJECT_ID, "original", tags, id, eventId, ts);

    Note copy = Note.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingFields() {
    Note original = Note.builder().owner(OWNER).subject(SUBJECT).value("original").build();

    Identifier eventId = new Identifier();
    Note modified = Note.from(original)
        .eventIdentifier(eventId)
        .build();

    assertThat(modified.ownerIdentifier()).isEqualTo(original.ownerIdentifier());
    assertThat(modified.subjectIdentifier()).isEqualTo(original.subjectIdentifier());
    assertThat(modified.identifier()).isEqualTo(original.identifier());
    assertThat(modified.timestamp()).isEqualTo(original.timestamp());
    assertThat(modified.value()).isEqualTo("original");
    assertThat(modified.eventIdentifier()).isSameAs(eventId);
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> Note.builder().subject(SUBJECT).value("value").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void builderRejectsNullSubjectIdentifier() {
    assertThatThrownBy(() -> Note.builder().owner(OWNER).value("value").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subjectIdentifier cannot be null");
  }

  @Test
  void builderRejectsNullValue() {
    assertThatThrownBy(() -> Note.builder().owner(OWNER).subject(SUBJECT).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void builderWithDefaults() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("note-value").build();

    assertThat(note.ownerIdentifier()).isEqualTo(OWNER_ID);
    assertThat(note.subjectIdentifier()).isEqualTo(SUBJECT_ID);
    assertThat(note.value()).isEqualTo("note-value");
    assertThat(note.identifier()).isNotNull();
    assertThat(note.timestamp()).isNotNull();
    assertThat(note.tags()).isEmpty();
    assertThat(note.eventIdentifier()).isNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("X"));
    Identifier eventId = new Identifier();

    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("note-value")
        .tags(tags)
        .identifier(id)
        .eventIdentifier(eventId)
        .timestamp(ts)
        .build();

    assertThat(note.ownerIdentifier()).isEqualTo(OWNER_ID);
    assertThat(note.subjectIdentifier()).isEqualTo(SUBJECT_ID);
    assertThat(note.value()).isEqualTo("note-value");
    assertThat(note.tags()).containsExactly(new Tag("X"));
    assertThat(note.identifier()).isSameAs(id);
    assertThat(note.eventIdentifier()).isSameAs(eventId);
    assertThat(note.timestamp()).isSameAs(ts);
  }
}
