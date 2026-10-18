-- Creates the application role and an empty database for local development.
-- Run as a PostgreSQL superuser; the app's tables are then created by Flyway on first start.
--
--   psql -h localhost -p <port> -U postgres -v app_password=<password> -f backend/scripts/create-local-db.sql
--
-- Safe to re-run: existing role/database are kept and the role's password is reset.

\set ON_ERROR_STOP on

SELECT 'CREATE ROLE newlife_app LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'newlife_app') \gexec

ALTER ROLE newlife_app WITH LOGIN PASSWORD :'app_password';

SELECT 'CREATE DATABASE newlife_hospital OWNER newlife_app ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'newlife_hospital') \gexec

-- PostgreSQL 15+ no longer lets every role create objects in "public"; the owner role needs it explicitly.
\connect newlife_hospital
ALTER SCHEMA public OWNER TO newlife_app;

\echo 'newlife_hospital is ready for newlife_app.'
