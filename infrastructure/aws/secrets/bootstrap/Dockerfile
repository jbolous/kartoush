ARG BASE_IMAGE=postgres:16-bookworm
FROM ${BASE_IMAGE}
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl --fail --silent --show-error \
      https://truststore.pki.rds.amazonaws.com/us-east-2/us-east-2-bundle.pem \
      --output /etc/ssl/certs/rds-ca-bundle.pem \
    && chmod 0444 /etc/ssl/certs/rds-ca-bundle.pem
COPY create-app-role.sql /bootstrap/create-app-role.sql
USER postgres
ENTRYPOINT ["psql", "--no-psqlrc", "--quiet", "--set", "ON_ERROR_STOP=1", "--file", "/bootstrap/create-app-role.sql"]
