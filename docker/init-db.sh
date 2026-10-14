#!/bin/sh
set -eu

: "${KEYCLOAK_DB_NAME:?init-db.sh needs KEYCLOAK_DB_NAME}"
: "${KEYCLOAK_DB_USER:?init-db.sh needs KEYCLOAK_DB_USER}"
: "${KEYCLOAK_DB_PASSWORD:?init-db.sh needs KEYCLOAK_DB_PASSWORD}"

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
