# Contributing

## Prerequisites

- Java 25
- Maven
- Docker
- Pip

## Setup

1. Copy `.env.development.example` to `.env` and set `DB_PASSWORD`.

2. Start the database:
   ```bash
   docker compose -f docker-compose.development.yml up -d
   ```

3. Install the pre-commit hook (requires `pip`):
   ```bash
   pip install pre-commit
   pre-commit install
   ```

## Workflow

- Branch off `main` for every change
- Open a PR. CI must pass before merging
- No direct pushes to `main`

## Branch naming

| Prefix | Use |
|---|---|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `chore/` | Infrastructure, config, tooling, documentation |

Examples: `feature/account-entity`, `fix/flyway-migration-order`, `chore/ci-setup`

## Pull requests

**Title** — use the same prefix as the branch, followed by a short description:

```
feat: add percentage-based budgeting
fix: flyway migration order
chore: set up CI pipeline
```

**Description** — briefly cover:
- What changed and why
- Any decisions or trade-offs worth noting
- How to test it manually (if applicable)

Keep PRs small and focused on a single concern. One feature or fix per PR.

## API development

This project uses contract-first development. All API changes must start with an update to `backend/core-api/src/main/resources/openapi/openapi.yaml`. Do not add endpoints or modify request/response shapes without updating the spec first.

## Code style

Formatting is handled automatically by Spotless via the pre-commit hook. If you skip the hook, `./mvnw verify` will catch it in CI.