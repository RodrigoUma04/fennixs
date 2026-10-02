# Use AGPL-3.0 for licensing

- **Status:** Accepted
- **Date:** 2026-09-24


## Context

Fennixs is a self-hosted personal finance tracker aimed at individuals. A hosted option, run by the maintainers and billed through Stripe, is planned alongside the self-hosted path.

We want the project to be genuinely open source and open to contributions, while preventing someone from taking the code closed.

The licence families differ in exactly that respect:

**Permissive licences** such as MIT and Apache-2.0 allow anyone to take Fennixs, close the source, rebrand it, and sell it, with no obligation to publish anything back.

**GPL-3.0** requires derivatives to stay open, but the obligation is triggered by *distribution*. Someone can fork Fennixs, improve it, run it as a hosted service, and never ship a binary to anyone, so they never have to publish a line of their changes. This is the SaaS loophole.

That loophole matters here more than it would for most projects, because we intend to operate a hosted Fennixs ourselves. Under a permissive or plain GPL licence, a competitor could host a modified Fennixs, compete directly with our own offering, and contribute nothing back.


## Decision

Licence Fennixs under **AGPL-3.0-or-later**.

Section 13 of the AGPL extends the source obligation to network use: anyone who runs a modified version as a network service must offer its source to the users of that service.

Source files carry the SPDX identifier `AGPL-3.0-or-later`.


## Consequences

Anyone hosting a modified Fennixs must publish their modifications, so improvements made by hosted forks come back to the project.

Forking, modifying, and commercial use all remain permitted. The AGPL prevents closing the source, not using the software. This is not a licence that stops people competing with us, only one that stops them doing so from a closed codebase.

We can still run the planned paid hosted offering. The AGPL places no restriction on the copyright holders operating their own software commercially.

**The running application must offer its source to network users.** In practice this means a visible source link in the frontend, pointing at the code the instance is actually running. This is an obligation, not a nicety, and it falls due when the frontend ships.

Some organisations prohibit AGPL software by internal policy. Given the target audience is individuals self-hosting rather than businesses, we accept this cost.

Source files must consistently use `AGPL-3.0-or-later` and never `AGPL-3.0-only`. Mixing the two across a codebase is difficult to untangle later.

Together with [accepting contributions under the DCO](0002-accept-contributions-under-dco.md), which leaves copyright with each contributor, this makes Fennixs effectively AGPL permanently. Relicensing would require the agreement of everyone who has contributed.
