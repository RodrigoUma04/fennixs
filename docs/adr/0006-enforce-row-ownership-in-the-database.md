# Enforce row ownership in the database

- **Status:** Accepted
- **Date:** 2026-10-02


## Context

Every financial row in Fennixs belongs to exactly one user, and no other user may read it. That is the product's central promise, not a feature of it.

The usual way to keep that promise is to scope every query by user and to keep doing so correctly forever. It works until it doesn't. One repository method written without its filter, one join that widens the result set, one endpoint that trusts a path parameter, and another person's transactions are on screen. Nothing errors, nothing logs, and the bug is invisible in review because the query looks ordinary.

That is a bad place to put a guarantee an application exists to make. It also scales badly: every additional author, whether a contributor or a coding agent, is one more place the filter has to be remembered.

[ADR-0004](0004-use-keycloak-as-identity-provider.md) gives a related boundary for free: credentials live in Keycloak's database and core-api holds no grant on it. But the owner role is a column in core-api's own database, beside the financial tables and under the same grants, so nothing in that arrangement constrains what core-api's own queries can reach.


## Decision

Row ownership is enforced by PostgreSQL, not by query discipline.

**Row-Level Security on every table holding user data**, with a policy keyed on the current user:

```sql
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions FORCE ROW LEVEL SECURITY;
CREATE POLICY own_rows ON transactions
  USING (user_id = current_setting('app.user_id')::uuid);
```

One `USING` expression with no `FOR` clause covers reads and writes both, so the same predicate that hides another user's rows also refuses to create one.

**The current user is set per transaction**, from the internal user id that [ADR-0003](0003-depend-only-on-standard-oidc.md) already resolves at the edge. Per transaction rather than per connection, because connections are pooled and a value that outlives the request hands one user's identity to the next. Set it with `set_config('app.user_id', ?, true)` rather than `SET LOCAL`, whose third argument is what scopes the value to the transaction. The two are equivalent, but only the function form takes a bind parameter, and the statement that establishes row ownership is the last place to be concatenating values into SQL.

**The application's database role owns nothing and bypasses nothing.** Not owning the tables is what subjects it to the policy, and having no `BYPASSRLS` is what keeps it there. `FORCE ROW LEVEL SECURITY` covers the case that arrangement misses: a table created by the wrong role, which the application would then own and bypass for free. Its cost is that the migration owner is covered too, so a migration touching rows across users takes an explicit `BYPASSRLS` grant, which overrides `FORCE`, rather than relying on ownership.

**A query that forgets its filter cannot widen past its own user.** The policy applies on top of whatever the query asks for, so a missing `user_id` predicate returns the caller's rows, and a lookup by another user's primary key returns nothing. The failure mode becomes an empty result rather than someone else's money.

**Constraints are checked outside the policy.** Referential integrity checks bypass row security by design, so a global `UNIQUE` constraint over user data is a covert channel: a rejected insert tells the caller a value exists without ever showing them the row. Constraints over user data are composite with `user_id`, which matters first for the import fingerprint and external id that transaction deduplication needs.


## Consequences

**This protects users from each other, and from us writing bad queries.** It does not protect them from whoever administers the database. A Postgres superuser, a role with `BYPASSRLS`, a filesystem copy of the data directory, and any backup all read everything. RLS is not a confidentiality boundary against an operator and must never be described as one.

**So the honest claim has a shape.** No user can read another user's data, enforced by the database rather than by discipline. On a self-hosted instance the operator is the user or their household, so that covers the real threat. On the hosted tier the maintainers have technical access to the database, which belongs in a privacy policy stated plainly rather than implied away.

What makes that bearable is that **no product surface reads user financial data.** Billing and platform analytics live outside this codebase entirely, so there is nothing in Fennixs that an operator could use to browse someone's transactions, only a database they would have to query by hand.

**The alternative that would close it was considered and rejected.** Excluding an administrator means the key lives with the client and never reaches the server, whether it is derived from the user's password or held some other way. That is what makes the cost structural rather than incidental: a server that cannot decrypt cannot sum, categorise, deduplicate on import, or search, which is most of what Fennixs does. Not a tradeoff worth making for an application whose purpose is aggregation.

**Every table holding user data needs RLS turned on, and new ones are easy to forget.** The two halves fail in opposite directions. A table with RLS enabled and no matching policy is default-deny, so it fails closed and loudly. A table that never had `ENABLE ROW LEVEL SECURITY` run against it is wide open and says nothing. The omission to defend against is therefore the `ALTER TABLE`, and it has to be enforced somewhere other than memory: a migration convention, or a test that enumerates tables carrying a user column and asserts RLS is enabled on each.

**The session variable is a new failure mode, and a loud one.** Forget to set it and `current_setting` raises rather than returning null, so the request fails instead of rendering an empty screen. That is the behaviour to keep: passing `missing_ok` would convert a missing identity into an empty result, which is still safe but reads as an absence of data rather than as a bug, and so gets diagnosed as the wrong thing. Setting it outside the transaction is the dangerous direction, because it leaks across pooled requests and surfaces as nothing at all. One place sets it, from the identity the edge already resolved.

**Background jobs need it too.** The importer writes on a user's behalf with no request in flight, so it has to set the same variable from the job's own record of whose import it is.
