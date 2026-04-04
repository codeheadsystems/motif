package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

  // --- Constructor tests ---

  @Test
  void constructorRejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> new Event(null, SUBJECT, "value", null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void constructorRejectsNullSubject() {
    assertThatThrownBy(() -> new Event(OWNER.identifier(), null, "value", null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void constructorRejectsNullValue() {
    assertThatThrownBy(() -> new Event(OWNER.identifier(), SUBJECT, null, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorStripsValue() {
    Event event = new Event(OWNER.identifier(), SUBJECT, "  hello world  ", null, null, null);

    assertThat(event.value()).isEqualTo("hello world");
  }

  @Test
  void constructorRejectsValueLongerThan256Characters() {
    String tooLong = "x".repeat(257);

    assertThatThrownBy(() -> new Event(OWNER.identifier(), SUBJECT, tooLong, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value cannot be longer than 256 characters");
  }

  @Test
  void constructorAcceptsValueAt256Characters() {
    String maxLength = "x".repeat(256);

    Event event = new Event(OWNER.identifier(), SUBJECT, maxLength, null, null, null);

    assertThat(event.value()).hasSize(256);
  }

  @Test
  void constructorDefaultsIdentifierWhenNull() {
    Event event = new Event(OWNER.identifier(), SUBJECT, "value", null, null, null);

    assertThat(event.identifier()).isNotNull();
    assertThat(event.identifier().uuid()).isNotNull();
  }

  @Test
  void constructorPreservesProvidedIdentifier() {
    Identifier id = new Identifier();

    Event event = new Event(OWNER.identifier(), SUBJECT, "value", id, null, null);

    assertThat(event.identifier()).isSameAs(id);
  }

  @Test
  void constructorDefaultsTimestampWhenNull() {
    Instant before = Instant.now();
    Event event = new Event(OWNER.identifier(), SUBJECT, "value", null, null, null);
    Instant after = Instant.now();

    assertThat(event.timestamp()).isNotNull();
    assertThat(event.timestamp().timestamp()).isBetween(before, after);
  }

  @Test
  void constructorPreservesProvidedTimestamp() {
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));

    Event event = new Event(OWNER.identifier(), SUBJECT, "value", null, ts, null);

    assertThat(event.timestamp()).isSameAs(ts);
  }

  @Test
  void constructorDefaultsTagsToEmptyListWhenNull() {
    Event event = new Event(OWNER.identifier(), SUBJECT, "value", null, null, null);

    assertThat(event.tags()).isNotNull().isEmpty();
  }

  @Test
  void constructorPreservesProvidedTags() {
    List<Tag> tags = List.of(new Tag("A"), new Tag("B"));

    Event event = new Event(OWNER.identifier(), SUBJECT, "value", null, null, tags);

    assertThat(event.tags()).containsExactly(new Tag("A"), new Tag("B"));
  }

  @Test
  void constructorMakesDefensiveCopyOfTags() {
    ArrayList<Tag> mutableTags = new ArrayList<>(List.of(new Tag("A")));

    Event event = new Event(OWNER.identifier(), SUBJECT, "value", null, null, mutableTags);
    mutableTags.add(new Tag("B"));

    assertThat(event.tags()).containsExactly(new Tag("A"));
  }

  @Test
  void constructorRejectsOwnerMismatchWithSubject() {
    Owner differentOwner = new Owner("DIFFERENT");
    assertThatThrownBy(() -> new Event(differentOwner.identifier(), SUBJECT, "value", null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("owner must match subject's owner");
  }

  // --- Builder.from tests ---

  @Test
  void fromRejectsNull() {
    assertThatThrownBy(() -> Event.from(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("event cannot be null");
  }

  @Test
  void fromCopiesAllFields() {
    Identifier id = new Identifier();
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("A"));
    Event original = new Event(OWNER.identifier(), SUBJECT, "original", id, ts, tags);

    Event copy = Event.from(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  void fromAllowsOverridingFields() {
    Event original = Event.builder().owner(OWNER).subject(SUBJECT).value("original").build();

    List<Tag> newTags = List.of(new Tag("NEW"));
    Event modified = Event.from(original)
        .tags(newTags)
        .build();

    assertThat(modified.ownerIdentifier()).isEqualTo(original.ownerIdentifier());
    assertThat(modified.subject()).isEqualTo(original.subject());
    assertThat(modified.value()).isEqualTo(original.value());
    assertThat(modified.identifier()).isEqualTo(original.identifier());
    assertThat(modified.timestamp()).isEqualTo(original.timestamp());
    assertThat(modified.tags()).containsExactly(new Tag("NEW"));
  }

  // --- Builder tests ---

  @Test
  void builderRejectsNullOwnerIdentifier() {
    assertThatThrownBy(() -> Event.builder().subject(SUBJECT).value("value").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void builderRejectsNullSubject() {
    assertThatThrownBy(() -> Event.builder().owner(OWNER).value("value").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subject cannot be null");
  }

  @Test
  void builderRejectsNullValue() {
    assertThatThrownBy(() -> Event.builder().owner(OWNER).subject(SUBJECT).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void builderWithDefaults() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("test").build();

    assertThat(event.ownerIdentifier()).isEqualTo(OWNER.identifier());
    assertThat(event.subject()).isEqualTo(SUBJECT);
    assertThat(event.value()).isEqualTo("test");
    assertThat(event.identifier()).isNotNull();
    assertThat(event.timestamp()).isNotNull();
    assertThat(event.tags()).isEmpty();
  }

  @Test
  void builderWithAllFields() {
    Identifier id = new Identifier();
    Timestamp ts = new Timestamp(Instant.parse("2026-01-01T00:00:00Z"));
    List<Tag> tags = List.of(new Tag("X"));

    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("test")
        .identifier(id)
        .timestamp(ts)
        .tags(tags)
        .build();

    assertThat(event.ownerIdentifier()).isEqualTo(OWNER.identifier());
    assertThat(event.subject()).isEqualTo(SUBJECT);
    assertThat(event.value()).isEqualTo("test");
    assertThat(event.identifier()).isSameAs(id);
    assertThat(event.timestamp()).isSameAs(ts);
    assertThat(event.tags()).containsExactly(new Tag("X"));
  }
}
