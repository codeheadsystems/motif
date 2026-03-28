package com.codeheadsystems.motif.model;

import java.util.Objects;

/**
 * Categories are strings of size 128 characters or less.
 * @param value
 */
public record Category(String value) {

  public Category {
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
