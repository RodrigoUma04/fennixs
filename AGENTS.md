# AGENTS.md

Guidance for AI coding agents working in this repository, in the format described at
[agents.md](https://agents.md). It is deliberately tool agnostic: nothing here assumes a
particular assistant, and no assistant-specific configuration is committed to this repo.

Human contributors should read [CONTRIBUTING.md](.github/CONTRIBUTING.md). Everything in it
applies to agents too, and this file does not repeat it.


## Project state

Fennixs is a self-hosted personal finance tracker. **There is no application code yet.** The
repository holds licensing, community health files, and architecture decision records.

Planned: Java and Spring Boot for `core-api`, Angular for the frontend, Python for the
statement importer, PostgreSQL, Keycloak for authentication, Docker Compose for self-hosting.
None of it is written.

This matters for how you work here. Most requests are documentation, configuration, or design
discussion. If you are about to generate application code, check that it was actually asked
for.


## Read before changing anything

Architecture decisions live in [docs/adr](docs/adr), one record per decision. They are binding,
not advisory, and several of them exist specifically to prevent plausible-looking code.

| Record | Decides |
| --- | --- |
| ADR-0001 | AGPL-3.0-or-later licensing |
| ADR-0002 | Contributions under the DCO |
| ADR-0003 | Identity depends only on standard OIDC, never on provider-specific claims |
| ADR-0004 | Keycloak is the shipped identity provider |
| ADR-0005 | Browsers authenticate with a server-side session, not a token in the browser |
| ADR-0006 | Row ownership is enforced by PostgreSQL, not by query discipline |
| ADR-0007 | Realm configuration ships as an import, with the updater deferred |

**Records are immutable once accepted.** If a change contradicts one, write a new record that
supersedes it. Never edit an accepted record to match new code.


## Rules

**Never commit, push, or open a pull request unless explicitly asked.** Stage nothing. Leave
the working tree for the author to inspect. Proposing a commit message is useful; running
`git commit` is not yours to do.

**Every commit is signed off.** `git commit -s`. This repository requires the DCO, and an
unsigned commit cannot be merged.

**Do not create files nobody asked for.** No summary documents, no design notes, no
`IMPLEMENTATION.md`, no README in every directory. If a change is worth explaining, explain it
in the pull request or in an ADR.

**Verify library behaviour against current documentation rather than recalling it.** The
planned stack is newer than most training data: Spring Boot 4 and Spring Security 7 differ from
Spring Boot 3 in ways that matter, Keycloak's admin API and realm configuration change between
releases, and PostgreSQL row-level security has exact semantics that are easy to misremember. A
confident wrong answer about a security mechanism is worse than no answer.

**Do not add a dependency for something a few lines solve.** Prefer the standard library, then
a platform feature, then something already in the project. New dependencies in a self-hosted
application are a maintenance and supply chain cost for every operator.

**Do not add speculative abstractions.** No interface with one implementation, no configuration
for a value that never changes, no scaffolding for a feature that does not exist.

**Do not weaken a security control to simplify code.** Input validation at trust boundaries,
error handling that prevents data loss, and the constraints below are not candidates for
simplification.


## Architecture constraints

These are the ones an agent is most likely to get wrong because the wrong version looks
ordinary. Each is explained in full in the record cited.

**Identity is keyed on `(iss, sub)`, never `sub` alone.** A subject identifier is only unique
within its issuer. See ADR-0003.

**Never read provider-specific claims.** No `realm_access.roles`, no Keycloak-shaped token
structure. Roles, email, and the enabled flag live on the Fennixs user row. The only standard
claims Fennixs reads are `iss`, `sub`, `aud`, `exp`, `iat`, and `email`. See ADR-0003.

**Tokens stay server-side.** The browser gets an `httpOnly` session cookie. Never place an
access token, refresh token, or ID token where browser JavaScript can reach it. See ADR-0005.

**Unauthenticated API responses are 401, not a redirect.** A redirect is correct for a
navigation and useless to a background request. See ADR-0005.

**Every table holding user data needs row-level security enabled**, and any unique constraint
over user data must be composite with `user_id`. Referential integrity checks bypass row
security, so a global unique constraint leaks the existence of other users' rows through failed
inserts. See ADR-0006.

**The current user is set per transaction** with `set_config('app.user_id', ?, true)`, inside
the transaction, from one place. The third argument scopes it to the transaction. Passing
`false` leaves the value on a pooled connection for the next request, which is a cross-user data
leak that produces no error. See ADR-0006.

**Fennixs contains no billing, plan, or revenue code.** Payments and platform analytics live in
a separate service outside this repository. Fennixs receives a neutral account-enabled flag and
never learns what a subscription is.

**Application features do not get Keycloak admin credentials.** The narrowest Keycloak role that
can change realm configuration also rewrites authentication flows and registers clients, so
holding it would turn a `core-api` compromise into an identity provider compromise. Operator
configuration belongs in environment variables.


## Writing

Documentation, commit messages, comments, and pull request descriptions:

- **No em dashes.** Use commas, colons, or separate sentences.
- Imperative mood in commit subjects and ADR titles, as in "add", not "added" or "adds".
- Plain statements over hedging. If something is uncertain, say what is uncertain and why.
- British or American spelling is not enforced, but be consistent within a file.

Do not describe work as complete, verified, or passing unless a command was run and its output
checked. Report what actually happened, including failures.


## Conventions

YAML, JSON and Markdown are formatted by [Prettier](https://prettier.io), enforced by a
pre-commit hook and checked in CI. Files whose spacing carries meaning are excluded in
`.prettierignore`, including the architecture decision records and this file. Do not reformat an
excluded file to match a formatter, and keep the Prettier version in `.pre-commit-config.yaml`
in step with the one pinned in `.github/workflows/ci.yml`.

There are no Java, TypeScript or Python conventions yet, because there is no code to have
conventions about. Naming, test structure and package layout will be decided alongside the first
service and documented here then.

Until that happens, do not invent them, and do not import conventions from another project as
though they were established here. If a choice needs making, raise it rather than settling it
silently.
