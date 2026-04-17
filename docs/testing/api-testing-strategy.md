# API Testing Strategy

## Purpose

Kartoush uses three complementary tools for API work:

- Swagger for API contract visibility and documentation
- Bruno for manual exploration and debugging
- REST Assured for automated API contract enforcement

These tools serve different purposes. They should be used together rather
than treated as interchangeable ways to test the same thing.

## Tool Roles

### Swagger

Swagger is the contract and documentation layer.

Use Swagger to:

- inspect available endpoints, request models, and response models
- confirm the intended contract for a new or changed endpoint
- verify that status codes and payload shapes are documented clearly
- communicate API behavior to other contributors and future clients

Swagger is not the source of truth for runtime correctness by itself. It
describes what the API is intended to do, but it does not prove that the
implementation actually behaves that way.

### Bruno

Bruno is the manual exploration and debugging tool.

Use Bruno to:

- exercise endpoints quickly during development
- inspect real responses while iterating on controller or service behavior
- reproduce scenarios such as validation failures or lifecycle conflicts
- debug request payloads, headers, and response bodies interactively

Bruno is useful when shaping behavior and learning how an endpoint behaves in
practice. It is intentionally manual and exploratory.

Bruno should not be the only validation for a finished API change. If a
behavior matters enough to keep working, it should usually become an automated
test.

### REST Assured

REST Assured is the automated API contract enforcement tool.

Use REST Assured to:

- verify API behavior through real HTTP requests
- assert status codes, headers, and response payloads together
- catch regressions where the endpoint is reachable but the contract is wrong
- protect important API paths in CI

REST Assured is the right tool when the question is:

"Does the application actually serve the HTTP contract we expect?"

This is especially important for problems that status-code-only assertions can
miss, such as:

- missing response fields
- incorrect field names or types
- wrong error payload structure
- incorrect headers or location values

## How The Tools Work Together

The intended progression is:

1. define or refine the endpoint contract in Swagger
2. explore and debug the endpoint manually with Bruno
3. lock in the important behavior with REST Assured tests
4. run those tests in CI so regressions block merge

This progression matters because each tool answers a different question:

- Swagger: What is the intended contract?
- Bruno: What is the endpoint doing right now while I develop it?
- REST Assured: Does the implementation continue to honor the contract?

## Recommended Workflow For A New Endpoint

### 1. Start with the contract

Before treating an endpoint as complete, make sure the contract is explicit.

That usually means:

- request model is defined clearly
- response model is defined clearly
- relevant status codes are documented
- error behavior is described when applicable

Swagger should make the endpoint understandable without reading controller
code.

### 2. Explore manually

Use Bruno while building the endpoint to answer questions such as:

- is request binding working as expected?
- do validation errors return the expected shape?
- does the endpoint return the right payload for the happy path?
- are edge cases behaving as expected?

Bruno is the fastest feedback loop while you are still discovering and
adjusting behavior.

### 3. Convert important scenarios into REST Assured tests

Once the behavior is understood, add REST Assured coverage for the scenarios
that should not regress.

At a minimum, prioritize:

- the happy path
- important validation failures
- meaningful lifecycle or conflict scenarios
- contract details that clients depend on

Assertions should validate both status and payload. A `200 OK` is not enough
if the response body is incomplete or malformed.

### 4. Let CI enforce it

After the REST Assured test exists, it becomes part of the automated safety
net. CI should be able to fail the pull request when the runtime API contract
changes unexpectedly.

## Choosing The Right Tool

Use Swagger when:

- you need to understand or communicate the intended API contract
- you are reviewing documentation quality
- you are checking whether the published contract matches the code

Use Bruno when:

- you are exploring behavior manually
- you are debugging an endpoint interactively
- you need a quick feedback loop before automating a scenario

Use REST Assured when:

- the behavior should be protected from regression
- you need end-to-end HTTP assertions
- the response contract matters to callers
- the scenario belongs in CI

## Testing Philosophy

Kartoush should validate API contracts beyond status codes.

A passing API test should usually tell us more than "the endpoint responded."
It should help answer:

- did the endpoint return the expected fields?
- did it use the expected status code?
- did validation or error responses follow the expected structure?
- did the contract remain consistent with what callers rely on?

This philosophy is why Swagger, Bruno, and REST Assured each matter:

- Swagger defines the contract
- Bruno helps discover and debug the behavior
- REST Assured enforces the contract automatically

## Current Scope

This guide defines how the tools should be used together.

It does not:

- replace the broader testing guidance in ADR 0023
- require every existing API test to be rewritten immediately
- define Bruno collection structure in detail
- define every CI workflow detail

Those concerns are handled by the related API testing and CI tasks.
