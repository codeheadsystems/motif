package com.codeheadsystems.motif.server.model;

import java.util.Objects;

/**
 * Tags are strings that are between 1 and 32 chars in size, inclusive.
 * They will be forced to be upper-case.
 *
 * @param value
 */
public record Tag(String value) {

  public Tag {
    value = Objects.requireNonNull(value, "value cannot be null")
        .strip()
        .toUpperCase();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value cannot be empty");
    }
    if (value.length() > 32) {
      throw new IllegalArgumentException("value cannot be longer than 32 characters");
    }
  }

}
