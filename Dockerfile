ARG BUILD_IMAGE=eclipse-temurin:25-jdk-noble
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre-noble

FROM ${BUILD_IMAGE} AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY app ./app
COPY auth ./auth
COPY customer ./customer
COPY notification ./notification
COPY platform ./platform
COPY test-support ./test-support
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew --no-daemon :app:bootJar
RUN set -eu; \
    for jar in app/build/libs/*.jar; do \
        case "$jar" in *-plain.jar) continue ;; esac; \
        test -z "${boot_jar:-}"; \
        boot_jar="$jar"; \
    done; \
    test -f "$boot_jar"; \
    cp "$boot_jar" /workspace/kartoush.jar

FROM ${RUNTIME_IMAGE} AS runtime
# script and cat are required for subsequent ECS Exec transcript delivery.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl bsdutils coreutils \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 kartoush \
    && useradd --uid 10001 --gid kartoush --no-create-home --shell /bin/sh kartoush
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/kartoush.jar ./kartoush.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    LOGGING_LEVEL_COM_KARTOUSH=INFO \
    LOGGING_LEVEL_ORG_HIBERNATE_SQL=WARN \
    LOGGING_LEVEL_ORG_HIBERNATE_ORM_JDBC_BIND=WARN \
    LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_TRANSACTION=INFO \
    LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_ORM_JPA=INFO
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl --fail --silent --show-error "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/kartoush.jar"]
