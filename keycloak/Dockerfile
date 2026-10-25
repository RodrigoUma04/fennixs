FROM quay.io/keycloak/keycloak:26.8.1@sha256:a78d6b79572168cb25886b80a8b5b8d4ed4390743e3eecb810538683e209dd6b AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_DB=postgres

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.1@sha256:a78d6b79572168cb25886b80a8b5b8d4ed4390743e3eecb810538683e209dd6b
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm-config.json /opt/keycloak/data/import/realm-config.json
COPY entrypoint.sh /opt/keycloak/bin/fennixs-entrypoint.sh

RUN mkdir -p /opt/keycloak/data/state

ENTRYPOINT ["/opt/keycloak/bin/fennixs-entrypoint.sh"]

CMD ["start", "--optimized", "--import-realm"]
