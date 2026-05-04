# Domain Rules Memory

## Customer lifecycle

- Customer statuses:
  - `PENDING`
  - `ACTIVE`
  - `INACTIVE`
  - `DELETED`
- Valid transitions:
  - `PENDING -> ACTIVE`
  - `PENDING -> INACTIVE`
  - `PENDING -> DELETED`
  - `ACTIVE -> INACTIVE`
  - `ACTIVE -> DELETED`
  - `INACTIVE -> ACTIVE`
  - `INACTIVE -> DELETED`
- `DELETED` is terminal

## Registration

- `POST /api/customers` is the registration entry point
- Successful registration creates a `PENDING` customer
- Registration requires:
  - First name
  - Last name
  - Valid email
  - Terms acceptance set to true
  - Terms version matching the current active Terms record
- Registration persists Terms acceptance audit data
- Registration issues an activation token and requests activation email delivery

## Activation

- Activation uses `POST /api/customers/{customerId}/activation`
- Customer must exist and be `PENDING`
- Activation token must belong to the customer
- Token must exist, be unexpired, and not already be consumed
- Successful activation moves the customer to `ACTIVE`
- Successful activation consumes the activation token
- Successful activation returns a one-time password setup token

## Initial password setup

- Initial password setup uses `POST /api/customers/{customerId}/initial-password`
- Customer must exist and be `ACTIVE`
- Setup token must belong to the customer
- Setup token must exist, be unexpired, and not already be consumed
- Password confirmation must match
- Current default password policy requires:
  - At least 12 characters
  - One uppercase letter
  - One lowercase letter
  - One digit
  - One special character
- Successful setup consumes the setup token

## Sign-in

- Sign-in uses `POST /api/auth/sign-in`
- Customer must exist
- Customer must be `ACTIVE`
- Customer must already have a password
- Password must match the stored hash
- Successful sign-in returns an opaque bearer token
- Failed sign-in attempts do not create auth-session records

## Password reset

- Reset request uses `POST /api/auth/password-reset`
- Request always returns `204 No Content` for a well-formed email
- Reset tokens are issued only for `ACTIVE` customers with an existing password
- Reset confirmation uses `POST /api/auth/password-reset/confirm`
- Successful reset consumes the reset token
- A customer cannot reset to the password currently on the account
- Successful password reset revokes existing auth sessions

## Resend activation

- Resend uses `POST /api/customers/{customerId}/activation/resend`
- Resend is allowed only for `PENDING` customers
- Issuing a new activation token consumes the previous active token first

## Notification and email

- Current customer-facing email flows:
  - Activation
  - Password reset
- Provider direction:
  - `mailtrap` for local or dev inspection
  - `brevo` for production transactional sending
- Sending subdomain direction is `notify.kartoush.com`
