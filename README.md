# Fennixs

[![License](https://img.shields.io/github/license/RodrigoUma04/fennixs)](LICENSE)

A self-hosted personal finance tracker for individuals who want to own their financial data outright.

> **Status:** Early development. There is no working software yet and nothing here is installable. This repository currently holds project documentation only.


## What Fennixs aims to be

A finance tracker you run yourself, covering accounts, transactions, subscriptions, budgets, and categories, with bank statement import so you are not typing everything in by hand.

Two ideas shape the design:

**Your data stays yours.** Fennixs is built to run on your own hardware. A hosted option is planned for people who would rather not self-host, but self-hosting is the primary path rather than an afterthought.

**The operator cannot read your finances.** Administrative authority and access to financial data are kept apart by design, so whoever runs an instance can manage accounts without being able to see what anyone spent.


## Planned stack

Decided, though none of it is built yet.

| Part | Choice |
| --- | --- |
| Core API | Java, Spring Boot |
| Frontend | Angular |
| Statement importer | Python |
| Database | PostgreSQL |
| Authentication | Keycloak, over OpenID Connect |
| Deployment | Docker Compose for self-hosting |


## Self-hosting

Not possible yet. Installation instructions will be added here with the first release.


## Contributing

Contributions are welcome. See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for branch naming, commit conventions, and the sign-off requirement.

This project follows the [Contributor Covenant](.github/CODE_OF_CONDUCT.md).


## Security

Do not report vulnerabilities through public issues. See [SECURITY.md](.github/SECURITY.md) for how to report one privately.


## License

Fennixs is free software, licensed under the [GNU Affero General Public License v3.0 or later](LICENSE).

Copyright (C) 2026 The Fennixs Authors

See [AUTHORS](AUTHORS) for the list of copyright holders.
