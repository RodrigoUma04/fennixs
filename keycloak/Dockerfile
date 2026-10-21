FROM quay.io/keycloak/keycloak:26.8.1@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244 AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_DB=postgres

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.1@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm-config.json /opt/keycloak/data/import/realm-config.json
COPY entrypoint.sh /opt/keycloak/bin/fennixs-entrypoint.sh

RUN mkdir -p /opt/keycloak/data/state

ENTRYPOINT ["/opt/keycloak/bin/fennixs-entrypoint.sh"]

CMD ["start", "--optimized", "--import-realm"]
