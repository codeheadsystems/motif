package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoteTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER, CATEGORY, "test-subject");

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullOwner() {
    assertThatThrownBy(() -> new Note(null, SUBJECT, "value", null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("owner cannot be null");
  }

  @Test
  void constructorRejectsNullSubject() {
    assertThatThrownBy(() -> new Note(OWNER, null, "value", null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Note(OWNER, SUBJECT, null, null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void constructorRejectsEmptyValue() {
    assertThatThrownBy(() -> new Note(OWNER, SUBJECT, "", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsBlankValue() {
    assertThatThrownBy(() -> new Note(OWNER, SUBJECT, "   ", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be empty");
  }

  @Test
  void constructorRejectsValueLongerThan4096Characters() {
    String tooLong = "x".repeat(4097);

    assertThatThrownBy(() -> new Note(OWNER, SUBJECT, tooLong, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 4096 characters");
  }

  @Test
  void constructorAcceptsValueAt4096Characters() {
    String maxLength = "x".repeat(4096);

    Note note = new Note(OWNER, SUBJECT, maxLength, null, null, null, null);

    assertThat(note.value()).hasSize(4096);
  }

  @Test
  void constructorStripsValue() {
    Note note = new Note(OWNER, SUBJECT, "  hello world  ", null, null, null, null);

    assertThat(note.value()).isEqualTo("hello world");
  }

  @Test
  void constructorDefaultsTagsToEmptyListWhenNull() {
    Note note = new Note(OWNER, SUBJECT, "value", null, null, null, null);

    assertThat(note.tags()).isNotNull().isEmpty();
  }

  @Test
  void constructorPreservesProvidedTags() {
    List<Tag> tags = List.of(new Tag("A"), new Tag("B"));

    Note note = new Note(OWNER, SUBJECT, "value", tags, null, null, null);

    assertThat(note.tags()).containsExactly(new Tag("A"), new Tag("B"));
  }

  @Test
  void constructorMakesDefensiveCopyOfTags() {
    ArrayList<Tag> mutableTags = new ArrayList<>(List.of(new Tag("A")));

    Note note = new Note(OWNER, SUBJECT, "value", mutableTags, null, null, null);
    mutableTags.add(new Tag("B"));

    assertThat(note.tags()).containsExactly(new Tag("A"));
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Note note = new Note(OWNER, SUBJECT, "value", null, null, null, null);

    assertThat(note.identifier()).isNotNull();
    assertThat(note.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Note note = new Note(OWNER, SUBJECT, "value", null, id, null, null);

    assertThat(note.identifier()).isSameAs(id);
  }

  @Test
  void constructorDefaultsTimestampWhenNull() {
    Instant before = Instant.now();
    Note note = new Note(OWNER, SUBJECT, "value", null, null, null, null);
    Instant after = Instant.now();

    assertThat(note.timestamp()).isNotNull();
    assertThat(note.timestamp().timestamp()).isBetween(before, after);
  }

  @Test
  void constructorPreservesProvidedTimestamp() {
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));

    Note note = new Note(OWNER, SUBJECT, "value", null, null, null, ts);

    assertThat(note.timestamp()).isSameAs(ts);
  }

  @Test
  void constructorAcceptsNullEvent() {
    Note note = new Note(OWNER, SUBJECT, "value", null, null, null, null);

    assertThat(note.event()).isNull();
  }

  @Test
  void constructorPreservesProvidedEvent() {
    Event event = Event.builder(OWNER, SUBJECT, "event-value").build();

    Note note = new Note(OWNER, SUBJECT, "value", null, null, event, null);

    assertThat(note.event()).isSameAs(event);
  }

  @Test
  void constructorRejectsOwnerMismatchWithSubject() {
    Owner differentOwner = new Owner("DIFFERENT");
    assertThatThrownBy(() -> new Note(differentOwner, SUBJECT, "value", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("owner must match subject's owner");
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
    Event event = Event.builder(OWNER, SUBJECT, "event-value").build();
    Note original = new Note(OWNER, SUBJECT, "original", tags, id, event, ts);

    Note copy = Note.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingFields() {
    Note original = Note.builder(OWNER, SUBJECT, "original").build();

    Event event = Event.builder(OWNER, SUBJECT, "event-value").build();
    Note modified = Note.from(original)
        .event(event)
        .build();

    assertThat(modified.owner()).isEqualTo(original.owner());
    assertThat(modified.subject()).isEqualTo(original.subject());
    assertThat(modified.identifier()).isEqualTo(original.identifier());
    assertThat(modified.timestamp()).isEqualTo(original.timestamp());
    assertThat(modified.value()).isEqualTo("original");
    assertThat(modified.event()).isSameAs(event);
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullOwner() {
    assertThatThrownBy(() -> Note.builder(null, SUBJECT, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("owner cannot be null");
  }

  @Test
  void builderRejectsNullSubject() {
    assertThatThrownBy(() -> Note.builder(OWNER, null, "value"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void builderRejectsNullValue() {
    assertThatThrownBy(() -> Note.builder(OWNER, SUBJECT, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void builderWithDefaults() {
    Note note = Note.builder(OWNER, SUBJECT, "note-value").build();

    assertThat(note.owner()).isEqualTo(OWNER);
    assertThat(note.subject()).isEqualTo(SUBJECT);
    assertThat(note.value()).isEqualTo("note-value");
    assertThat(note.identifier()).isNotNull();
    assertThat(note.timestamp()).isNotNull();
    assertThat(note.tags()).isEmpty();
    assertThat(note.event()).isNull();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("X"));
    Event event = Event.builder(OWNER, SUBJECT, "event-value").build();

    Note note = Note.builder(OWNER, SUBJECT, "note-value")
        .tags(tags)
        .identifier(id)
        .event(event)
        .timestamp(ts)
        .build();

    assertThat(note.owner()).isEqualTo(OWNER);
    assertThat(note.subject()).isEqualTo(SUBJECT);
    assertThat(note.value()).isEqualTo("note-value");
    assertThat(note.tags()).containsExactly(new Tag("X"));
    assertThat(note.identifier()).isSameAs(id);
    assertThat(note.event()).isSameAs(event);
    assertThat(note.timestamp()).isSameAs(ts);
  }
}
