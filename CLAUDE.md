# Motif Project

## Build & Test

- Build: `./gradlew build`
- Test all: `./gradlew test`
- Test single class: `./gradlew :server:test --tests '*ClassName'`

## Architecture

Three-layer architecture in the `server` module under `com.codeheadsystems.motif.server`:

- **model**: Immutable Java records (`Owner`, `Subject`, `Event`, `Note`, `Category`, `Tag`, `Identifier`, `Timestamp`)
- **dao**: JDBI SqlObject interfaces with `@RegisterRowMapper` and default convenience methods
- **manager**: Business logic layer (`@Singleton`, `@Inject` constructor, Dagger DI)

All entities are multi-tenant, isolated by `Owner`.

## Record Builder Pattern

Records with multiple optional fields use an inner `Builder` class following this pattern:

- **`builder()`**: Static factory with no arguments. All fields are set via fluent setters on the builder.
- **`from(instance)`**: Static method on the record that returns a `Builder` pre-populated with all fields from an existing instance, allowing selective overrides. Delegates to a **private** `Builder.from()` method.
- **`Builder` constructor is private and no-arg**. Only `builder()` and `from()` on the record are public entry points.
- Optional/nullable record parameters are annotated with `@Nullable` from `org.jspecify.annotations`.
- The canonical constructor handles defaults for nullable fields (e.g. generating an `Identifier`, defaulting a `Timestamp`).

Example usage:
```java
// Create new
Event event = Event.builder()
    .owner(owner).subject(subject).value("value")
    .tags(List.of(new Tag("A")))
    .build();

// Copy and modify
Event modified = Event.from(event)
    .tags(List.of(new Tag("B")))
    .build();
```

## Testing

- Always run the full test suite (`./gradlew test`) after any change to ensure nothing is broken.
- Tests use JUnit Jupiter, AssertJ, and Testcontainers (PostgreSQL 16).
- DAO and Manager tests use real PostgreSQL via Testcontainers with Flyway migrations.

## Design Principles

- **Correctness over convenience.** Always choose the right behavior for the application, not the easiest implementation. If the correct approach requires more code, more layers, or a harder migration, do it anyway. Quick hacks and shortcuts accumulate into security vulnerabilities and architectural debt.
- **Security by default.** All data access must be owner-scoped. All user input must be validated server-side. Secrets must not be stored in plaintext or accessible to client code. Prefer HttpOnly cookies over client-accessible token storage. Assume any client-side check can be bypassed.
- **Validate at system boundaries.** Resource endpoints must validate and reject malformed input with 400 responses, not let exceptions bubble into 500s. Internal code between trusted layers does not need redundant validation.
