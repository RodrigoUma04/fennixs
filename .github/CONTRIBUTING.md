# Contributing to Fennixs

Thanks for taking an interest in Fennixs. Contributions of all kinds are welcome, whether that is reporting a bug, improving documentation, or writing code.

Fennixs is in early development and the codebase is still taking shape, so please open an issue to discuss anything substantial before writing it. It saves everyone the disappointment of a rejected pull request.


## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). By taking part you agree to uphold it.


## Security Issues

**Do not open a public issue for a security vulnerability.** See [SECURITY.md](SECURITY.md) for how to report one privately.


## Development Setup

Fennixs has no application code yet. Setup instructions will be added here with the first service, along with the versions of Java, Node, Python, and Docker the project targets.

Until then, contributions are limited to documentation and project configuration.


## Branch Naming

Branches follow the [Conventional Branch](https://conventionalbranch.org/) specification: `<type>/<description>`.

| Type | Use for |
| --- | --- |
| `feat/` or `feature/` | A new feature |
| `fix/` or `bugfix/` | A bug fix |
| `hotfix/` | An urgent fix against a release |
| `release/` | Release preparation |
| `chore/` | Tooling, configuration, and everything else |

Descriptions are lowercase, with hyphens between words. No uppercase, spaces, underscores, or consecutive hyphens.

```
feat/add-transaction-import
fix/duplicate-detection-on-reimport
chore/add-base-documentation
```


## Commit Messages

Commits follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

| Type | Use for |
| --- | --- |
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `refactor` | A change that neither fixes a bug nor adds a feature |
| `perf` | A performance improvement |
| `test` | Adding or correcting tests |
| `build` | Build system or dependencies |
| `ci` | CI configuration |
| `chore` | Anything else |

Scope is optional and names the part of the project affected, such as `core-api`,
`importer`, or `frontend`.

```
feat(core-api): add transaction category endpoint
fix(importer): keep identical same-day transactions distinct
docs: add security policy
```

Breaking changes are marked with a `!` before the colon, and explained in a `BREAKING CHANGE:` footer:

```
feat(core-api)!: require an account id on every transaction

BREAKING CHANGE: transactions without an account id are rejected.
```

This matters beyond tidiness. Releases and the changelog are generated from these messages, so `feat` and `fix` decide version numbers and anything mislabelled is invisible in the release notes.

Write the description in the imperative mood, as in "add", not "added" or "adds".


## Signing Your Commits

Every commit must carry a `Signed-off-by` line. This is the [Developer Certificate of Origin](../DCO), and by adding it you certify that you wrote the contribution, or otherwise have the right to submit it under this project's license.

Git adds the line for you with `-s`:

```bash
git commit -s -m "fix(importer): handle empty statement files"
```

which appends:

```
Signed-off-by: Your Name <your.email@example.com>
```

The name and email must match the commit author, so set them before you start:

```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

If you forget, amend the most recent commit with `git commit --amend -s`, or for a whole branch, `git rebase --signoff main`.


## Pull Requests

1. Fork the repository and create a branch from `main`.
2. Make your change, with tests where there is something to test.
3. Sign off every commit.
4. Open a pull request against `main`.

The pull request **title** must also follow Conventional Commits, since it becomes the commit message when the branch is squashed. A description is required: explain what changed and why, and link any related issue.

Expect review comments. They are about the code, not about you.


## License

Fennixs is licensed under the [AGPL-3.0-or-later](../LICENSE). Contributions are accepted under the same license, and you keep the copyright on what you write.
Significant contributors are listed in [AUTHORS](../AUTHORS).


## Questions

Open a [discussion](https://github.com/RodrigoUma04/fennixs/discussions) or an issue.
