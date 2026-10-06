# Ship realm configuration by import

- **Status:** Accepted
- **Date:** 2026-10-04


## Context

[ADR-0004](0004-use-keycloak-as-identity-provider.md) decided to apply realm configuration with a reconciler rather than an import, and named `keycloak-config-cli` as the mechanism. Its reasoning is not in question here: import acts only when the realm does not already exist, so a new realm file arriving on an existing installation does nothing.

**The reconciler lags Keycloak, and does not say so.** Its newest release is 6.5.1, from 2026-05-22, whose highest published image pairs with Keycloak 26.5.5 from 2026-03-05. Keycloak 26.6.0 shipped on 2026-04-08, so that release was already a minor behind on the day it appeared, and 26.7 and 26.8 have shipped since with no release to match them. Keycloak ships minors quarterly. The lag is structural rather than incidental: the tool compiles against Keycloak's admin client, so every Keycloak minor needs a rebuild and a release from a third party.

Run against a Keycloak newer than it was built for, it neither fails nor warns. Its version check compares the major version alone, so every Keycloak 26.x looks alike to it, and it discards fields it does not recognise by design, matching Keycloak's own admin client. Measured on 2026-10-04 against Keycloak 26.8.0: everything Fennixs declares applied correctly, and a reconcile that wrote also reset `webAuthnPolicyResidentKey` from `required` to `not specified` and exited zero. A control confirms the cause is the version gap rather than WebAuthn handling in general. A tool that silently narrows security configuration is the wrong shape for this job, which is the same reasoning ADR-0004 used when it declined to write an authentication server.

**The update path has no users.** A reconciler exists to carry a configuration change to an installation that already has a realm. Fennixs has no application code, no release, and no configuration change that has ever needed shipping. The requirement ADR-0004 solved is real and arrives on the second configuration version, not the first.


## Decision

Ship the realm configuration as an import, carry no reconciler, and write our own updater when a configuration change first needs to reach existing installations.

**The shipped realm records which configuration version it is**, so that updater has a defined starting point rather than having to infer one.

**The updater will apply named changes, not whole state.** The failure measured above requires reading the realm into a model that can fall behind the server. Writing only the fields a change names avoids that class of failure rather than managing it.

**Overwriting on import was considered and is not available.** Keycloak's only overwrite path deletes the realm, and every user in it, before reimporting, and the startup import cannot reach that behaviour in any case.

**The product and instance split from ADR-0004 is unchanged.** Clients, protocol mappers, token lifespans and password policy are ours. Hostname, TLS, SMTP, admin credentials and the registration toggle belong to the operator and come from environment variables.


## Consequences

**A configuration change we ship does not reach an existing installation, and nothing says so.** Keycloak logs that the realm already exists, reports the import as finished successfully, and starts normally. This is the cost of the decision and the thing most likely to catch us out: ship a realm change before the updater exists and nobody learns, ourselves included. Detecting that belongs with the version logging the entrypoint already does, at the point the first update makes it meaningful.

**Operator changes survive.** The reconciler achieved this by tracking the resources it manages, which is behaviour that can be configured off. Import achieves it by not writing to a realm that exists.

**The Keycloak version is no longer pinned to what a third party has built against.** Keycloak fixes vulnerabilities in the current `major.minor` release, or for lower severity in the following one, and directs anyone needing longer support to the Red Hat build. Older minors are not served. Pinning Keycloak to whatever the reconciler supported therefore meant an identity provider receiving no security fixes until the reconciler moved, which is a worse exposure than any configuration concern.

**One fewer dependency and one fewer JVM.** ADR-0004 noted that self-hosted stacks would run two JVMs sizing their heaps independently of each other. The reconciler was the second, so that consequence is withdrawn.

**We owe the updater, and the first time we need it is a poor time to design it.** The risk is that the first configuration change arrives under pressure, as a security fix, and it gets written in a hurry. Recording the configuration version from the first release is what keeps the work possible later rather than urgent now.

**Keycloak may make this unnecessary, and this record does not assume it will.** Admin API v2 offers declarative configuration and is intended to grow into several APIs, but at 26.8 it is a preview feature covering clients only, full parity with the original API is an explicit non-goal, and nothing published commits to realm-level configuration. Worth re-checking when the updater is actually needed.

**Nothing here is hard to reverse.** The durable artefact is the realm file in Keycloak's export format, which a reconciler, an updater of our own, and a manual partial import all read. The applier is swappable, which is what makes deferring the choice a decision rather than a delay.

**This supersedes the configuration mechanism in ADR-0004 and nothing else.** Running Keycloak, shipping it as a wrapped image pinning a version and carrying matching realm configuration, building at image build time and starting with `--optimized`, giving Keycloak its own database, and keeping users out of the shipped realm file all stand.
