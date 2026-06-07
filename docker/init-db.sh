#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE fennixs_auth;
    CREATE DATABASE fennixs_core;
EOSQL
