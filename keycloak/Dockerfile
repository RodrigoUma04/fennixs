FROM quay.io/keycloak/keycloak:26.8.2-3@sha256:7e66a96e4739239070aeaf72e4c5decf0f7f682cfc725b9f7ea2b336eaba38b3 AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_DB=postgres

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.2-3@sha256:7e66a96e4739239070aeaf72e4c5decf0f7f682cfc725b9f7ea2b336eaba38b3
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm-config.json /opt/keycloak/data/import/realm-config.json
COPY entrypoint.sh /opt/keycloak/bin/fennixs-entrypoint.sh

RUN mkdir -p /opt/keycloak/data/state

ENTRYPOINT ["/opt/keycloak/bin/fennixs-entrypoint.sh"]

CMD ["start", "--optimized", "--import-realm"]
