# Accept contributions under the DCO

- **Status:** Accepted
- **Date:** 2026-09-24


## Context

Fennixs has a second contributor and expects contributions from outside the maintainers. Without any agreement in place, each contributor keeps copyright on the code they write, and the project can only ever be relicensed with the permission of every one of them.

Two mechanisms are normally used to address this:

**A Developer Certificate of Origin (DCO)** is a short statement a contributor affirms by adding a `Signed-off-by` line to each commit. It certifies that they have the right to submit the code under the project's licence. It grants the project no additional rights. Its value is provenance: if someone later claims Fennixs contains code they own, there is a record of the contributor asserting otherwise.

**A Contributor Licence Agreement (CLA)** grants the project owner a broad licence over the contribution, typically including the right to relicense. It preserves the option of dual licensing or a closed edition, at the cost of a signing step that deters casual contributors.

The planned hosted offering does not require a CLA. Running our own AGPL code as a paid service is permitted by the licence without any agreement from contributors.

So the question is narrow: do we want to preserve the option of selling proprietary licences or shipping closed features? We do not. Fennixs is intended to stay open.


## Decision

Accept contributions under **Developer Certificate of Origin 1.1**.

Every commit carries a `Signed-off-by` line matching its author, added with `git commit -s`. The certificate text lives in `DCO` at the repository root, and the requirement is documented in `.github/CONTRIBUTING.md`.

No CLA is required.


## Consequences

Contributing stays low friction. There is no agreement to sign and no account to create, which matters for a project that wants drive-by documentation fixes as much as feature work.

Every contributor retains copyright on their own contributions. The `AUTHORS` file names the copyright holders, and the notice `Copyright (C) 2026 The Fennixs Authors` refers to that set rather than to a single entity.

**Relicensing now requires the agreement of every contributor.** Combined with [the AGPL licence choice](0001-use-agpl-3-0-for-licensing.md), this makes the project effectively AGPL permanently. There is no route to a proprietary licence, dual licensing, or a closed enterprise edition containing contributed code.

**This decision is close to one-way.** Reversing it means collecting a CLA from everyone who has already contributed, which gets harder with every merged pull request. It is being made now, while there are two contributors, precisely because that is when it is cheap.

Sign-off is currently unenforced and relies on contributors following CONTRIBUTING. Until the DCO GitHub App or an equivalent check runs on pull requests, the requirement is honour system and unsigned commits can merge. Enforcement should land with the CI workflows.

The maintainers should separately record what they intend regarding a future legal entity or commercial arrangement. The DCO settles what contributors grant the project; it says nothing about what the maintainers have agreed between themselves.
