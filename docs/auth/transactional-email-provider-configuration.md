# Transactional Email Provider Configuration

## Purpose

This document explains how Kartoush chooses and configures transactional email
delivery by environment.

For the architectural decision behind this setup, see:

- `docs/architecture/decisions/0031-transactional-email-provider-and-sending-domain-strategy.md`

## Current Delivery Modes

Kartoush supports the following delivery modes through the shared internal
email boundary:

- `noop`
  - Does not send email
  - Logs that delivery was requested
  - Safe default when delivery is disabled
- `mailtrap`
  - Sends email through the Mailtrap email sandbox API
  - Intended for local and development inspection
- `brevo`
  - Sends email through the Brevo transactional email API
  - Intended for production transactional delivery

## Spring Profile Direction

The current profile direction is:

- `dev`
  - Defaults to Mailtrap as the selected provider
  - Delivery is disabled unless explicitly enabled through environment
    configuration
- `test`
  - Defaults to no-op delivery
- `prod`
  - Defaults to Brevo as the selected provider
  - Delivery is enabled by default and expects real provider configuration

## Shared Email Configuration

These values are used regardless of the provider:

- `KARTOUSH_EMAIL_SENDER_NAME`
- `KARTOUSH_EMAIL_SENDER_ADDRESS`
- `KARTOUSH_EMAIL_ACTIVATION_BASE_URL`
- `KARTOUSH_EMAIL_PASSWORD_RESET_BASE_URL`
- `KARTOUSH_EMAIL_DELIVERY_PROVIDER`
- `KARTOUSH_EMAIL_DELIVERY_ENABLED`

Examples:

```bash
export KARTOUSH_EMAIL_SENDER_NAME="Kartoush"
export KARTOUSH_EMAIL_SENDER_ADDRESS="no-reply@notify.kartoush.com"
export KARTOUSH_EMAIL_ACTIVATION_BASE_URL="http://localhost:3000/activate"
export KARTOUSH_EMAIL_PASSWORD_RESET_BASE_URL="http://localhost:3000/reset-password"
```

## Local Development With Mailtrap

To inspect local email through Mailtrap, run these `export` commands in the
same terminal session before starting the application:

```bash
export SPRING_PROFILES_ACTIVE=dev
export KARTOUSH_EMAIL_DELIVERY_PROVIDER=mailtrap
export KARTOUSH_EMAIL_DELIVERY_ENABLED=true
export KARTOUSH_EMAIL_MAILTRAP_API_BASE_URL="https://sandbox.api.mailtrap.io/api/send"
export KARTOUSH_EMAIL_MAILTRAP_API_TOKEN="your-mailtrap-api-token"
export KARTOUSH_EMAIL_MAILTRAP_INBOX_ID=12345
```

If delivery should remain off in local development, leave
`KARTOUSH_EMAIL_DELIVERY_ENABLED=false` or omit it entirely.

### Local Quick Start

Use this flow when you want to verify local customer emails in Mailtrap:

1. Start PostgreSQL locally and make sure Kartoush can connect to it
2. Export the `dev` Mailtrap environment variables shown above
3. Export the shared customer email settings if you want non-default values
4. Start the application

```bash
./gradlew :app:bootRun
```

5. Trigger an email-producing flow, for example:
   - Create a customer through `POST /api/customers` to send an activation email
   - Request a password reset through `POST /api/auth/password-reset`
6. Review the delivered message in the configured Mailtrap inbox

Use the `dev` profile for real local Mailtrap delivery checks. The `test`
profile stays on no-op delivery and is intended for automated tests rather
than provider-backed local inspection.

## Test Behavior

The `test` profile defaults to:

- `KARTOUSH_EMAIL_DELIVERY_PROVIDER=noop`
- `KARTOUSH_EMAIL_DELIVERY_ENABLED=false`

Most automated tests should continue using no-op or mocked delivery rather
than attempting real provider interaction.

## Production Configuration With Brevo

Production delivery expects the `prod` profile and Brevo configuration:

```bash
export SPRING_PROFILES_ACTIVE=prod
export KARTOUSH_EMAIL_DELIVERY_PROVIDER=brevo
export KARTOUSH_EMAIL_DELIVERY_ENABLED=true
export KARTOUSH_EMAIL_BREVO_API_BASE_URL="https://api.brevo.com/v3"
export KARTOUSH_EMAIL_BREVO_API_KEY="your-brevo-api-key"
export KARTOUSH_EMAIL_SENDER_NAME="Kartoush"
export KARTOUSH_EMAIL_SENDER_ADDRESS="no-reply@notify.kartoush.com"
export KARTOUSH_EMAIL_ACTIVATION_BASE_URL="https://kartoush.com/activate"
export KARTOUSH_EMAIL_PASSWORD_RESET_BASE_URL="https://kartoush.com/reset-password"
```

The Brevo API key must not be committed. Runtime production secrets should come
from AWS Secrets Manager or AWS Systems Manager Parameter Store.

## CI And Workflow Secrets

GitHub secrets are intended for CI and workflow automation only.

They are appropriate when:

- A workflow needs provider credentials
- A CI environment must exercise provider-backed setup

They are not the source of truth for runtime production secrets.

## Provider Setup Expectations

Kartoush currently assumes a dedicated sending subdomain:

- `notify.kartoush.com`

Before real delivery is used, the sending domain should be configured with:

- Provider verification records
- SPF
- DKIM
- DMARC

MX records are not required for this one-way transactional sending setup.

## Failure Handling

Provider failures are treated as infrastructure failures.

Current behavior:

- Delivery failures are logged with provider, email type, and recipient context
- Raw activation, password setup, and password reset tokens must not be logged
- If provider delivery fails, the email delivery call throws an
  `EmailDeliveryException`
- Customer-facing flows may therefore fail after the business-side token work
  has already completed, which matches the current registration and reset flow
  design

When available, provider message IDs should be logged instead of secret values
or raw tokens.
