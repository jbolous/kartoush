ARG BASE_IMAGE=nginx:stable-trixie@sha256:9bf97bd7714f5e24c1ccd545ecb9eb5435cb6d109c97cebb15e7e455e0239edb
FROM ${BASE_IMAGE}
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl openssl ca-certificates bsdutils coreutils \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 kartoush \
    && useradd --uid 10001 --gid kartoush --no-create-home --shell /bin/sh kartoush \
    && mkdir -p /tls /tmp/nginx \
    && chown 10001:10001 /tls /tmp/nginx
COPY nginx.conf /etc/nginx/nginx.conf
COPY materialize-tls.sh start-proxy.sh /opt/kartoush/
USER 10001:10001
EXPOSE 443
ENTRYPOINT ["bash", "/opt/kartoush/start-proxy.sh"]
