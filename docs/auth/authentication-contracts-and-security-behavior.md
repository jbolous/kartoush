# Authentication Contracts and Security Behavior

## Purpose

This document describes the current customer-facing authentication surface in
Kartoush and the current security behavior.

The auth ADRs explain why the architecture is shaped this way. This document
explains what the current API actually does.

## Current Auth Surface

Kartoush currently exposes the following customer-facing auth flow:

1. `POST /api/customers`
   Creates a `PENDING` customer and issues an activation token through the
   configured email delivery service
2. `POST /api/customers/{customerId}/activation`
   Activates the customer and returns a one-time password setup token
3. `POST /api/customers/{customerId}/initial-password`
   Consumes the setup token and establishes the first usable customer password
4. `POST /api/auth/password-reset`
   Accepts a customer email address and requests delivery of a one-time
   password reset token when the account is eligible
5. `POST /api/auth/password-reset/confirm`
   Consumes the password reset token and establishes a replacement customer
   password
6. `POST /api/auth/sign-in`
   Authenticates the customer with email and password and returns an opaque
   bearer token

## Current Security Behavior

### Public vs authenticated routes

The registration, activation, initial password setup, password reset, and
sign-in routes are currently public entry points by design.

Kartoush now issues opaque bearer tokens, but it does not yet use those
tokens to protect customer or internal routes. Runtime token validation and
authenticated route enforcement remain follow-up work under the API security
track.

### Customer eligibility rules

Current customer sign-in requires all of the following:

- The customer must exist
- The customer must be in `ACTIVE` status
- The customer must already have an established password
- The supplied password must match the stored password hash

If any of those checks fail after request validation, the runtime API returns
the same `401 Unauthorized` auth failure response.

### Session behavior

Successful sign-in creates a server-side auth session and returns an opaque
`Bearer` access token to the client.

Failed sign-in attempts do not create auth-session records. That is part of
the tested runtime contract, not just an implementation detail.

### Password reset behavior

The current password reset flow follows these rules:

- Password reset request accepts a well-formed email address and always returns
  `204 No Content`
- Kartoush does not reveal through the request endpoint whether the email is
  known or eligible for reset
- Reset tokens are issued only for `ACTIVE` customers who already have an
  established password
- Successful password reset consumes the reset token so it cannot be reused
- A customer cannot reset to the password currently on the account
- Kartoush does not currently enforce a deeper password history rule beyond
  blocking reuse of the immediately previous password

### Token behavior

Kartoush currently uses three token types with distinct responsibilities:

- Activation tokens
  Used to move a customer from `PENDING` to `ACTIVE`
- Password setup tokens
  One-time tokens used to establish the first password after activation
- Password reset tokens
  One-time tokens used to replace an existing customer password
- Access tokens
  Opaque bearer tokens returned by sign-in and backed by server-side auth
  session state

Activation tokens, password setup tokens, and password reset tokens are
one-time use. Successful use consumes the token so it cannot be reused.

## Error Contract Notes

The current auth-related API contract follows these rules:

- Validation failures return `400 Bad Request` with the shared validation
  problem schema
- Authentication failures return `401 Unauthorized` with
  `INVALID_CUSTOMER_CREDENTIALS`
- Password setup lifecycle and duplicate-password failures return
  `409 Conflict`
- Password reset token expiry, consumption, invalid reset state, and password
  reuse failures return `409 Conflict`
- Missing activation or password setup tokens return `404 Not Found`
- Missing password reset tokens return `404 Not Found`

These contracts are reflected in OpenAPI and verified by MVC and integration
tests.

## Current Automated Coverage

The current auth behavior is covered by:

- MVC tests for auth request and response shape
- OpenAPI tests for auth endpoint documentation
- Integration tests for activation, password setup, and sign-in flows
- Integration tests for password reset flows
- Auth module unit and repository tests for password, token, and auth-session behavior

Representative tests include:

- `CustomerAuthenticationControllerWebMvcTest`
- `CustomerAuthenticationOpenApiWebMvcTest`
- `CustomerActivationAndPasswordSetupIntegrationTest`
- `CustomerPasswordResetFlowIntegrationTest`
- `CustomerSignInFlowIntegrationTest`
- `DefaultCustomerSignInFacadeTest`
- `DefaultCustomerPasswordFacadeResetTest`
- `DefaultCustomerAuthSessionServiceTest`
- `CustomerAuthSessionRepositoryTest`
- `PasswordResetTokenRepositoryTest`

## What Is Not Covered Yet

The following are intentionally not implemented yet:

- External authentication providers such as Google or Facebook
- Password history enforcement beyond the current immediate previous-password rule
- Runtime token validation for protected customer routes
- Runtime authorization for internal management APIs

Those concerns are tracked separately and should not be inferred from the
current bearer token issuance alone.
