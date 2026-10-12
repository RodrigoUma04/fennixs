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

exec /opt/keycloak/bin/kc.sh "$@"
