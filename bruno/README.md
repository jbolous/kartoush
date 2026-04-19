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
- `customers/`
  Customer API requests, including both happy-path and failure scenarios

## How To Use

1. Open the `bruno/` directory as a Bruno collection.
2. Select the `local` environment.
3. Run `customers/02-create-customer.bru` first to create a customer and
   capture `customerId` into the environment.
4. Use the remaining customer requests to inspect success and error
   behavior.

The create request also refreshes the `email` environment variable with a
timestamp-based value so repeated runs stay usable without manual cleanup.

## Activation Flow Caveat

The activation endpoints are included in this collection, but the full
manual happy path still depends on a development-only fallback.

Kartoush does not expose the raw activation token through the API. For
local development, the current `NoOpActivationEmailService` logs the raw
token so it can be copied into the Bruno environment. That means:

- `customers/06-resend-activation-token.bru` is usable for pending customers
- `customers/07-activate-customer.bru` requires a valid token to be copied
  from the local application logs into the environment
- the invalid-token and blank-token activation requests are the current
  out-of-the-box error-path requests for activation

Once a real email delivery implementation exists, this collection can be
updated to use the delivered token instead of relying on the no-op log
output.
