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
   DB_USERNAME=fennixs
   DB_PASSWORD=your-strong-password
   JWT_SECRET=a-long-random-secret
   ```

   Generate a strong `JWT_SECRET`, for example with `openssl rand -base64 48`.

3. Start the stack:

   ```bash
   docker compose up -d
   ```

4. Create the owner account. On first start with an empty database, the auth service prints a one-time **setup token** to its logs. Retrieve it:

   ```bash
   docker compose logs auth | grep "Setup token"
   ```

   Then register the first user through the app, entering this token when prompted. That first account becomes the instance owner. The token is single-use, and registration stays closed afterward unless you set `ALLOW_REGISTRATION=true`.

| Service | Default URL           |
| ------- | --------------------- |
| Gateway | `http://localhost:80` |

All requests go through the gateway:

- `/auth/*` — authentication endpoints
- `/api/*` — core API endpoints

### Environment variables

| Variable                            | Required | Default | Description                                                                                 |
| ----------------------------------- | -------- | ------- | ------------------------------------------------------------------------------------------- |
| `DB_USERNAME`                       | yes      | —       | PostgreSQL username                                                                         |
| `DB_PASSWORD`                       | yes      | —       | PostgreSQL password                                                                         |
| `JWT_SECRET`                        | yes      | —       | Random string of at least 32 characters, used to sign tokens. App will not start without it |
| `GATEWAY_PORT`                      | no       | `80`    | Host port for the gateway                                                                   |
| `COOKIE_SECURE`                     | no       | `false` | Set to `true` when serving over HTTPS                                                       |
| `ALLOW_REGISTRATION`                | no       | `false` | Allow new user sign-ups                                                                     |
| `TIMEZONE`                          | no       | `UTC`   | App timezone (e.g. `Europe/Brussels`)                                                       |
| `HEALTH_SHOW_DETAILS`               | no       | `never` | Set to `always` to expose full health details                                               |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | no       | —       | Set to `logstash`, `ecs`, or `gelf` for structured logs                                     |
| `AUTH_MEMORY_LIMIT`                 | no       | `512M`  | Memory cap for the auth service                                                             |
| `API_MEMORY_LIMIT`                  | no       | `512M`  | Memory cap for the core API service                                                         |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). This project follows a [Code of Conduct](CODE_OF_CONDUCT.md).

## License

[GNU Affero General Public License v3.0](LICENSE)
