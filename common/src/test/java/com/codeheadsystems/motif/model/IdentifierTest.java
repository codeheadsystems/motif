package com.codeheadsystems.motif.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class IdentifierTest {

  @Test
  void canonicalConstructorStoresClassAndUuid() {
	UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	Identifier identifier = new Identifier(TestPerson.class, uuid);

	assertThat(identifier.clazz()).isEqualTo(TestPerson.class);
	assertThat(identifier.uuid()).isEqualTo(uuid);
  }

  @Test
  void canonicalConstructorRejectsNullClass() {
	UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	assertThatThrownBy(() -> new Identifier(null, uuid))
		.isInstanceOf(NullPointerException.class)
		.hasMessage("class cannot be null");
  }

  @Test
  void canonicalConstructorRejectsNullUuid() {
	assertThatThrownBy(() -> new Identifier(TestPerson.class, null))
		.isInstanceOf(NullPointerException.class)
		.hasMessage("uuid cannot be null");
  }

  @Test
  void convenienceConstructorPreservesClassAndGeneratesUuid() {
	Identifier identifier = new Identifier(TestPerson.class);

	assertThat(identifier.clazz()).isEqualTo(TestPerson.class);
	assertThat(identifier.uuid()).isNotNull();
	assertThat(identifier.formatted())
		.isEqualTo(TestPerson.class.getSimpleName() + ":" + identifier.uuid());
  }

  @Test
  void formattedUsesSimpleClassNameAndUuid() {
	UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	Identifier identifier = new Identifier(TestPerson.class, uuid);

	assertThat(identifier.formatted())
		.isEqualTo(TestPerson.class.getSimpleName() + ":123e4567-e89b-12d3-a456-426614174000");
  }

  @Test
  void equalityAndHashCodeDependOnClassAndUuid() {
	UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	Identifier left = new Identifier(TestPerson.class, uuid);
	Identifier same = new Identifier(TestPerson.class, uuid);
	Identifier differentClass = new Identifier(TestGroup.class, uuid);
	Identifier differentUuid = new Identifier(TestPerson.class,
		UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));

	assertThat(left)
		.isEqualTo(same)
		.hasSameHashCodeAs(same)
		.isNotEqualTo(differentClass)
		.isNotEqualTo(differentUuid);
  }

  private static final class TestPerson {
  }

  private static final class TestGroup {
  }


}