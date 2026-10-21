#!/bin/sh
set -eu

: "${POSTGRES_USER:?init-db.sh needs POSTGRES_USER}"
: "${POSTGRES_DB:?init-db.sh needs POSTGRES_DB}"

: "${KEYCLOAK_DB_NAME:?init-db.sh needs KEYCLOAK_DB_NAME}"
: "${KEYCLOAK_DB_USER:?init-db.sh needs KEYCLOAK_DB_USER}"
: "${KEYCLOAK_DB_PASSWORD:?init-db.sh needs KEYCLOAK_DB_PASSWORD}"

: "${CORE_DB_NAME:?init-db.sh needs CORE_DB_NAME}"
: "${CORE_MIGRATOR_USER:?init-db.sh needs CORE_MIGRATOR_USER}"
: "${CORE_MIGRATOR_PASSWORD:?init-db.sh needs CORE_MIGRATOR_PASSWORD}"
: "${CORE_APP_USER:?init-db.sh needs CORE_APP_USER}"
: "${CORE_APP_PASSWORD:?init-db.sh needs CORE_APP_PASSWORD}"

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v role="$KEYCLOAK_DB_USER" \
  -v pw="$KEYCLOAK_DB_PASSWORD" \
  -v db="$KEYCLOAK_DB_NAME" <<'EOSQL'
CREATE ROLE :"role" LOGIN PASSWORD :'pw';
CREATE DATABASE :"db" OWNER :"role";
REVOKE CONNECT ON DATABASE :"db" FROM PUBLIC;
EOSQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v migrator_role="$CORE_MIGRATOR_USER" \
  -v migrator_pw="$CORE_MIGRATOR_PASSWORD" \
  -v app_role="$CORE_APP_USER" \
  -v app_pw="$CORE_APP_PASSWORD" \
  -v db="$CORE_DB_NAME" <<'EOSQL'
CREATE ROLE :"migrator_role" LOGIN PASSWORD :'migrator_pw';
CREATE ROLE :"app_role" LOGIN PASSWORD :'app_pw';
CREATE DATABASE :"db" OWNER :"migrator_role";
REVOKE CONNECT ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO :"app_role";
EOSQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$CORE_DB_NAME" \
  -v migrator_role="$CORE_MIGRATOR_USER" \
  -v app_role="$CORE_APP_USER" <<'EOSQL'
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator_role" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_role";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator_role" IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"app_role";
EOSQL
