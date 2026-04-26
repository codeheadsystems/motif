package com.codeheadsystems.motif.server.db.model;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Category groups Subjects within an Owner. Owns a display color (hex #RRGGBB) and a logical
 * icon name (e.g. {@code "house"}, {@code "heart"}) which the webapp resolves to a concrete
 * SVG component.
 *
 * @param ownerIdentifier owner this category belongs to
 * @param name            display name, 1–128 characters
 * @param color           hex color, exactly 7 characters in {@code #RRGGBB}
 * @param icon            logical icon name, 1–64 characters
 * @param identifier      stable identifier; auto-generated if null
 */
public record Category(
    Identifier ownerIdentifier,
    String name,
    String color,
    String icon,
    @Nullable Identifier identifier) {

  private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

  public Category {
    Objects.requireNonNull(ownerIdentifier, "ownerIdentifier cannot be null");
    name = Objects.requireNonNull(name, "name cannot be null").strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be empty");
    }
    if (name.length() > 128) {
      throw new IllegalArgumentException("name cannot be longer than 128 characters");
    }
    color = Objects.requireNonNull(color, "color cannot be null").strip();
    if (!HEX_COLOR.matcher(color).matches()) {
      throw new IllegalArgumentException("color must be #RRGGBB hex (got: " + color + ")");
    }
    icon = Objects.requireNonNull(icon, "icon cannot be null").strip();
    if (icon.isEmpty()) {
      throw new IllegalArgumentException("icon cannot be empty");
    }
    if (icon.length() > 64) {
      throw new IllegalArgumentException("icon cannot be longer than 64 characters");
    }
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
  }

  public static Builder from(Category category) {
    return Builder.from(category);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Identifier ownerIdentifier;
    private String name;
    private String color;
    private String icon;
    private Identifier identifier;

    private Builder() {
    }

    private static Builder from(Category category) {
      Objects.requireNonNull(category, "category cannot be null");
      Builder builder = new Builder();
      builder.ownerIdentifier = category.ownerIdentifier();
      builder.name = category.name();
      builder.color = category.color();
      builder.icon = category.icon();
      builder.identifier = category.identifier();
      return builder;
    }

    public Builder owner(Owner owner) {
      return ownerIdentifier(owner.identifier());
    }

    public Builder ownerIdentifier(Identifier ownerIdentifier) {
      this.ownerIdentifier = ownerIdentifier;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder color(String color) {
      this.color = color;
      return this;
    }

    public Builder icon(String icon) {
      this.icon = icon;
      return this;
    }

    public Builder identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
    }

    public Category build() {
      return new Category(ownerIdentifier, name, color, icon, identifier);
    }
  }
}
