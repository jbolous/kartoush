\set ON_ERROR_STOP on
\getenv app_password APP_DB_PASSWORD
BEGIN;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kartoush_app') THEN
    RAISE EXCEPTION 'Application role already exists; inspect it before changing credentials';
  END IF;
END
$$;
CREATE ROLE kartoush_app LOGIN PASSWORD :'app_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT;
REVOKE ALL ON DATABASE kartoush FROM PUBLIC;
GRANT CONNECT, CREATE, TEMPORARY ON DATABASE kartoush TO kartoush_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO kartoush_app;
COMMIT;
