#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${TLS_CERTIFICATE_PEM:?Certificate is required}"
: "${TLS_CERTIFICATE_CHAIN_PEM:?Certificate chain is required}"
: "${TLS_PRIVATE_KEY_PEM:?Private key is required}"
# This script runs once as root; only the proxy mounts the resulting files afterward.
install -d -m 0700 -o 10001 -g 10001 /tls /tmp/nginx
printf '%s\n' "$TLS_CERTIFICATE_PEM" > /tls/certificate.pem
printf '%s\n' "$TLS_CERTIFICATE_CHAIN_PEM" > /tls/chain.pem
printf '%s\n%s\n' "$TLS_CERTIFICATE_PEM" "$TLS_CERTIFICATE_CHAIN_PEM" > /tls/fullchain.pem
printf '%s\n' "$TLS_PRIVATE_KEY_PEM" > /tls/private-key.pem
unset TLS_CERTIFICATE_PEM TLS_CERTIFICATE_CHAIN_PEM TLS_PRIVATE_KEY_PEM
openssl x509 -in /tls/certificate.pem -noout -checkend 3600 >/dev/null
openssl verify -purpose sslserver -verify_hostname api.kartoush.dev \
    -untrusted /tls/chain.pem /tls/certificate.pem >/dev/null
certificate_key=$(openssl x509 -in /tls/certificate.pem -pubkey -noout | openssl pkey -pubin -outform DER | sha256sum)
private_key=$(openssl pkey -in /tls/private-key.pem -pubout -outform DER | sha256sum)
[[ "$certificate_key" == "$private_key" ]] || { echo 'TLS certificate and key do not match' >&2; exit 1; }
chown 10001:10001 /tls/*.pem
chmod 0400 /tls/*.pem
