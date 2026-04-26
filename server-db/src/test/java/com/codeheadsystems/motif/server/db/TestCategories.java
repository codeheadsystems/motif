package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;

/**
 * Test helper for constructing Category fixtures with sensible defaults so existing tests can
 * focus on the field they actually care about (typically just the name).
 */
public final class TestCategories {

  public static final String DEFAULT_COLOR = "#9CA3AF";
  public static final String DEFAULT_ICON = "tag";

  private TestCategories() {}

  public static Category of(Identifier ownerIdentifier, String name) {
    return Category.builder()
        .ownerIdentifier(ownerIdentifier)
        .name(name)
        .color(DEFAULT_COLOR)
        .icon(DEFAULT_ICON)
        .build();
  }
}
