#!/bin/bash
# Keycloak migrates its own database schema on startup, so a version change is an
# unannounced migration on the operator's identity data. Say so before it happens.
#
# bash rather than sh: the readiness probe below needs /dev/tcp, and the image ships
# neither curl nor wget.
set -euo pipefail

# Only a start migrates anything or imports a realm, so every other subcommand, such
# as show-config, passes straight through without reporting or recording a thing.
if [ "${1:-}" != "start" ]; then
  exec /opt/keycloak/bin/kc.sh "$@"
fi

STATE=/opt/keycloak/data/state
MARKER="$STATE/keycloak-version"
REALM_MARKER="$STATE/realm-settings"

VERSION=$(sed -n 's/^Keycloak - Version //p' /opt/keycloak/version.txt 2>/dev/null || true)
SETTINGS="sslRequired=${FENNIXS_SSL_REQUIRED:-external}"

if [ -n "$VERSION" ]; then
  PREVIOUS=$(cat "$MARKER" 2>/dev/null || true)

  if [ -z "$PREVIOUS" ]; then
    echo "FENNIXS: first start on Keycloak $VERSION"
  elif [ "$PREVIOUS" != "$VERSION" ]; then
    echo "FENNIXS: Keycloak $PREVIOUS -> $VERSION, migrating schema on startup"
  fi
fi

# Realm settings are written by the import, which runs only when the realm does not
# already exist, so editing one of these later is ignored without any error. The
# default below has to match the one Compose supplies. See ADR-0008.
PREVIOUS_SETTINGS=$(cat "$REALM_MARKER" 2>/dev/null || true)

if [ -n "$PREVIOUS_SETTINGS" ] && [ "$PREVIOUS_SETTINGS" != "$SETTINGS" ]; then
  echo "FENNIXS: .env changed $PREVIOUS_SETTINGS -> $SETTINGS. Realm settings are stored on"
  echo "FENNIXS: the first start only, so this start does not apply it and nothing here can"
  echo "FENNIXS: tell you whether the realm already holds it. The live value is in the admin"
  echo "FENNIXS: console under Realm settings > General > Require SSL."
fi

# Record both markers only once the server reports ready. Writing them here, before
# exec, is what the messages above are compared against next start, so a start that
# fails afterwards would consume them and say nothing on the attempt that really
# migrates. The loop is a child of the server process and dies with the container, so
# a failed start records nothing. If it gives up while the server is healthy, the
# messages simply repeat on the next start, which is the harmless direction.
(
  for ((i = 0; i < 150; i++)); do
    if (
      exec 3<>/dev/tcp/127.0.0.1/9000 &&
        printf 'HEAD /health/ready HTTP/1.0\r\n\r\n' >&3 &&
        IFS= read -r line <&3 &&
        [[ $line == HTTP/1.[01]" 200"* ]]
    ) 2>/dev/null; then
      {
        printf '%s\n' "$VERSION" >"$MARKER"
        printf '%s\n' "$SETTINGS" >"$REALM_MARKER"
      } 2>/dev/null ||
        echo "FENNIXS: warning: could not write $STATE, version and realm setting changes will not be reported"
      exit 0
    fi
    sleep 2
  done
) &

exec /opt/keycloak/bin/kc.sh "$@"
