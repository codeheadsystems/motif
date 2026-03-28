package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class NoteTest {

  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(CATEGORY, "test-subject");

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullSubject() {
    assertThatThrownBy(() -> new Note(null, "value", null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void constructorDefaultsNullValueToEmpty() {
    Note note = new Note(SUBJECT, null, null, null, null, null);

    assertThat(note.value()).isEmpty();
  }

  @Test
  void constructorRejectsValueLongerThan4096Characters() {
    String tooLong = "x".repeat(4097);

    assertThatThrownBy(() -> new Note(SUBJECT, tooLong, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 4096 characters");
  }

  @Test
  void constructorAcceptsValueAt4096Characters() {
    String maxLength = "x".repeat(4096);

    Note note = new Note(SUBJECT, maxLength, null, null, null, null);

    assertThat(note.value()).hasSize(4096);
  }

  @Test
  void constructorStripsValue() {
    Note note = new Note(SUBJECT, "  hello world  ", null, null, null, null);

    assertThat(note.value()).isEqualTo("hello world");
  }

  @Test
  void constructorDefaultsTagsToEmptyListWhenNull() {
    Note note = new Note(SUBJECT, "value", null, null, null, null);

    assertThat(note.tags()).isNotNull().isEmpty();
  }

  @Test
  void constructorPreservesProvidedTags() {
    List<Tag> tags = List.of(new Tag("A"), new Tag("B"));

    Note note = new Note(SUBJECT, "value", tags, null, null, null);

    assertThat(note.tags()).containsExactly(new Tag("A"), new Tag("B"));
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Note note = new Note(SUBJECT, "value", null, null, null, null);

    assertThat(note.identifier()).isNotNull();
    assertThat(note.identifier().clazz()).isEqualTo(Note.class);
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier(Note.class);

    Note note = new Note(SUBJECT, "value", null, id, null, null);

    assertThat(note.identifier()).isSameAs(id);
  }

  @Test
  void constructorDefaultsTimestampWhenNull() {
    Instant before = Instant.now();
    Note note = new Note(SUBJECT, "value", null, null, null, null);
    Instant after = Instant.now();

    assertThat(note.timestamp()).isNotNull();
    assertThat(note.timestamp().timestamp()).isBetween(before, after);
  }

  @Test
  void constructorPreservesProvidedTimestamp() {
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));

    Note note = new Note(SUBJECT, "value", null, null, null, ts);

    assertThat(note.timestamp()).isSameAs(ts);
  }

  @Test
  void constructorAcceptsNullEvent() {
    Note note = new Note(SUBJECT, "value", null, null, null, null);

    assertThat(note.event()).isNull();
  }

  @Test
  void constructorPreservesProvidedEvent() {
    Event event = Event.builder(SUBJECT, "event-value").build();

    Note note = new Note(SUBJECT, "value", null, null, event, null);

    assertThat(note.event()).isSameAs(event);
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
    Identifier id = new Identifier(Note.class);
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("A"));
    Event event = Event.builder(SUBJECT, "event-value").build();
    Note original = new Note(SUBJECT, "original", tags, id, event, ts);

    Note copy = Note.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingFields() {
    Note original = Note.builder(SUBJECT).value("original").build();

    Event event = Event.builder(SUBJECT, "event-value").build();
    Note modified = Note.from(original)
        .event(event)
        .value("modified")
        .build();

    assertThat(modified.subject()).isEqualTo(original.subject());
    assertThat(modified.identifier()).isEqualTo(original.identifier());
    assertThat(modified.timestamp()).isEqualTo(original.timestamp());
    assertThat(modified.value()).isEqualTo("modified");
    assertThat(modified.event()).isSameAs(event);
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullSubject() {
    assertThatThrownBy(() -> Note.builder(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void builderDefaultsNullValueToEmpty() {
    Note note = Note.builder(SUBJECT).build();

    assertThat(note.value()).isEmpty();
  }

  @Test
  void builderWithDefaults() {
    Note note = Note.builder(SUBJECT).value("note-value").build();

    assertThat(note.subject()).isEqualTo(SUBJECT);
    assertThat(note.value()).isEqualTo("note-value");
    assertThat(note.identifier()).isNotNull();
    assertThat(note.timestamp()).isNotNull();
    assertThat(note.tags()).isEmpty();
    assertThat(note.event()).isNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier(Note.class);
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("X"));
    Event event = Event.builder(SUBJECT, "event-value").build();

    Note note = Note.builder(SUBJECT)
        .value("note-value")
        .tags(tags)
        .identifier(id)
        .event(event)
        .timestamp(ts)
        .build();

    assertThat(note.subject()).isEqualTo(SUBJECT);
    assertThat(note.value()).isEqualTo("note-value");
    assertThat(note.tags()).containsExactly(new Tag("X"));
    assertThat(note.identifier()).isSameAs(id);
    assertThat(note.event()).isSameAs(event);
    assertThat(note.timestamp()).isSameAs(ts);
  }
}
