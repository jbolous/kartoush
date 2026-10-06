# HTTPS ingress

This implementation follows the [Phase 1 architecture](../architecture/aws-phase-1-target-architecture.md) for #341. The single Fargate task contains the loopback-bound application, a short-lived certificate initializer, and an Nginx HTTPS proxy. Only TCP 443 is exposed through the existing application security group. There is no load balancer, NAT Gateway, HTTP listener, or public management endpoint.

## Hostname and DNS prerequisite

The selected hostname is `api.kartoush.dev`. On 2026-10-06, the project had no Route 53 hosted zone or ACM certificate. Public nameservers for `kartoush.dev` were registrar-managed. Domain ownership and DNS control must be confirmed before requesting a certificate or publishing records. Do not purchase a domain, change its nameservers, migrate unrelated records, or substitute a different hostname implicitly.

Use Ohio (`us-east-2`), profile `kartoush`, and project `790873128308`. Confirm the selected Region in AWS Settings > View all projects > Overview > Additional Info > Region. The service remains at desired count zero until certificate material and deployment checks are ready.

Once DNS control is available, use the agreed Route 53 public zone. Preserve existing mail, website, and verification records during any explicitly authorized migration. ACM DNS validation requires its CNAME to be publicly resolvable; creating a hosted zone without delegating it does not accomplish that. Keep the validation CNAME for renewal.

## Proxy image and route policy

Build from `infrastructure/aws/ingress/proxy/` for Linux amd64. The base is digest-pinned Nginx 1.30.5 on Debian trixie. The image contains `script` and `cat` for future ECS Exec transcript support. The running proxy uses UID/GID 10001, a read-only root filesystem, and a writable task scratch volume. A task network sysctl permits the non-root proxy to bind 443; the security group still exposes only that port.

The tested image is published in the private Ohio ECR repository. Its immutable release tag, digest, source commit, and size are recorded in [image-publication.json](../../infrastructure/aws/ingress/image-publication.json). Live combined-task validation and public DNS publication remain pending domain ownership and DNS confirmation.

The proxy checks the original request target against exact method/path patterns. It does not normalize an encoded or ambiguous path into an allowed endpoint. Default routes, encoded paths, repeated slashes, dot-segments, unknown hosts, and direct-IP requests are rejected. TLS 1.2 and 1.3 are supported. Unknown SNI is rejected during the handshake.

| Public route | Allowed methods |
| --- | --- |
| `/api/auth/sign-in` | POST |
| `/api/auth/password-reset` | POST |
| `/api/auth/password-reset/confirm` | POST |
| `/api/customers` | POST |
| `/api/customers/{canonical ULID}` | GET, HEAD, PUT, DELETE |
| `/api/customers/{canonical ULID}/activation` | POST |
| `/api/customers/{canonical ULID}/initial-password` | POST |
| `/api/customers/{canonical ULID}/activation/resend` | POST |
| `/api/terms-of-service/current` and `/api/terms-of-service/{version}` | GET, HEAD |

Everything else is blocked, including `/internal/**`, `/dev/**`, `/actuator/**`, OpenAPI/Swagger, and JobRunr dashboard paths. Authentication and customer ownership checks remain in Spring Security. The proxy imposes a 64 KiB body limit and finite connection/body/read timeouts; application rate limiting and abuse prevention remain #344.

The proxy preserves request bodies and Authorization headers, replaces client-supplied forwarding headers, and forwards over task loopback. The application uses native forwarded-header handling and remains bound to `127.0.0.1`. Access logs contain client address, method, status, and duration only. Request-level Nginx error logging is suppressed because its messages can include secret-bearing query strings. Log retention is seven days.

## ACM and managed TLS material

Request one DNS-validated, export-enabled ACM public certificate for the exact hostname in Ohio. Do not add wildcard or extra names. Exportable public certificates are charged at issuance and renewal; the architecture budgets $7 for one exact hostname per certificate billing event. Re-export a renewed certificate rather than requesting a replacement on every startup.

After ACM reports the certificate issued, store its bundle using:

```sh
python3 infrastructure/aws/ingress/store-tls-secret.py --profile kartoush \
  --certificate-arn CERTIFICATE_ARN
```

The helper verifies the project, Region, hostname, exportability, and expiry. It generates an export passphrase in memory, decrypts the returned private key through OpenSSL, and writes the certificate, chain, and usable key directly to `/kartoush/demo/tls` in Secrets Manager. It prints metadata only. No private key or passphrase enters disk, image layers, source, or command-line arguments. Never enable SDK debug logging for this operation.

