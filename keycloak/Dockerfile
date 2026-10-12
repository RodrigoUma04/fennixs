FROM quay.io/keycloak/keycloak:26.8.0@sha256:b0f60d489d51c5d113390bdf5461d4c06e6051be026c05549f2e1e10ec352bcc AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_DB=postgres

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.0@sha256:b0f60d489d51c5d113390bdf5461d4c06e6051be026c05549f2e1e10ec352bcc
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm-config.json /opt/keycloak/data/import/realm-config.json
COPY entrypoint.sh /opt/keycloak/bin/fennixs-entrypoint.sh

RUN mkdir -p /opt/keycloak/data/state

ENTRYPOINT ["/opt/keycloak/bin/fennixs-entrypoint.sh"]

CMD ["start", "--optimized", "--import-realm"]
