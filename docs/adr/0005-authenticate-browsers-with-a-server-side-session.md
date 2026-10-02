# Authenticate browsers with a server-side session

- **Status:** Accepted
- **Date:** 2026-10-02


## Context

[ADR-0003](0003-depend-only-on-standard-oidc.md) deferred how a credential reaches core-api. This record settles it for browser clients.

core-api serves an Angular application, so something has to hold a credential between requests. If the credential arriving on each request is a token, then either the browser keeps it, where JavaScript can reach it, or a separate component in front of core-api keeps it on the browser's behalf. Neither is free.

RFC 10017, published as BCP 212 in August 2026, is the IETF Best Current Practice for browser-based OAuth. Its recommendation is to manage tokens server-side in the context of a cookie-based session, so that no token is available to extract from the browser at all. Where that is not feasible it gives a descending ladder: a service worker, then memory only, then persistent storage such as `localStorage`, which it rates least safe.

A token-based alternative was worked out and discarded. The shape was to take the access token out of Spring's own storage, write it into a cookie, read it back with a custom resolver, and own the refresh. That uses the framework's client support to obtain tokens and then keeps a second copy somewhere the framework does not manage. Spring still refreshes its own copy, but the cookie the browser holds is a snapshot, so propagating each new token into it becomes ours to write and ours to get wrong. More moving parts in exchange for one property, statelessness, that nothing currently needs.

A mobile client is planned, some months out. Native applications have operating system secure storage, so tokens are genuinely safe there in a way browser storage is not. That is a separate decision for a separate client and does not bear on this one.


## Decision

core-api is the OAuth2 client. Keycloak authenticates the user on its own domain, and core-api exchanges the resulting code for tokens and holds them server-side.

**The browser carries a session cookie and nothing else.** httpOnly, Secure, and `SameSite=Lax`. No access token, no refresh token, nothing reachable from JavaScript.

`Lax` rather than `Strict` is forced by the flow: the provider's redirect back to the callback is a cross-site navigation, and `Strict` withholds the cookie there, so the session holding the pending authorization request cannot be found and login fails. `Lax` still withholds the cookie from cross-site requests that are not top-level navigations, so state-changing requests remain protected, and CSRF tokens cover the rest.

**PKCE is required, and is not inherited.** Spring applies it automatically only when a client has no secret, and core-api is confidential, so it has to be switched on deliberately. OAuth 2.1 and the OAuth security best current practice require proof key of every client regardless of whether it holds a secret, because a secret defends against a stolen code being redeemed and not against one being injected.

**The framework provides the login path.** The authorization redirect and the callback that exchanges the code are framework endpoints, and refresh happens transparently when something asks for the token. We write no login endpoint, no callback, no refresh exchange and no token storage. The one thing we do add is the trigger described below, since refresh only fires when something asks for the token.

**Logging out ends the provider session too.** RP-initiated logout is standard OIDC, and the provider advertises its `end_session_endpoint` in the discovery document, so this costs configuration rather than code. Without it a local logout kills our session and leaves the provider's alive, so the next click on login silently re-authenticates with no credentials asked for, which on a shared machine is the opposite of logging out.

**Identity resolution is a service, not part of an adapter.** The lookup from `(iss, sub)` to an internal user id, carrying the role, the email address and the enabled flag, lives in its own class. The session adapter calls it, and so does anything that comes later.


## Consequences

**CSRF protection is required on state-changing endpoints.** A browser attaches cookies to cross-site requests by itself, which it never does for an `Authorization` header. `SameSite` narrows this rather than closing it.

**The frontend cannot read its own identity**, because the cookie is httpOnly by design. It asks an endpoint instead and holds the answer in memory. Unauthenticated API calls have to return 401 rather than a redirect, since a redirect is right for a navigation and useless to an XHR, and the frontend then sends the browser to the login path as a full navigation.

**Revocation has three paths.** Anything Fennixs decides is immediate, because the enabled flag is read alongside the identity lookup. Provider-side decisions arrive two other ways. Back-channel logout lets the provider push a session termination, which is standard OIDC and immediate. And the refresh cycle is a mandatory check-in: Spring re-authorizes only once the access token has expired, so a disabled account fails its next refresh and that failure is ours to act on by ending the session. Something has to resolve the authorized client per request for that check to run at all, and the access token lifetime, fifteen minutes, is then the maximum delay before a provider-side revocation lands.

**The session lifetime is the main exposure window, so it is ours to set.** The session is the credential here, which makes its timeout a security parameter rather than a convenience default. An idle timeout renews on activity, so on its own it never ends an active attacker's access; an absolute cap is what bounds that. Both belong with the token lifespans in [ADR-0004](0004-use-keycloak-as-identity-provider.md)'s product config, shipped rather than left to operators.

That absolute cap must not exceed the provider's own SSO session maximum. Otherwise a Fennixs session can outlive the provider session it was derived from, which means holding a credential after the authority that granted it has stopped vouching for the user. Provider session expiry is passive and fires no event, so nothing pushes that fact to us; the cap is what makes it structural rather than dependent on a refresh attempt happening to land.

**core-api is stateful.** One instance is unaffected. When the hosted tier needs more, a single Spring Session dependency replaces the `HttpSession` with a Redis-backed one, with no annotation and no further configuration beyond connection details. Keep it behind a profile so self-hosters never acquire a Redis dependency they were promised they would not need. That same store makes sessions enumerable and deletable per user once they are indexed by principal name. Killing every session a user holds, across every replica, immediately, is something tokens cannot offer. The hosted tier therefore gains revocation power by staying session-based rather than losing it.

**Adding a token-based client stays additive.** A second filter chain, discriminated by the presence of an `Authorization` header rather than by path so routes are not duplicated, plus an adapter that calls the same resolution service. Controllers, authorization rules and the user table are untouched. Inline that resolution into the session adapter instead and the two copies drift, which is the reason it is a service from the first commit rather than a refactor later.
