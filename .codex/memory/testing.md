# Testing Memory

## Repo testing strategy

- `./gradlew verifyAll` is the repo-wide verification command recommended in `README`
- Root `verifyAll` depends on `test` in all subprojects and any `integrationTest` tasks that exist

## Test layers

- Unit tests
  - Default choice for isolated logic
  - No Spring context unless there is a strong reason
- Persistence tests
  - Use real PostgreSQL behavior
  - Commonly `@DataJpaTest`, Testcontainers, and `@ServiceConnection`
- Web tests
  - Focused MVC slices for controllers and HTTP behavior
- Integration tests
  - Spring context plus collaborating components and real infrastructure when needed

## Current Gradle task shape

- `app`, `auth`, and `customer` define custom `integrationTest` tasks
- Integration tests are tagged with `integration`
- `notification` currently has unit tests only

## Shared test support

- `test-support` provides shared annotations and base support, including:
  - `@IntegrationTest`
  - `@SpringIntegrationTest`
  - `@HttpSpringIntegrationTest`
  - `@PostgresDataJpaTest`
  - `@PostgresSpringIntegrationTest`
  - `@PostgresRestAssuredIntegrationTest`

## Useful commands

- All tests:
  - `./gradlew verifyAll`
- Unit tests only:
  - `./gradlew test`
- App API integration:
  - `./gradlew --no-daemon :app:integrationTest --tests 'com.kartoush.api.auth.*'`
  - `./gradlew --no-daemon :app:integrationTest --tests 'com.kartoush.api.customer.*'`
  - `./gradlew --no-daemon :app:integrationTest --tests 'com.kartoush.api.terms.*'`
- Customer module integration:
  - `./gradlew --no-daemon :customer:integrationTest`
- Auth module integration:
  - `./gradlew --no-daemon :auth:integrationTest`

## CI coverage

- CI runs:
  - `unit-tests`
  - API integration matrix for `auth`, `customer`, and `terms`
  - `customer-module-integration-tests`
  - `auth-module-integration-tests`
- `verify` checks those upstream jobs

## Test-writing conventions

- Prefer method names in `shouldXWhenY` form
- Use `given / when / then` comments only when they improve readability
- Do not use `@DisplayName` as the normal style
- Do not replace real persistence behavior with mocks in repository tests
