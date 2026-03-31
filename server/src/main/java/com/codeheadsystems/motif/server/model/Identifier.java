package com.codeheadsystems.motif.server.model;

import java.util.Objects;
import java.util.UUID;

public record Identifier(UUID uuid) {

  public Identifier {
    Objects.requireNonNull(uuid, "uuid cannot be null");
  }

  public Identifier() {
    this(UUID.randomUUID());
  }

}
