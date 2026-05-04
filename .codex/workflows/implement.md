# Implement Workflow

When changing code:

1. Keep the change inside the owning module unless there is a real boundary reason not to
2. Prefer explicit code over clever indirection
3. Do not leak provider, framework, or persistence details across module boundaries
4. Keep logs useful but never log raw auth or email tokens
5. Add or update the narrowest tests that prove the behavior
6. Update docs or ADRs when the change alters a documented rule or architecture decision

Implementation reminders for Kartoush:

- Customer lifecycle stays in `customer`
- Credential and session behavior stays in `auth`
- Email delivery stays in `notification`
- Shared value objects belong in `platform-types`
- Request-shape validation belongs near HTTP models, business validation belongs in facades or validators

Do not:

- Move code across modules just because it is convenient
- Make provider-specific code reachable from controllers, facades, or domain types
- Treat opaque access tokens as self-describing JWTs
