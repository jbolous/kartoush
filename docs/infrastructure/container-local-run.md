# Run Kartoush in a Container

The root Dockerfile builds the application with Java 25 and the Gradle wrapper, then copies its executable JAR into a Java 25 runtime image. The runtime runs as UID/GID `10001`, uses the `prod` profile and loopback listener (`SERVER_ADDRESS=127.0.0.1`) by default, and includes `curl`, `script`, and `cat` for health checks and later operational transcript support. Gradle and application sources stay in the build stage. Build arguments `BUILD_IMAGE` and `RUNTIME_IMAGE` can override the base images with approved digest-pinned references.

## Local Compose Setup

The standalone `compose.container.yaml` runs the app and PostgreSQL 16 in an isolated `kartoush-container` project. It does not change the existing PostgreSQL-only `compose.yaml` workflow. Docker with BuildKit must be running.

1. Copy `.env.container.example` to `.env.container`.
2. Fill the database password, internal administrator username/password, and activation-email encryption key. Generate the encryption key once with `openssl rand -base64 32`. Keep it stable while retaining the database volume, because queued jobs depend on it.
3. Choose an available host port through `KARTOUSH_HTTP_PORT`; `SERVER_PORT` controls the port inside the app container.
4. Start the stack:

```bash
docker compose --env-file .env.container -f compose.container.yaml up --build -d --wait
curl --fail http://localhost:8080/actuator/health
```

Use the selected host port in the health URL and Bruno's `host` variable. PostgreSQL is accessible only inside the Compose network; the application port binds to host loopback. The application waits for PostgreSQL readiness, then Flyway migrates the database and Hibernate validates it. The image health check follows `SERVER_PORT`.

The local container uses the production security configuration with explicit administrator credentials and quieter logs. Email delivery and the JobRunr dashboard are disabled in this Compose setup. It is suitable for startup/API checks; activation and reset email flows require configuring an actual delivery provider separately. Developer action pages are unavailable under the production profile.

Database credentials, administrator credentials, and the encryption key are runtime environment values. `.env.container` is ignored by Git, and `.env*` files and common credential file extensions are excluded from the Docker build context. Do not add credentials to Docker build arguments or the Dockerfile.

## Image and Runtime Configuration

Build the image independently:

```bash
docker build -t kartoush-app:local .
```

The build executes only `:app:bootJar`, not application tests. On an ARM machine, the default build is ARM64. Build and validate the deployment architecture before publishing; #176 owns ECR publication and release digest pinning. No image is pushed and no AWS resources are created by these commands.

The executable image uses Spring Boot's environment configuration. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `KARTOUSH_INTERNAL_ADMIN_USERNAME`, `KARTOUSH_INTERNAL_ADMIN_PASSWORD`, and `KARTOUSH_JOBS_ACTIVATION_EMAIL_ENCRYPTION_KEY` for a production-profile runtime. The default production email provider also needs its delivery settings/API key unless delivery is explicitly disabled. Use `SERVER_PORT` for the listener port and `SERVER_ADDRESS` for binding. The image defaults to loopback for the later AWS task behind its HTTPS proxy; local Compose explicitly overrides the address to `0.0.0.0` so its loopback-only host publication can reach the app.

The local Compose app is limited to 0.25 CPU and 768 MiB without extra swap, leaving 256 MiB of the planned 1 GiB task budget for the proxy and operational overhead. PostgreSQL runs separately, as RDS will in AWS. No JVM memory limit is hardcoded in the image. Set container memory and any `JAVA_TOOL_OPTIONS` through the runtime environment, and validate the complete task's memory usage before accepting the AWS task size. `EXPOSE 8080` documents the default; it does not publish a port or override `SERVER_PORT`. The Docker health check validates task-local HTTP health and does not replace the AWS proxy's public route restrictions.

## Stop and Restart

Stop the local stack while retaining database state:

```bash
docker compose --env-file .env.container -f compose.container.yaml down
```

Run `up -d --wait` again to reuse the named volume. Changing the PostgreSQL environment credentials does not rewrite credentials already initialized in that volume.

Only when the local database is disposable, remove its state:

```bash
docker compose --env-file .env.container -f compose.container.yaml down --volumes
```

The last command deletes the Compose database volume. It does not remove the application image or the build cache.

## Validation

The image was built and run locally on ARM64 with Java 25. The production-profile application passed its health check with host port `18080` and container port `18081`, and Flyway applied all 16 current migrations. Smoke checks exercised authenticated Terms of Service creation/activation, public terms retrieval, customer registration, and duplicate rejection. Terms and customer state survived an application restart. The runtime user was `10001`; `script` and `cat` were present, while the JDK/compiler and build workspace were absent. Compose configuration and documentation style checks passed. The disposable smoke stack and its database volume were removed after verification. A subsequent ARM64 check passed startup, Flyway, health, and an authenticated database-backed customer query with the app capped at 0.25 CPU and 768 MiB without extra swap. Memory usage sampled after the request was approximately 305 MiB; this is a smoke-check sample, not a peak or load measurement. The disposable constrained stack was removed. AWS transcript delivery and complete Linux/x86 task sizing with the proxy remain deployment acceptance checks.

## CI Checks

Application/module, Gradle, and CI workflow changes run the Java unit and integration suites. Container packaging and Docker source/build input changes run a dedicated image-build and Compose startup check with generated disposable credentials and non-default ports. Markdown documentation, Bruno collections, and IAM policy artifacts do not trigger the Java suites. The required verification job checks that each selected job succeeds and each unselected job is skipped; container failures therefore block merging. Qodana watches application/build paths, `.editorconfig`, and its own configuration and workflow. CI verifies the app container has the CPU, memory, and swap limits described above.

## RDS Certificate Bundle

The runtime image downloads the public Ohio RDS CA bundle from AWS over verified HTTPS at build time and stores it read-only at `/app/rds-ca-bundle.pem`, readable by the non-root application user. Supply the RDS endpoint hostname and the bundled certificate path in the runtime JDBC URL:

```text
jdbc:postgresql://RDS_ENDPOINT:5432/kartoush?currentSchema=kartoush&sslmode=verify-full&sslrootcert=/app/rds-ca-bundle.pem
```

The bundle is a public trust artifact, not a database credential or private key. The JDBC settings remain externalized; the local Compose database does not use RDS certificates. Rebuild the runtime stage without cache when refreshing the bundle for CA changes and republish the new image digest. Do not replace `verify-full` with a mode that permits plaintext fallback. See [AWS PostgreSQL TLS guidance](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/PostgreSQL.Concepts.General.SSL.html). Verified connections to the actual RDS database remain part of #180.
