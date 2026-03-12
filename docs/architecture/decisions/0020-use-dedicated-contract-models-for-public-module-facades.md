# Use dedicated contract models for public module facades

Date: 2026-03-10

## Status

Proposed

## Context

Kartoush is being built as a modular monolith with explicit module boundaries.
As module facades are introduced, starting with the Customer module, we need a consistent way to define the public contract exposed by each module.

There are several options:

1. Expose domain objects directly from module facades
2. Expose dedicated contract models with generic names such as `*Dto`
3. Expose dedicated contract models with role-based names such as `*Request` and `*View`

This decision affects:

- how modules publish their public APIs
- whether internal domain and persistence models remain encapsulated
- naming consistency across current and future modules
- how consuming modules and the `app` module interact with module boundaries

The Customer facade is the first concrete case where this decision is needed.

## Decision

Kartoush module facades will expose dedicated contract models rather than domain or persistence types.

The following rules apply:

1. Domain models remain internal to their owning module
2. Persistence entities, embeddables, repositories, and persistence mappers remain internal to their owning module
3. Public module facades publish explicit contract models for inputs and outputs
4. Input contract models should use role-based names such as `*Request`
5. Output contract models should use role-based names such as `*View`
6. Generic `*Dto` naming is avoided for public module facade contracts unless there is a strong reason to do otherwise

Example:

```java
public interface CustomerFacade {

    CustomerView createCustomer(CreateCustomerRequest request);

    Optional<CustomerView> getCustomerById(CustomerId customerId);
}
