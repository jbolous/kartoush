# Conventions Memory

## Java and Spring style

- Use constructor injection
- Prefer `final` parameters and immutable values where practical
- Simple HTTP request and response shapes are commonly Java `record`s
- Explicit Spring stereotypes are used for controllers, services, and configuration classes
- Logger pattern is `private static final Logger LOG = LoggerFactory.getLogger(...)`

## Naming

- Tests use `should<ExpectedBehavior>When<Condition>`
- Implementation classes commonly use `Default...`
- Branch names follow `<type>/<scope>-<short-description>`
- Use lowercase kebab-case in branch names

## Boundary and model usage

- Do not leak JPA entities outside the owning module
- Do not pass domain objects across HTTP boundaries
- Prefer value types from `platform-types` instead of raw strings where the domain already has a type
- Controller code should stay focused on HTTP concerns and delegation
- Facade validators own business and module-specific validation
- App-layer validation is for request shape and binding concerns

## Logging

- Avoid logging raw activation, password setup, or password reset tokens
- Prefer stable identifiers and provider message ids over secrets
- Log useful request context such as customer id or email only when it is not a secret-bearing value

## Mapping and DTOs

- Customer persistence still uses MapStruct mappers in `customer.persistence.mapper`
- Do not assume MapStruct is the default answer elsewhere
- Prefer explicit mapping when it keeps boundaries clearer

## Notification code

- Keep provider-specific code in provider packages
- Keep low-level HTTP concerns behind shared notification HTTP and API client types
- Do not couple customer or auth flows directly to provider SDKs or APIs
