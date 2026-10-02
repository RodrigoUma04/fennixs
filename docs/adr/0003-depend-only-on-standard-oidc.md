# Depend only on standard OIDC for authentication

- **Status:** Accepted
- **Date:** 2026-10-02


## Context

Fennixs ships in two shapes. Self-hosted instances are run by the people using them, usually one to five users, and their operators care about footprint and a single `docker compose up`. The hosted offering is run by the maintainers for many users, with billing attached.

What genuinely differs is footprint tolerance, and what the operator already has. A home server running six containers has a different budget from a hosted platform, and a meaningful number of self-hosters already run an identity provider for their other services and would rather point Fennixs at it than run a second one.

That makes the identity provider a deployment choice rather than a fixed part of the system, but only if nothing in the application is written against a particular one.

The risk is specific rather than theoretical. OIDC standardises `iss`, `sub`, `aud` and `exp`, but says nothing about roles. Keycloak invents its own shapes, putting realm roles in `realm_access.roles` and client roles in `resource_access.<client-id>.roles`. Code written to read those is coupled to Keycloak permanently, and any replacement has to imitate its quirks.

A second trap sits alongside it. OIDC defines `sub` as "a locally unique and never reassigned identifier within the Issuer", an opaque string whose format is the provider's business. Its uniqueness is scoped to the issuer, so a `sub` from one provider means nothing next to a `sub` from another. Using it directly as the foreign key on financial rows means that changing provider, or reinstalling the current one, orphans every transaction a user owns.


## Decision

Fennixs knows one thing about its identity provider: an issuer URL.

**Validation is local.** core-api fetches the issuer's discovery document, caches its JWKS, and verifies signatures in process. It does not use token introspection, which would put the identity provider in the request path.

**Only standard claims are read.** Fennixs reads `iss`, `sub`, `aud`, `exp`, `iat` and `email`, and nothing else. No custom claims, no protocol mappers, no provider-specific shapes. The contract is covered by tests that build tokens from our own fixtures rather than from a running provider.

**Fennixs data stays in Fennixs.** Roles such as `OWNER` and `USER` describe authority inside this application, so they live on our own user row, not in the token. An external identity provider has no reason to know they exist.

The email address follows the same rule, for a different reason. `email` only appears when the `email` scope is requested, and nothing in the spec requires an access token to carry it. Keycloak happens to, others need not, so reading it per request would be depending on one provider's behaviour. It is stored on the user row instead, read at first sight and refreshed from the ID token at every subsequent login.

It has to be stored rather than read from the token, because jobs that send notifications or deliver a data export run with no request and no token in hand. The cost is a staleness window of one login: a user who changes their address at the provider sees the old one in Fennixs until they next sign in. Nothing worse than that, because `email` is a copy of a fact the provider owns and never an identifier.

**Identity is mapped at the edge.** core-api keeps its own internal user id. The external identity is the pair `(iss, sub)` rather than `sub` alone, since a subject is only unique within its issuer. That pair is translated to the internal id on the way in, and financial rows reference the internal id only. The role, the email address and the account's enabled flag all come back from that same lookup, so keeping them out of the token costs nothing.

**Audience is validated, always.** Whatever token Fennixs reads, its `aud` must name Fennixs. This is not a preference of ours, and both specs require it against their own value. RFC 9068 says "the resource server MUST validate that the 'aud' claim contains a resource indicator value corresponding to an identifier the resource server expects for itself". OIDC Core section 3.1.3.7 says "the Client MUST validate that the aud (audience) Claim contains its client_id value", and that the ID Token "MUST be rejected if it does not list the Client as a valid audience". The stakes are concrete in the bring-your-own case, where the operator's provider also issues tokens for their other applications, any of which would otherwise be accepted here.

**No provider-specific dependency.** Talking to the provider through standard OIDC flows is fine, and a service that performs the login itself necessarily does. What is forbidden is depending on anything provider-specific: no vendor SDK, no admin API calls, no provider-specific claim shapes, and no provider named in configuration beyond the issuer URL.


## Consequences

Changing identity provider is a configuration change as far as the application is concerned, since there is no claim mapping to redo. An installation with data in it also needs its users relinked, because a new provider issues subjects Fennixs has never seen and nothing in a token says which existing account is meant. That mechanism does not exist yet and is the missing half of the swap this record promises. The likeliest trigger is not a deliberate migration but a self-hoster who reinstalls their provider and finds every subject is new.

**Bringing your own identity provider becomes realistic rather than theoretical.** The operator registers a client and configures its redirect URIs, the scopes, and an audience naming it. Nothing about roles, nothing about custom claims. The full set of configuration steps belongs in the bring-your-own documentation rather than here, because it will gain entries as people hit them, and that page can be corrected without superseding this record.

**Audience configuration is the one real trap.** Providers do not all put the Fennixs client in `aud` by default, and Keycloak in particular derives audiences from client scope and role mappings rather than adding them automatically. The shipped realm must be verified to emit the Fennixs client in `aud`, and the bring-your-own documentation has to tell operators to check the same thing.

**Operators will hit validation failures whose cause is not apparent.** A token can be rejected for the issuer, the audience, the signing key, or clock skew, and from the outside every one of those looks the same. Worse, a frontend that redirects on 401 turns every one of them into a login loop, a symptom that points nowhere near the cause. The reason is available, since RFC 6750 requires it in the `WWW-Authenticate` header, but nothing about the symptom suggests looking there. Making it findable is a cost this decision creates, and it falls mostly on the bring-your-own path.

**The provider is not consulted on ordinary requests.** Nothing in serving a request asks it anything, which is the point of validating locally. A token refresh does contact it, so a provider-side decision such as a disabled account can arrive that way without extra wiring. But how often refresh happens, or whether it happens at all, depends on how credentials arrive, so this record cannot promise that a decision the provider makes after login reaches Fennixs.

**What Fennixs decides is immediate.** core-api reads its own account state alongside the identity lookup. Disabling an account there takes effect immediately, which is the path that matters for non-payment and abuse on the hosted tier.

The internal user id costs a lookup on the way in. It buys immunity from provider changes orphaning financial data, and it is the one place the role, email and enabled flag are read from.

How a credential reaches core-api is **not decided here**. A session cookie, a bearer token, or both at once for different clients are all compatible with everything above, because what changes is the adapter that extracts the credential rather than what Fennixs trusts about it. That decision gets its own record.

What happens when a credential arrives for a subject core-api has not seen before is likewise **not decided here**. It is a separate decision with real alternatives, and gets its own record when provisioning is built. It is the same question as relinking above: an unknown subject is either a new user or an existing one arriving through a new identity, and nothing in the credential tells them apart. Settling one without the other hands a fresh empty account to anyone whose provider changed. This record assumes only that some mapping from `(iss, sub)` to an internal id has to exist.

If Fennixs ever calls the identity provider directly for something a credential cannot carry, the seam is broken and the provider is no longer swappable.
