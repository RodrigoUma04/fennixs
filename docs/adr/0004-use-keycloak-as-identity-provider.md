# Use Keycloak as the identity provider

- **Status:** Accepted
- **Date:** 2026-10-02


## Context

Given that core-api [depends only on standard OIDC](0003-depend-only-on-standard-oidc.md), the identity provider is a deployment choice. This record covers which one we run.

One requirement shapes the choice. Registration is closed by default on a self-hosted instance and open on the hosted one, but an operator can open it on their own, so whichever provider we pick has to handle self-service signup in both shapes.

**Writing our own** was seriously considered, motivated by footprint for self-hosters. The realistic scope is smaller than replicating Keycloak: argon2id hashing, short-lived access tokens, rotating refresh tokens with reuse detection, and TOTP. It is achievable.

It was rejected because authentication bugs are silent. Earlier work on a previous version of this project produced a dormant failure on refresh for soft-deleted users, and left session hardening unfinished. Those are exactly the subtle failure modes an established provider has already found and fixed, and owning that surface is a permanent maintenance cost rather than a one-time build.

**Footprint is not a performance question.** Because tokens validate locally, the identity provider is not in the request path. A self-hosted instance with a handful of users contacts it on login and when an access token needs refreshing, a few hundred times a day at most, rather than on every request. The cost is idle memory, a few hundred megabytes for Keycloak on default settings, not throughput. Any specific figure is something to measure on the target hardware rather than a constant to plan around.

**Lighter providers exist.** Authelia in particular is a single Go binary rather than a JVM, so its idle footprint is far smaller, and it stores its configuration in a file rather than a database, which makes shipping config updates trivial.

It has no user self-registration, and its maintainers treat that as deliberate rather than missing: registration is considered outside the job of an authentication service. Authelia can surface a link to a registration service the operator runs separately, but gains no registration of its own, so an account must already exist in the backend before anyone can sign in. Choosing Authelia therefore means building that registration service ourselves, both for the hosted offering and for any self-hosted instance whose operator opens registration.

**The onboarding objection is solvable.** "Create a realm, configure a client, set the redirect URIs" is a wall that self-hosters bounce off, but realm configuration can be shipped rather than explained.


## Decision

Run **Keycloak** as the identity provider, in both development and production.

**Ship it as a wrapped image.** A derived image pins a specific Keycloak version and carries the matching realm configuration. It runs `kc.sh build` at image build time and starts with `--optimized`, because Keycloak otherwise runs that build itself on every start. Both halves are needed: the build alone still pays for the startup check without the flag.

**Apply configuration with a reconciler, not an import.** `keycloak-config-cli` runs on every startup and applies only the differences. It tracks which resources it manages, so by default it leaves users and undeclared resources untouched. That behaviour is configurable, so the shipped configuration must not turn it off.

**Separate product config from instance config.** Clients, protocol mappers, token lifespans and password policy are ours, reconciled unconditionally. Hostname, TLS, SMTP, admin credentials and the registration toggle belong to the operator, come from environment variables, and are never touched.

**Users are in neither category.** They are the operator's data and the shipped realm file contains none, which needs saying because a realm export can include them. Exporting a working development realm and shipping it would create our test accounts, with their credentials, on every installation.


## Consequences

Self-hosted stacks carry a few hundred megabytes more than a minimal alternative would, and now run two JVMs rather than one. Each sizes its heap as a share of the memory it believes it has, and neither knows about the other, so without limits the pair can plan for more than the host meant to give them.

**We own the pairing.** A Fennixs auth image version means one tested combination of Keycloak version and realm configuration. That is the main thing the wrapped image buys, since it stops operators from running a Keycloak version the configuration was never tested against.

Keycloak migrates its own database on startup, so a version bump is also a schema change. Its upgrade guide is per-version and asks for a database backup first, which means bumps are a deliberate operation rather than a tag change. Each one also requires re-verifying the realm configuration against the new version.

**Realm import cannot ship configuration updates.** `--import-realm` only acts when the realm does not already exist, so a new realm file arriving on an existing installation is silently ignored. Partial import can apply a file to a live realm, with per-resource Fail, Skip or Overwrite handling, but it is an operator action through the admin console or CLI rather than something a container can perform for itself at startup. Neither is a mechanism an image update can rely on.

**The reconciler can, because it is declarative and idempotent:** applying the same configuration twice produces the same result. That means a fresh install and an upgrade take one code path rather than two, and the path that only runs on upgrades is the one that never gets tested.

Keycloak is Apache-2.0, so a derived image is permitted. It must not be branded so as to imply endorsement by the Keycloak project.

Credentials live in Keycloak's own database, which core-api has no grant on, so core-api cannot read password hashes or provider sessions. That boundary between authentication data and financial data comes free rather than being built. It does not extend to administrative authority, because the owner role is a column in core-api's own database beside the financial tables, so that guarantee needs its own mechanism, enforced in the database rather than by this boundary, and recorded separately.

**Disabling a user and signing them out are different operations.** Keycloak invalidates refresh tokens lazily, through a per-user not-before timestamp: existing tokens are not deleted, they are rejected as stale the next time a client tries to use one. Setting that timestamp and terminating live sessions is what the admin logout operation does, and it is also what fires back-channel logout at us. Toggling `enabled` alone does neither, so revoking someone's access at Keycloak means signing them out, not just disabling them, and a procedure that can be skipped is a poor last line of defence. What makes it tolerable is that Fennixs does not depend on it: the enabled flag on our own user row takes effect on the next request regardless of what Keycloak was or was not told.

Keycloak's native role claims are not consumed, and no protocol mapper is needed for them, since roles live on the Fennixs user row. Audience is the exception: Keycloak derives `aud` from client scope and role mappings rather than adding the resource server automatically, so the shipped realm must be configured and verified to name the Fennixs client in `aud`.

Self-hosters who already run an identity provider can point Fennixs at theirs instead. Because core-api reads only standard claims, that path costs a short page of provider-neutral documentation rather than a guide per provider. It does require that page to cover reading validation errors out of the `WWW-Authenticate` header, as noted in [ADR-0003](0003-depend-only-on-standard-oidc.md), without which the support burden outweighs the benefit.
