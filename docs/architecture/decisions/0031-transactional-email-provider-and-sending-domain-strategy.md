# ADR 0031: Transactional Email Provider and Sending Domain Strategy

## Status

Accepted

## Context

Kartoush now has customer-facing email flows for:

- Customer activation
- Initial password setup
- Password reset

Task `#274` established a shared email delivery boundary so these flows no
longer depend directly on provider-specific code.

That leaves a new set of decisions that should be made before provider adapter
work begins:

- Which provider Kartoush should use for local and test email inspection
- Which provider Kartoush should use for initial production transactional sending
- Which domain and sender-address strategy Kartoush should use
- Which DNS records are actually required for transactional sending
- How secrets and non-secret configuration should be handled by environment

Kartoush owns both `kartoush.com` and `kartoush.dev`, but neither domain
currently needs full mail hosting for this phase.

The immediate need is one-way transactional sending. That means Kartoush needs
DNS control for provider verification and deliverability records, not inbound
mail handling.

Task `#275` needs a decision that is specific enough to guide `#276` and later
email work without coupling the application code directly to one provider SDK.

## Decision

Kartoush will use the following transactional email strategy:

1. The application will keep email delivery behind provider-agnostic internal
   email abstractions
2. Mailtrap will be the preferred provider for local, development, and test
   email inspection when real email output needs to be reviewed
3. Brevo will be the preferred initial production transactional email provider
4. Kartoush will send transactional email from a dedicated sending subdomain
   rather than the root domain
5. The initial preferred sending subdomain will be `notify.kartoush.com`
6. Transactional sending does not require MX records or full mail hosting for
   the sending subdomain
7. Kartoush will manage SPF, DKIM, DMARC, and provider verification records as
   part of the sending-domain setup
8. Non-secret email behavior configuration may be committed in application
   config, but provider credentials must remain outside source control
9. GitHub secrets are for CI and workflow automation only, while runtime
   production secrets should live in AWS-managed secret storage

## Provider Strategy

Kartoush will separate email concerns into two categories:

- Message composition and internal delivery contracts
- Provider-specific delivery adapters

The application code should depend only on internal email contracts such as
the shared email boundary introduced in task `#274`.

Provider-specific concerns such as SMTP details, API clients, and provider
message identifiers should stay behind infrastructure adapters.

### Local, development, and test behavior

Mailtrap is the preferred local and development provider when the team wants to
inspect actual email output without sending real customer email.

This supports:

- Local manual flow testing
- Development email inspection
- Safe shared preview or sandbox behavior

Not every test needs Mailtrap.

Unit and integration tests may still use no-op or capture-style email delivery
where direct provider interaction adds no value.

### Early real transactional sending

Brevo is the preferred initial production transactional email provider.

Brevo should be used only through a Kartoush adapter so that:

- Delivery code is not scattered across customer or auth flows
- Provider SDK assumptions do not leak into application logic
- Kartoush can change providers later with less disruption

## Sending Domain Strategy

Kartoush should use a dedicated sending subdomain instead of the root domain.

The preferred initial sending subdomain is `notify.kartoush.com`.

This keeps transactional sending isolated from future website or root-domain
uses and makes the purpose of the sending domain clearer.

Examples of acceptable sender addresses include:

- `no-reply@notify.kartoush.com`
- `activation@notify.kartoush.com`
- `password-reset@notify.kartoush.com`

The exact sender names and addresses remain configuration choices, but they
should stay on the dedicated sending subdomain unless a later ADR changes this
strategy.

## DNS Requirements

Transactional email sending for Kartoush requires DNS setup, but it does not
require inbound mail hosting.

The sending subdomain should be configured with:

- Provider verification TXT or CNAME records required by the chosen provider
- SPF records for permitted senders
- DKIM records for message signing
- A DMARC policy for domain-level delivery guidance and reporting

Kartoush does not need MX records for the sending subdomain in order to send
transactional email.

MX records are only needed if Kartoush later wants that subdomain to receive
mail.

## Configuration and Secrets

Kartoush should keep a clear split between normal configuration and secrets.

### Non-secret configuration

The following may live in committed application configuration or environment
configuration:

- Sender display name
- Sender email address
- Sending subdomain choice
- Activation and password reset base URLs
- Provider selection flags
- Feature enablement flags

### Secret configuration

The following must not be committed:

- Brevo API keys
- Mailtrap API or SMTP credentials
- Any later webhook signing secrets
- Any later provider private credentials

### Secret handling by environment

