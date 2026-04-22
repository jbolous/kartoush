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

When Bruno is available for the domain, start from the committed collection
instead of building one-off requests from scratch. This keeps manual API
exploration aligned with the repository and makes it easier to turn a manual
scenario into a repeatable automated test later.

For the current customer API collection:

1. open `bruno/` in Bruno
2. select the `local` environment
3. run the relevant request sequence for the behavior you are exploring
4. inspect the response payload, headers, and status code
5. capture any behavior that should become part of the automated contract

Bruno is where you should answer questions like:

- what does the error payload actually look like right now?
- which fields are stable enough that clients will depend on them?
- which scenario is important enough to automate instead of checking manually?

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

The conversion from Bruno to REST Assured should be direct:

1. keep the same request shape you validated manually
2. preserve the same success or failure scenario
3. assert the parts of the response that matter to callers
4. avoid over-asserting implementation noise that is not part of the contract

If a Bruno request helped uncover the correct behavior, the REST Assured test
should explain that behavior clearly enough that a future reader does not need
to reconstruct the manual debugging session.

### 4. Let CI enforce it

After the REST Assured test exists, it becomes part of the automated safety
net. CI should be able to fail the pull request when the runtime API contract
changes unexpectedly.

CI is the final gate, not the discovery tool. The intended progression is:

- Swagger to define the contract
- Bruno to explore and debug it
- REST Assured to preserve it
- CI to enforce it on every branch and pull request

## Debugging Workflow With DevTools

Spring Boot DevTools is useful while iterating on API behavior because it
keeps the local feedback loop short without changing the testing strategy.

Use DevTools during API work when you need to:

- adjust controller or validation behavior and retry quickly
- inspect logs after manual Bruno requests
- reproduce a failure path before deciding what should be automated

Recommended local debugging loop:

1. run the application locally with DevTools enabled
2. make a targeted change to the controller, validation, or facade behavior
3. let the application restart
4. re-run the relevant Bruno request
5. inspect logs and response payloads
6. once the behavior is correct, add or update the REST Assured test
7. run the relevant automated tests before pushing

DevTools helps shorten the manual iteration cycle, but it should not become a
substitute for writing the automated test that locks the behavior in.

## Step-By-Step Example

The customer create endpoint is a good example of the intended workflow.

### Example: add or refine customer creation behavior

1. confirm the contract in Swagger

   Check that `POST /api/customers` documents:

   - the `CreateCustomerRequest` fields
   - the `201 Created` happy path
   - the `400` validation failure path
   - the `409` conflict path

2. explore the endpoint manually with Bruno

   Use the customer requests in `bruno/customers/`:

   - `02-create-customer.bru` for the happy path
   - `10-create-customer-invalid-email.bru` for validation behavior
   - `11-create-customer-duplicate-pending.bru` for conflict behavior

   While exploring, verify:

   - the `Location` header is returned on create
   - the response body contains the expected customer fields
   - validation failures return the expected ProblemDetails structure

3. convert the important scenarios into REST Assured coverage

   The current customer API tests show the intended shape:

   - `CustomerCreationRestAssuredIntegrationTest` covers create and validation
   - `CustomerDuplicateEmailRestAssuredIntegrationTest` covers a meaningful
     lifecycle conflict scenario

   These tests assert more than status codes. They validate response fields,
   headers, and error payload details that clients depend on.

4. let CI enforce the contract

   Once the REST Assured coverage is in place, CI runs the app integration
   test groups by stable API package boundaries and fails the pull request if
   the contract changes unexpectedly.

   App integration CI should not require a workflow edit for each new test
   class. Grouping by stable package boundaries keeps the workflow
   maintainable while still allowing parallel execution across API domains.

   Today that means:

   - `com.kartoush.api.customer.*`
   - `com.kartoush.api.terms.*`

   New integration tests should be added under the correct API package so CI
   picks them up automatically.

This example is the intended pattern for future endpoints:

- use Bruno to discover behavior quickly
- use REST Assured to preserve the important scenarios
- let CI make those expectations non-optional

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
