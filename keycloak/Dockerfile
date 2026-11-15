FROM quay.io/keycloak/keycloak:26.8.2-2@sha256:c68e286cacd87b942ac1939a0a1481461109d6acd3a57dc496969a319a76bbf0 AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_DB=postgres

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.2-2@sha256:c68e286cacd87b942ac1939a0a1481461109d6acd3a57dc496969a319a76bbf0
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm-config.json /opt/keycloak/data/import/realm-config.json
COPY entrypoint.sh /opt/keycloak/bin/fennixs-entrypoint.sh

RUN mkdir -p /opt/keycloak/data/state

ENTRYPOINT ["/opt/keycloak/bin/fennixs-entrypoint.sh"]

CMD ["start", "--optimized", "--import-realm"]