Kartoush will use:

- GitHub secrets for CI and workflow automation needs
- AWS Secrets Manager or AWS Systems Manager Parameter Store for runtime
  production secrets
- Per-developer credentials, Mailtrap sandboxes, or no-op profiles for local
  development when real provider access is needed

This keeps shared code and committed configuration free of provider secrets.

## Spring Profile Direction

Kartoush should support profile-based email behavior.

The intended direction is:

- `dev` and similar local profiles use Mailtrap, sandbox, or no-op delivery depending on what the developer needs
- `test` uses no-op or capture delivery by default
- `prod` uses Brevo-backed transactional delivery

This ADR sets the direction. The actual adapter wiring belongs in follow-up
implementation work.

## Logging and Delivery References

Kartoush should avoid logging raw externally usable tokens in activation,
password setup, or password reset email flows.

When a concrete provider exposes a delivery reference such as a provider
message id, Kartoush should log that reference instead of logging raw tokens or
provider secrets.

This improves supportability without leaking sensitive values into logs.

## Out of Scope

This ADR does not settle:

- The exact Brevo adapter implementation
- The exact Mailtrap integration style
- Whether Kartoush later adopts internal HTML templating, provider-managed
  templates, or another rendering approach
- Operational alerting or bounce-handling workflows
- Website hosting or frontend routing for activation and password reset links

Those concerns belong in follow-up implementation work.

## Rationale

### Keep providers behind Kartoush abstractions

Kartoush already established a shared email boundary.

Keeping provider code behind that boundary avoids coupling customer and auth
flows directly to Mailtrap, Brevo, or any specific SDK.

### Use different providers for inspection and real delivery

Mailtrap is well-suited for safe inspection and sandbox use.

Brevo is a reasonable initial provider for production transactional sending.

Using them for different purposes keeps local and test workflows safe without
forcing production behavior into every environment.

### Use a dedicated sending subdomain

A dedicated sending subdomain keeps transactional email isolated from root
domain concerns and makes DNS and deliverability management easier to reason
about.

### Do not overbuild mail hosting

Kartoush only needs one-way transactional sending right now.

That means DNS verification and deliverability records are necessary, but full
mail hosting and MX records are not.

### Keep secrets out of source control

Provider credentials are operational secrets, not normal application
configuration.

Keeping them in GitHub workflow secrets, AWS-managed runtime secret storage, or
developer-specific credentials is safer and more scalable than sharing them
through the repository.

## Consequences

### Positive

This decision:

- Gives `#276` a concrete provider and domain direction
- Keeps application code provider-agnostic
- Makes local and test email behavior easier to support safely
- Avoids unnecessary inbound mail setup
- Creates a clean path for future deliverability work

### Negative

This decision:

- Introduces environment-specific provider behavior that must be documented
- Requires DNS coordination before real sending can be used
- Defers the exact email template rendering approach
- Requires secret-management setup outside the repository

## Alternatives Considered

### Use one provider for every environment

This was rejected because local and test inspection needs are different from
early production delivery needs.

Using one real provider everywhere would blur those concerns and make local
testing less safe or more cumbersome.

### Send from the root domain

This was rejected because a dedicated sending subdomain is easier to isolate
and manage for transactional email.

### Build directly against a provider SDK in customer or auth flows

This was rejected because it would leak infrastructure concerns into
application logic and make future provider changes harder.

### Treat MX records as required from the start

This was rejected because Kartoush does not need inbound mail hosting for
one-way transactional sending in this phase.

## Follow-Up

- Task [`#274`](https://github.com/jbolous/kartoush/issues/274) established the shared email delivery boundary this ADR depends on
- Task [`#276`](https://github.com/jbolous/kartoush/issues/276) should implement the provider adapters and profile-based configuration using this direction
- The web account experience tasks under epic [`#279`](https://github.com/jbolous/kartoush/issues/279) will eventually consume the activation and password reset links generated through this strategy

## Related Decisions

- [ADR 0015: Headless and API Strategy](./0015-headless-and-api-strategy.md)
- [ADR 0018: Module Boundaries and Dependencies](./0018-module-boundaries-and-dependencies.md)
- [ADR 0024: Customer Activation Token Ownership and Lifecycle](./0024-customer-activation-token-ownership-and-lifecycle.md)
- [ADR 0028: Authentication and authorization strategy](./0028-authentication-and-authorization-strategy.md)
- [ADR 0030: External customer authentication strategy](./0030-external-customer-authentication-strategy.md)
