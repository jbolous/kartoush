# ADR 0023: Testing Strategy and Conventions

## Status

Accepted

## Context

Kartoush needs a clear and lightweight testing strategy so contributors can make consistent choices about what to test, how to structure tests, and which test style to use for each layer of the system.

As the codebase grows, unclear testing boundaries increase the risk of duplicated coverage, fragile tests, inconsistent naming, and confusion about when to use mocks versus real infrastructure. Recent work on customer lifecycle behavior, exception handling, and persistence testing highlighted the need for a shared standard.

Kartoush already uses a mix of unit tests, web tests, and persistence integration tests. This ADR defines the intended purpose of each testing layer and establishes conventions for readability and consistency.

## Decision

Kartoush will use a layered testing strategy with explicit responsibilities for each test type.

### Unit tests

Unit tests verify isolated logic without requiring Spring application context startup unless there is a strong justification.

Unit tests should be the default choice for:
- domain behavior
- validation logic
- mapping logic
- lifecycle rules
- exception behavior
- small utility classes

Unit tests should prefer direct object construction over framework bootstrapping. Mocks may be used when collaborating dependencies are required and isolation provides meaningful value.

### Persistence tests

Persistence tests verify JPA mappings, repository behavior, entity lifecycle callbacks, and behavior that depends on a real database.

Persistence tests should use:
- `@DataJpaTest`
- Testcontainers
- `@ServiceConnection`

Persistence tests should prefer a real PostgreSQL database over mocks or in-memory database substitutes when testing persistence behavior.

### Web/API tests

Web tests verify controller contracts and HTTP behavior, including:
- request binding
- validation errors
- response codes
- response payloads
- exception-to-response mapping

Web tests should use focused MVC test slices rather than full application startup unless there is a strong reason to test broader wiring.

### Integration tests

Integration tests verify behavior across collaborating components where wiring, transactions, module boundaries, or infrastructure interaction are part of the behavior under test.

Integration tests may use Spring context startup and real infrastructure when the goal is to verify module integration rather than isolated logic.

Integration tests should be used selectively because they are slower and more expensive than unit tests.

## Test structure conventions

Tests should use clear method names in the following format:

`should<ExpectedBehavior>When<Condition>`

Examples:
- `shouldCreateCustomerWhenValidInputProvided`
- `shouldThrowExceptionWhenCustomerIdIsMissing`
- `shouldNotReactivateCustomerWhenStatusIsPending`

Tests should use `given`, `when`, and `then` comments when doing so improves readability.

Example:

    @Test
    void shouldNormalizeEmailWhenCreatingCustomer() {
        // given
        CustomerEntity entity = ...

        // when
        entity.onCreate();

        // then
        assertThat(entity.getEmail()).isEqualTo(...);
    }

For exception tests, combining `when / then` is acceptable when that is clearer.

`@DisplayName` is not part of the standard Kartoush approach. Clear test method names are preferred instead.

## Mocking guidance

Mocks are appropriate when:
- isolating a unit of logic
- simulating collaboration failures
- avoiding unrelated infrastructure in unit tests

Mocks should not replace real infrastructure in persistence tests.

Where test confidence depends on actual database behavior, actual Spring wiring, or real HTTP request handling, mocks should not be used as a substitute for the behavior under test.

## Consequences

### Positive

This decision:
- makes testing expectations clearer
- reduces inconsistency in naming and structure
- encourages fast unit tests for isolated logic
- preserves real database confidence for persistence behavior
- improves readability in reviews and maintenance

### Negative

This decision:
- adds some upfront discipline to test writing
- may require incremental cleanup of older tests over time
- does not automatically enforce conventions without future tooling or review discipline

## Scope and adoption

This ADR applies to new tests and to existing tests that are modified as part of normal feature work.

This ADR does not require immediate retrofitting of the entire existing test suite.

## Alternatives considered

### Ad hoc testing decisions

Continue allowing contributors to choose test style and structure case by case without a documented standard.

This was rejected because it leads to inconsistent naming, unclear boundaries, and more review churn.

### Heavy enforcement through tooling first

Introduce automated enforcement for naming and structure before documenting expectations.

This was rejected for now because the immediate need is clarity and shared guidance, not process overhead. Tooling can be added later if needed.

## Related Decisions

- ADR 0002: Architecture Style
- ADR 0003: Data Ownership
- ADR 0011: Layering and Facade Contracts