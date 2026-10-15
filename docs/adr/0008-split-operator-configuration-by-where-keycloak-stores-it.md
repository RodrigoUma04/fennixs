# Split operator configuration by where Keycloak stores it

- **Status:** Accepted
- **Date:** 2026-10-13


## Context

[ADR-0004](0004-use-keycloak-as-identity-provider.md) separated product configuration from instance configuration and put the second group in environment variables: hostname, TLS, SMTP, admin credentials and the registration toggle. [ADR-0007](0007-ship-realm-configuration-by-import.md) changed the applier from a reconciler to an import and restated that split unchanged.

Under a reconciler the claim held, because it ran on every start and re-read the variables. Under import it holds only for the settings Keycloak itself reads at startup, and the split does not divide them along that line.

**Measured against Keycloak 26.8.0.** Hostname, the HTTP toggle, both certificate paths and the database settings are Keycloak startup options, so a new value applies on the next start. The published ports and the certificate directory never reach Keycloak at all: Compose resolves them and binds them when the container is created, so they follow a `docker compose up -d` and not a `restart`. Two variables changed nothing either way, because Keycloak stores them in the realm beside the users: the registration toggle and `sslRequired`. The environment said `true` and `none`, the realm kept `false` and `external`, and Keycloak logged `Realm 'fennixs' already exists. Import skipped` and started normally. The bootstrap admin variables are their own case, documented by Keycloak itself: they seed the first administrator and are ignored once one exists.

**The operator is not locked out of fixing that.** The admin console authenticates against `master`, not our realm, so our `sslRequired` cannot block it, and one `PUT` corrects a locked realm with immediate effect and no restart. One case is still a cliff. Keycloak's `external` setting permits plaintext from a site-local source address, measured as 10.0.0.0/8, 172.16.0.0/12 and 192.168.0.0/16, so a tunnel handing out an RFC 1918 address needs nothing from us. A tunnel assigning from 100.64.0.0/10, as Tailscale does, is refused, and so is any public address. Those operators need `sslRequired=none` before their users can log in at all, and they cannot set it from the tunnel either, because Keycloak ships `master` at `external` and we do not ship that realm. Without a variable they would be administering from the host before anyone could sign in.

**The credential an updater would need is affordable, but the applier is the work.** A service account inside the Fennixs realm holding only `realm-management: manage-realm` can write that realm's settings and little else: users, clients, client secrets, role granting, other realms and `master` are all refused. The import can create it and the image can generate its secret, so it need never reach `.env`. It is not harmless, because `manage-realm` can rebind the realm's browser flow and Keycloak offers no finer grain. What defers the work is that a realm is not writable until Keycloak is running, so applying happens after the entrypoint hands off, as a poll-then-apply loop with its own failure modes. ADR-0007 deferred the updater until a shipped configuration change first needs to reach existing installations, and that reasoning stands.


## Decision

**A realm-stored setting gets a first-start environment variable only when getting it wrong stops end users from logging in.** `sslRequired` is the only one that does, and keeps `FENNIXS_SSL_REQUIRED`.

**Registration loses its variable.** It ships closed and is changed in the admin console. This supersedes ADR-0004's decision that registration is an environment variable decided at startup, though not its reasoning: that record kept the toggle out of the application because the narrowest role that can change it is `manage-realm`, and the admin console is the operator rather than the application. For a setting that is a checkbox, a variable that works once is worse than the checkbox.

**SMTP gets no variable.** Under first-start semantics it is the worst fit of the three: several correlated values, a typo leaves it configured but broken, and credential rotation makes the later change the normal case rather than the exception. That argues against a variable that applies once, not against the setting ever being `.env`-owned.

**`.env.example` marks the one remaining first-start variable as such, next to it**, and the entrypoint records the realm-affecting variables on the state volume and warns when one changes, naming where to change it instead. It warns and starts: it does not apply the change and does not refuse to start. The operator changing a variable is not the operator reading `.env.example`.

Settings Keycloak reads as startup options stay ordinary environment variables and carry none of this.


## Consequences

**The dangerous direction for `sslRequired` is tightening, not relaxing.** An operator who starts plaintext over a tunnel and later puts a certificate in front keeps a realm that still permits plaintext after the reason for permitting it is gone, and nothing in Keycloak reports that. The warning is weaker than applying the value would be, and that gap closes only with the updater. The opposite move fails safe: starting strict and later needing plaintext locks remote logins out rather than leaving them open.

**One less variable to supply**, and the one that remains is the one an operator must get right before the first start rather than one they may want to revisit.

**Password reset and email verification stay unavailable until the operator configures SMTP**, so the only recovery for a forgotten password is an administrator resetting it. Enabling `resetPasswordAllowed` without SMTP is worse than leaving it off: the user is told an email was sent while the send fails. That message is deliberately identical for an existing and a nonexistent username, so it cannot be made accurate without turning the reset form into an account enumeration oracle.

**The updater is expected to make `FENNIXS_SSL_REQUIRED` live**, reusing the credential and apply loop it needs anyway. Writing it on every start makes `.env` authoritative for that field and overrides an operator who changed it in the console. That is not what ADR-0007 rejected `keycloak-config-cli` for: that tool reset a field nobody had declared, because it wrote whole realm state through a model that lagged the server. Writing one named field we do declare is an ownership model to document rather than a surprise, and ADR-0007 already requires named changes over whole state. When it happens, four statements of the old behaviour go with it: the entrypoint warning, the first-start caveat in `.env.example`, the section there sending registration and SMTP to the admin console, and the operator configuration constraint in `AGENTS.md`.

ADR-0007's trigger for writing the updater is unchanged, a shipped configuration change that has to reach an installation already holding a realm, and nothing in this record brings that forward.

**No setting changes hands.** Who owns each one is exactly as ADR-0004 decided. What is superseded is the claim that the operator's half reaches Keycloak through environment variables, which holds only for the settings Keycloak reads at startup. This supersedes that claim as restated in ADR-0007, and the registration decision and equivalent wording in ADR-0004, and nothing else in either record.
