# GMC Backend

Spring Boot backend of GameManagerCloud (GMC). It provides the REST API for the web app in [`../frontend`](../frontend)
and the WebSocket (STOMP) connection for the GMC daemons running on the game server hosts. Data is stored in MongoDB.

## Requirements

- JDK 25
- MongoDB
- `de.swiftbyte.gmc:gmc-common` 3.0.17, resolved from `https://repo.swiftbyte.de/releases` (see `pom.xml`)
- Optional: an SMTP server for e-mails

## Run locally

The default profile is `local` (`application.properties`). It reads the database and SMTP settings from environment
variables:

```bash
export MONGODB_HOST=localhost MONGODB_PORT=27017 MONGODB_DATABASE=gmc-dev
export MONGODB_USERNAME=admin MONGODB_PASSWORD=...
./mvnw spring-boot:run
```

The API is available on `http://localhost:8080`, the OpenAPI UI on `/api-docs/ui`. The `local` profile allows the
frontend dev server (`http://localhost:5173`) and disables e-mail by default.

## Profiles

| Profile | Purpose |
|---|---|
| `local` | Local development (default). Database via `MONGODB_*`, SMTP via `SMTP_*`, e-mail off unless `GMC_MAIL_ENABLED=true` |
| `dev`, `prod` | The former hosted environments (`*.gamemanager.cloud`). Database and SMTP are not configured in the files and come from Spring Boot's standard properties (`spring.mongodb.*`, `spring.mail.*`) |

Select a profile with `SPRING_PROFILES_ACTIVE`, in Docker with `--build-arg SPRING_PROFILE=<profile>`.

## Configuration

| Variable / property | Default | Description |
|---|---|---|
| `MONGODB_HOST`, `MONGODB_PORT`, `MONGODB_DATABASE`, `MONGODB_USERNAME`, `MONGODB_PASSWORD` | `localhost`, `27017`, `gmc-dev`, `admin`, – | Database (`local` profile, authentication database `admin`) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost`, `587`, –, – | SMTP server (`local` profile). The username is also the sender address |
| `GMC_MAIL_ENABLED` | `true` (`false` in `local`) | `false` disables all e-mails, see below |
| `gmc.cors.allowed-origins` | `*` | Comma-separated origins of the web app |
| `gmc.cors.secure-cookies`, `gmc.cors.set-samesite-none` | `true`, `false` | Session cookie flags |
| `gmc.session.cookie-domain` | – | Domain of the session cookie |
| `gmc.helpdesk.email.to` | `contact@swiftbyte.de` | Recipient of bug reports from the web app |
| `SENTRY_DSN` | – | Optional error reporting (`dev`, `prod`) |
| `GMC_LIMIT_*` | see below | Default limits |

Properties can also be set as environment variables using Spring Boot's naming rules, e.g. `GMC_CORS_ALLOWEDORIGINS`
for `gmc.cors.allowed-origins`.

### Without e-mail

With `GMC_MAIL_ENABLED=false` no e-mails are sent and no SMTP server is needed. E-mail addresses are then accepted as
verified on registration and change. Password reset by e-mail, resending verification codes, enabling e-mail 2FA and
bug reports are rejected with an error. Users who already use e-mail 2FA log in with a backup code.

## Default limits

Default limits apply to new teams and users. `0` means unlimited. They are stored in MongoDB (`config` collection,
id `defaultLimits`) and can be changed via the admin API (`/admin/limits`). Environment variables that are set override
the stored value on every start; variables that are not set keep the stored value.

| Variable | Built-in default | Limit |
|---|---|---|
| `GMC_LIMIT_USER_MAX_OWNED_TEAMS` | 0 | Teams a user can own |
| `GMC_LIMIT_TEAM_MAX_MEMBERS` | 0 | Members per team |
| `GMC_LIMIT_TEAM_MAX_NODES` | 0 | Nodes per team |
| `GMC_LIMIT_TEAM_MAX_SERVERS` | 0 | Servers per team |
| `GMC_LIMIT_SERVER_MAX_BACKUPS` | 0 | Backups kept per server (oldest are deleted) |
| `GMC_LIMIT_SERVER_MAX_RCON_COMMANDS` | 0 | Saved RCON commands per server |
| `GMC_LIMIT_SERVER_STATISTICS_MAX_SIZE` | 200 | Statistics entries per server (stored in one document, keep it bounded) |
| `GMC_LIMIT_SERVER_MAX_LOGS` | 0 | Log files per server |

Limits are copied into a team when it is created, so changed defaults only affect new teams. Limits of existing teams
can be changed per team via the admin API.

## Docker

```bash
docker build --build-arg SPRING_PROFILE=prod -t gmc-backend .
docker run -p 8080:8080 gmc-backend
```

The image listens on port 8080.

## API

- OpenAPI: `/api-docs`, UI: `/api-docs/ui`
- WebSockets: `/websocket-nodes` (daemon, headers `Node-Id` and `Node-Secret`), `/websocket-frontend` (web app)
- Further notes in [`docs/`](docs): two-factor authentication, error keys, team notifications, automations, server logs
