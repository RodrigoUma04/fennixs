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

- Branch off `main` for every feature or fix
- Open a PR. CI must pass before merging
- No direct pushes to `main`

## Code style

Formatting is handled automatically by Spotless via the pre-commit hook. If you skip the hook, `./mvnw verify` will catch it in CI.