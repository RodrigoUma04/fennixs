#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<EOSQL
SELECT 'CREATE DATABASE fennixs_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fennixs_auth')
\gexec

SELECT 'CREATE DATABASE fennixs_core'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fennixs_core')
\gexec
EOSQL