The TLS secret uses AWS-managed encryption, separate from application credentials and the RDS master secret. The existing execution role can retrieve the TLS secret; the application task role cannot. Only the initializer receives TLS secret values as environment variables. The proxy receives files, not those environment variables, and the application has no TLS volume mount.

At startup the initializer verifies the trusted certificate chain, hostname, remaining validity, and matching key. It writes the task-scoped files with mode 0400 under a mode-0700 directory owned by UID/GID 10001. The proxy mounts them read-only and waits for successful initialization. Its entrypoint then waits up to five minutes for local application readiness before opening the HTTPS listener, accommodating the measured small-task cold start without requiring a long ECS container-dependency timeout.

## Deploy without starting compute

Publish the proxy to the existing private ECR repository using an immutable release tag, then use its digest and the actual TLS secret ARN:

```sh
python3 infrastructure/aws/ingress/deploy-ingress.py --profile kartoush \
  --proxy-image PROXY_ECR_DIGEST_URI --tls-secret-arn TLS_SECRET_ARN
```

The helper refuses a wrong project, unpinned/unexpected image, mismatched secret, or a service with any desired/running/pending tasks. It registers the combined template and updates the service at desired count zero. The template contains explicit replacement markers and must not be registered directly. Application memory remains 768 MiB, proxy 128 MiB, and transient initializer 64 MiB within the approved 1 GiB/0.25 vCPU task.

The proxy health check verifies the local HTTPS connection and hostname; its public terms route may legitimately return 404 until terms exist. Application health is checked independently. A healthy proxy does not by itself establish that every public route works or that application authorization succeeds.

## Startup, deployed validation, and shutdown

Use a bounded startup procedure with cleanup armed before scaling up. The existing ECS validation script restores desired count zero on exit, so do not publish a persistent A record during that short validation without also arranging its deletion before shutdown.

Discover the healthy task's ENI and current public IPv4. Before DNS publication, use a hostname-valid client with an explicit address override to verify public TLS, expected API responses, and blocked routes. Use representative requests and inspect application/proxy memory headroom before accepting the combined task. Verify that HTTP 80, application 8080, PostgreSQL 5432, and dashboard ports are unreachable externally.

Only after the checks pass, publish `api.kartoush.dev` as a standard A record with TTL 60 to the current task address. Fargate task addresses are not supported Route 53 alias targets. Confirm the change is INSYNC and validate through public DNS. Do not publish DNS for a failed, unhealthy, or certificate-invalid task.

On shutdown, delete the matching API A record before scaling the service to zero. Verify desired/running/pending counts zero and ENI/address release. Keep the zone and ACM validation CNAME. A task replacement changes the address: reconcile or remove stale DNS before declaring availability. There is no automatically maintained stable ingress address.

## Renewal and revocation

ACM renewal does not update exported files in a standalone container. Monitor certificate expiry, preserve DNS validation, and deliberately refresh the existing secret using `store-tls-secret.py --rotate`. Then replace the task, validate TLS and routes, and reconcile its new address. The initializer refuses a certificate with less than one hour remaining; it cannot renew a certificate itself.

For suspected key compromise, remove public DNS and stop the service, obtain replacement certificate material through the authorized certificate process, refresh the TLS secret, and validate replacement tasks before publishing DNS. Changing secret permissions alone does not erase material already loaded into a task. Do not delete certificate/validation resources until their lifecycle and any revocation requirements have been assessed.

## Focused local validation

With Docker and OpenSSL available:

```sh
docker build --platform linux/amd64 -t kartoush-proxy:local infrastructure/aws/ingress/proxy
python3 infrastructure/aws/ingress/test_proxy.py
```

These container checks create an ephemeral test CA, verify certificate materialization and restrictive key permissions, and run a mock backend over the task-style shared network. They exercise allowed/blocked methods and paths, encoded/normalized variants, Host/SNI rejection, body/Authorization forwarding, trusted headers, and sanitized log delivery. Test containers, volume, and certificate files are removed afterward. This does not run Java integration suites or establish deployed AWS acceptance.

## AWS references

- [Exportable ACM public certificates](https://docs.aws.amazon.com/acm/latest/userguide/acm-exportable-certificates.html)
- [ACM pricing](https://aws.amazon.com/certificate-manager/pricing/)
- [Fargate task parameters and system controls](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definition_parameters.html)
- [Nginx security advisories](https://nginx.org/en/security_advisories.html)
