# Architecture Decision Records

Each file here records one decision: what we chose, why, and what it commits us to.

Records use [Michael Nygard's format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions): Context, Decision, Status, Consequences.

Two rules keep them useful:

**One decision per record.** If a file covers two choices, split it.

**Records are immutable once accepted.** When a decision changes, write a new record that supersedes the old one and mark the old one `Superseded by <file>`. The history is the point. A record you rewrite is just documentation.

A decision earns a record when someone could reasonably look at the result and want to undo it without knowing why it is there. Choices with no interesting tradeoff do not need one.

Write the record when a decision takes effect, not when it is first discussed.

Files are named `NNNN-imperative-description.md`, numbered in the order they were accepted, so the directory listing reads as a timeline. Refer to a record by its number, as in ADR-0003.
