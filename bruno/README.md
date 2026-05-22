# Bruno Collection

This directory contains a Bruno collection for manually exploring the
Kartoush HTTP API during local development.

## Prerequisites

- Bruno installed locally
- Kartoush running on `http://localhost:8080`

## Collection Layout

- `bruno.json`
  Bruno collection metadata
- `collection.bru`
  Collection-level configuration
- `environments/local.bru`
  Local environment variables for the running application
- `auth/`
  Customer authentication requests, including sign-in and password reset flows
- `customers/`
  Customer API requests, including both happy-path and failure scenarios
- `internal-customers/`
  Internal customer management requests for administrative operations
- `scenarios/customer-lifecycle/`
  End-to-end customer lifecycle requests arranged in execution order
- `internal-terms-of-service/`
  Internal Terms of Service management requests for draft and lifecycle operations
- `terms-of-service/`
  Read-only Terms of Service metadata requests

## How To Use

1. Open the `bruno/` directory as a Bruno collection.
2. Select the `local` environment.
3. For a full walkthrough, run the requests under `scenarios/customer-lifecycle/` in order.
4. Use the `auth/` and `customers/` folders when you want endpoint-oriented exploration instead of the full scenario flow.
5. Use the `internal-terms-of-service/` requests to create and manage draft Terms versions through the internal lifecycle endpoints.
6. Use the `terms-of-service/` requests to inspect the current supported Terms version, retrieve a published Terms document by version, and inspect the not-found response for an unpublished or missing version.

The create request also refreshes the `email` environment variable with a timestamp-based value so repeated runs stay usable without manual cleanup.

## Activation Flow Caveat

The activation endpoints are included in this collection, but the full manual happy path still depends on a development-only fallback.

Kartoush does not expose the raw activation token through the API. For local development, the raw token still needs to be copied into the Bruno environment from the delivery mechanism you are using. That means:

- `customers/06-resend-activation-token.bru` is usable for pending customers
- `customers/07-activate-customer.bru` requires a valid token to be copied from delivered email, Mailtrap, or local development output into the environment
- the invalid-token and blank-token activation requests are the current out-of-the-box error-path requests for activation

Once a real email delivery implementation exists, this collection can be updated further to capture the token more directly.

## Password Reset Caveat

The password reset request and confirm endpoints are included in this collection, but the happy-path confirm request still depends on a delivered reset token.

For local development:

- run `auth/03-request-password-reset.bru`
- copy the reset token from the delivered email, Mailtrap, or local development output into `resetToken` in the Bruno environment
- run `auth/05-confirm-password-reset.bru`

The invalid-token confirm request is included as the current out-of-the-box error-path request for password reset confirmation.

The scenario flow does not auto-capture reset tokens either. You still need to copy the delivered reset token into `resetToken` before running
`scenarios/customer-lifecycle/08-confirm-password-reset.bru`.

## Internal Route Auth

`customers/01-list-customers.bru` now targets the internal customer-listing route and uses the local development admin credentials from the `dev` profile.

The `internal-terms-of-service/` requests use the same local development admin credentials.
