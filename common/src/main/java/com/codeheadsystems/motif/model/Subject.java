package com.codeheadsystems.motif.model;

import java.util.Objects;

/**
 * Subjects are value objects that live in a category. They are strings less than
 * 128 chars.
 *
 * @param category The category of the subject.
 * @param value The value of the subject.
 */
public record Subject(Category category, String value) {

  public Subject {
    Objects.requireNonNull(category, "category cannot be null");
    value = Objects.requireNonNull(value, "value cannot be null")
        .strip();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value cannot be empty");
    }
    if (value.length() > 128) {
      throw new IllegalArgumentException("value cannot be longer than 128 characters");
    }
  }
}
