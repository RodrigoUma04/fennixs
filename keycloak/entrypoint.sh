#!/bin/sh
# Keycloak migrates its own database schema on startup, so a version change is an
# unannounced migration on the operator's identity data. Say so before it happens.
set -eu

MARKER=/opt/keycloak/data/state/keycloak-version
VERSION=$(sed -n 's/^Keycloak - Version //p' /opt/keycloak/version.txt 2>/dev/null || true)

if [ -n "$VERSION" ]; then
  PREVIOUS=$(cat "$MARKER" 2>/dev/null || true)

  if [ -z "$PREVIOUS" ]; then
    echo "FENNIXS: first start on Keycloak $VERSION"
  elif [ "$PREVIOUS" != "$VERSION" ]; then
    echo "FENNIXS: Keycloak $PREVIOUS -> $VERSION, migrating schema on startup"
  fi

  { printf '%s\n' "$VERSION" > "$MARKER" ; } 2>/dev/null ||
    echo "FENNIXS: warning: could not write $MARKER, version changes will not be reported"
fi

# Realm settings are written by the import, which runs only when the realm does not
# already exist, so editing one of these later is ignored without any error. The
# default below has to match the one in realm-config.json. See ADR-0008.
REALM_MARKER=/opt/keycloak/data/state/realm-settings
SETTINGS="sslRequired=${FENNIXS_SSL_REQUIRED:-external}"
PREVIOUS_SETTINGS=$(cat "$REALM_MARKER" 2>/dev/null || true)

if [ -n "$PREVIOUS_SETTINGS" ] && [ "$PREVIOUS_SETTINGS" != "$SETTINGS" ]; then
  echo "FENNIXS: $PREVIOUS_SETTINGS -> $SETTINGS was not applied. Realm settings are"
  echo "FENNIXS: written on the first start only. Change it in the admin console under"
  echo "FENNIXS: Realm settings > General > Require SSL, or this start has no effect."
fi

{ printf '%s\n' "$SETTINGS" > "$REALM_MARKER" ; } 2>/dev/null ||
  echo "FENNIXS: warning: could not write $REALM_MARKER, realm setting changes will not be reported"

exec /opt/keycloak/bin/kc.sh "$@"
