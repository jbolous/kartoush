 # Review Workflow

Before finishing:

1. Re-read the issue or task and check that the scope stayed tight
2. Check module ownership again
3. Look for duplicated code or duplicated abstractions
4. Check names for clarity
5. Verify exceptions, logs, and config names are understandable
6. Confirm tests cover the changed behavior, not just happy paths
7. Confirm docs still match runtime behavior
8. Confirm bullets and checklist items use sentence case where the repo expects prose bullets
9. Confirm replaced code paths were removed instead of left in parallel
10. Confirm any new test slice is either covered by CI already or called out explicitly
11. Remove unnecessary imports left behind by the change

Review questions:

- Whether this introduced framework leakage across a module boundary
- Whether this added a provider-specific concept where a repo-owned interface should exist
- Whether this changed a documented rule without updating docs
- Whether this added a new test slice that CI does not run
- Whether this changed request or response contracts that need MVC or OpenAPI coverage
- Whether this “cleanup” is correcting a real problem or undoing an intentional tradeoff
