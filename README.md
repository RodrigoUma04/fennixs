# Fennixs

A self-hosted personal finance tracker. Track accounts, transactions, budgets, and categories — with a clean UI and full data ownership.

> **Status:** Early development. Not ready for production use.

## Features

- Account and transaction management
- Budget tracking and categories
- Self-hosted with a single `docker compose up -d`
- Multi-user support with registration control

## Self-hosting

**Requirements:** Docker and Docker Compose.

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set your values:
   ```env
   DB_NAME=fennixsdb
   DB_USERNAME=fennixs
   DB_PASSWORD=your-strong-password
   ```

3. Start the stack:
   ```bash
   docker compose up -d
   ```

The API will be available at `http://localhost:8080`.

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_NAME` | yes | — | PostgreSQL database name |
| `DB_USERNAME` | yes | — | PostgreSQL username |
| `DB_PASSWORD` | yes | — | PostgreSQL password |
| `BACKEND_PORT` | no | `8080` | Port exposed on the host |
| `ALLOW_REGISTRATION` | no | `false` | Allow new user sign-ups |
| `TIMEZONE` | no | `UTC` | App timezone (e.g. `Europe/Brussels`) |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | no | — | Set to `logstash`, `ecs`, or `gelf` for structured logs |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). This project follows a [Code of Conduct](CODE_OF_CONDUCT.md).

## License

[GNU Affero General Public License v3.0](LICENSE)