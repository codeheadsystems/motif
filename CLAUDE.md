# Motif Project

## Build & Test

- Build: `./gradlew build`
- Test all: `./gradlew test`
- Test single class: `./gradlew :common:test --tests '*ClassName'`

## Record Builder Pattern

Records with multiple optional fields use an inner `Builder` class following this pattern:

- **`builder(...)`**: Static factory taking only the required fields. Optional fields have fluent setters on the builder.
- **`from(instance)`**: Static method on the record that returns a `Builder` pre-populated with all fields from an existing instance, allowing selective overrides. Delegates to a **private** `Builder.from()` method.
- **`Builder` constructor is private**. Only `builder()` and `from()` on the record are public entry points.
- Optional/nullable record parameters are annotated with `@Nullable` from `org.jspecify.annotations`.
- The canonical constructor handles defaults for nullable fields (e.g. generating an `Identifier`, defaulting a `Timestamp`).

Example usage:
```java
// Create new
Event event = Event.builder(subject, "value")
    .tags(List.of(new Tag("A")))
    .build();

// Copy and modify
Event modified = Event.from(event)
    .tags(List.of(new Tag("B")))
    .build();
```

## Testing

- Always run the full test suite (`./gradlew test`) after any change to ensure nothing is broken.
- Tests use JUnit Jupiter and AssertJ.
