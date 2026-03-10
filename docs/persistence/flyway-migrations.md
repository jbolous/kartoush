# Flyway migrations

## Convention

Each domain module owns its own Flyway migrations.

Migrations live at:

\<module>/src/main/resources/db/migration/

Flyway scans the application classpath, so module-owned migrations are discovered and applied when the module is on the runtime classpath.

## Notes

- Migrations are treated as stable contracts and should be append-only.
- Never modify an existing migration that has been applied. Add a new migration instead.
