# Customer

Customer domain module.

## Ownership

This module owns:
- Customer domain model
- Customer persistence model and migrations
- Published interfaces for cross-module access (facades / ports)

## Constraints

- Other modules must not access internal packages or persistence directly
- Cross-module access must go through published contracts
