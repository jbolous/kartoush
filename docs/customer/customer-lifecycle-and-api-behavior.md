# Customer Lifecycle and API Behavior

## Overview

The customer module defines the lifecycle, validation rules, and API behavior for customer management within the system.

This document describes the current implemented behavior, including lifecycle transitions, API expectations, and error handling conventions. It is intended to provide a clear reference without requiring readers to inspect the implementation.

---

## Customer Lifecycle

### States

Customers exist in one of the following states:

- PENDING
- ACTIVE
- INACTIVE
- DELETED

---

### State Definitions

Each customer lifecycle state represents a distinct stage in the customer's relationship with the system.

#### PENDING

A customer has been created but has not yet completed activation.

This state typically represents a newly registered customer who has not verified their account or completed an activation step. Pending customers are not considered fully active in the system.

Pending customers are not returned in standard customer queries and are not eligible for normal operations until activated.

---

#### ACTIVE

A customer is fully active and can interact with the system as expected.

This is the normal operating state for a customer. Active customers are returned in standard queries and are eligible for all supported operations.

---

#### INACTIVE

A customer is temporarily disabled but not deleted.

This state represents a customer who previously had access but is no longer active due to a system or administrative action. The customer may be reactivated in the future.

Inactive customers are not considered active in the system but retain their data and identity.

---

#### DELETED

A customer has been soft deleted.

The customer record remains in the system for historical and auditing purposes, but the customer is no longer considered active. The original email is mutated to allow reuse, and the customer cannot transition to any other state.

---

### Valid Transitions

The following lifecycle transitions are supported:

- PENDING -> ACTIVE
- PENDING -> INACTIVE
- PENDING -> DELETED
- ACTIVE -> INACTIVE
- ACTIVE -> DELETED
- INACTIVE -> ACTIVE
- INACTIVE -> DELETED

---

### Invalid Transitions

The following transitions are not allowed:

- DELETED -> any state
- Reactivation from any state other than INACTIVE
- Invalid transitions during specific operations, such as attempting to reactivate a PENDING customer

Invalid transitions result in domain exceptions.

---

### Activation vs Reactivation

The system distinguishes between activation and reactivation.

Activation moves a customer from PENDING to ACTIVE and represents initial account activation.

Reactivation moves a customer from INACTIVE to ACTIVE and is used when restoring access for an existing customer.

Reactivation is not allowed for PENDING or DELETED customers.

---

### Delete Behavior (Soft Delete)

Customer deletion is implemented as a soft delete.

The customer status is set to DELETED, and the record remains in persistence. The original email is mutated to allow reuse of the original email address while preserving historical data.

---

## API Behavior

### Base Path

`/api/customers`

---

### Endpoints

### Registration requirements

Kartoush currently treats `POST /api/customers` as the registration entry
point for new customers.

A registration request is valid only when all of the following requirements are
satisfied:

- first name is present and not blank
- last name is present and not blank
- email is present and valid
- phone number is optional, but if provided it must be valid
- Terms of Service acceptance must be explicitly true
- the submitted Terms version must match the current supported Terms version

Successful registration always creates a customer in `PENDING` status. The
public API does not allow callers to choose an alternative initial lifecycle
state.

If registration validation fails:

- the request is rejected with a ProblemDetails response
- no customer record is created
- no activation token is issued

These requirements define the current registration contract for onboarding a
new customer into the `PENDING` lifecycle state.

---

#### Get all customers

`GET /api/customers`

Returns a list of customers. Only active customers are returned.

---

#### Get customer by ID

`GET /api/customers/{customerId}`

Returns a single customer. Returns 404 if the customer does not exist.

---

#### Create customer

`POST /api/customers`

Creates a new customer and returns 201 Created with the created customer.

New customers are created in `PENDING` status. Customer creation also issues an
activation token and requests delivery through the configured activation email
service.

Validation rules:

- First name and last name are required
- Email must be valid and within defined length constraints
- Phone number, if provided, must be valid

Error scenarios:

- 400 Bad Request for validation failures
- 409 Conflict if the customer already exists or is in a conflicting state

---

#### Activate customer

`POST /api/customers/{customerId}/activation`

Activates a pending customer using a valid activation token and returns the
updated customer.

Activation behavior:

- the customer must exist
- the customer must currently be in `PENDING` status
- the activation token must belong to the target customer
- the activation token must exist, be unexpired, and not already be consumed
- a successful activation moves the customer to `ACTIVE`
- a successful activation consumes the activation token so it cannot be reused

Error scenarios:

- 400 Bad Request for request validation failures, such as a blank token
- 404 Not Found if the customer or activation token does not exist
- 409 Conflict if the token is expired, already consumed, or the customer
  cannot be activated from the current lifecycle state

---

#### Update customer

`PUT /api/customers/{customerId}`

Updates customer profile fields such as first name, last name, and phone number.

The operation does not update the email address and does not change the customer lifecycle state. The request is intended to be idempotent.

Error scenarios:

- 400 Bad Request for validation failures
- 404 Not Found if the customer does not exist

---

#### Resend activation token

`POST /api/customers/{customerId}/activation/resend`

Issues a new activation token for a pending customer and returns 204 No
Content.

Resend behavior:

- resend is allowed only for customers in `PENDING` status
- issuing a new activation token consumes any existing active token for that
  customer before creating the replacement
- the new token becomes the only valid activation token for that customer
- token delivery is delegated to the configured activation email service

Error scenarios:

- 404 Not Found if the customer does not exist
- 409 Conflict if the customer is not in a state that allows resend

---

#### Delete customer

`DELETE /api/customers/{customerId}`

Performs a soft delete and returns 204 No Content.

The customer status is set to DELETED, and the record is not physically removed.

---

## Error Handling

The API uses a consistent error response structure based on ProblemDetail.

### Common Status Codes

- 400 Bad Request for validation failures
- 404 Not Found when the customer does not exist
- 409 Conflict for business rule violations
- 500 Internal Server Error for unexpected failures

### Error Response Characteristics

Error responses include:

- Status
- Title
- Detail
- Type (error code URI)
- Instance (request path)

Additional properties may include:

- ErrorCode
- Timestamp

Validation errors are standardized across both custom validation logic and framework-level validation.

---

## Not Yet Exposed

The following capabilities are not currently exposed via the API:

- Reactivation endpoint
- Administrative or support-oriented activation recovery flows
- Public token introspection or token management APIs

These will be introduced in a future iteration.

---

## Notes

This document reflects the current implemented behavior and should be updated as lifecycle rules or API behavior evolve.
