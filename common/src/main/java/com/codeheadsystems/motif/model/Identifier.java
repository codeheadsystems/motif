package com.codeheadsystems.motif.model;

import java.util.Objects;
import java.util.UUID;

public record Identifier(Class<?> clazz, UUID uuid) {

  public Identifier {
    Objects.requireNonNull(clazz, "class cannot be null");
    Objects.requireNonNull(uuid, "uuid cannot be null");
  }

  public Identifier(Class<?> clazz) {
    this(clazz, UUID.randomUUID());
  }

  public String formatted() {
    return String.format("%s:%s", clazz.getSimpleName(), uuid);
  }

}