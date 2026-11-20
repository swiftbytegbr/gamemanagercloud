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
| `dev`, `prod` | Server deployments (`dev` sends `SameSite=None` cookies). Database and SMTP are not configured in the files and come from Spring Boot's standard properties (`spring.mongodb.*`, `spring.mail.*`, e.g. `SPRING_MONGODB_URI`) |

Select a profile with `SPRING_PROFILES_ACTIVE`, in Docker with `--build-arg SPRING_PROFILE=<profile>`.

## Configuration

| Variable / property | Default | Description |
|---|---|---|
| `MONGODB_HOST`, `MONGODB_PORT`, `MONGODB_DATABASE`, `MONGODB_USERNAME`, `MONGODB_PASSWORD` | `localhost`, `27017`, `gmc-dev`, `admin`, – | Database (`local` profile, authentication database `admin`) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost`, `587`, –, – | SMTP server (`local` profile). The username is also the sender address |
| `GMC_MAIL_ENABLED` | `true` (`false` in `local`) | `false` disables all e-mails, see below |
| `GMC_OPERATOR_NAME` | – | Operator shown in the e-mail footer |
| `GMC_CONTACT_EMAIL` | – | Contact address in the e-mail footer and in security e-mails ("contact us at ..."); without it the e-mails refer to the administrator of the instance |
| `GMC_HELPDESK_EMAIL` | – | Recipient of bug reports from the web app; empty disables bug reports |
| `GMC_CORS_ALLOWED_ORIGINS` | `*` (`local`: `http://localhost:5173,http://127.0.0.1:5173`) | Comma-separated origins of the web app, e.g. `https://gmc.example.com` |
| `GMC_COOKIE_DOMAIN` | – (`local`: `localhost`) | Domain of the session cookie |
| `GMC_TRUSTED_PROXIES` | `10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8,::1/128,fc00::/7` | Comma-separated IPs or CIDR ranges of your reverse proxies, see below |
| `GMC_CLIENT_IP_HEADER` | `X-Forwarded-For` | Header with the client IP; `CF-Connecting-IP` behind Cloudflare |
| `GMC_APPLICATIONS_EMAIL_DOMAIN` | `applications.invalid` | Domain of the generated logins of API applications (`<application>@<team>.<domain>`); these addresses never receive e-mail |
| `gmc.cors.secure-cookies`, `gmc.cors.set-samesite-none` | `true`, `false` | Session cookie flags |
| `SENTRY_DSN` | – | Optional error reporting (`dev`, `prod`) |
| `GMC_LIMIT_*` | see below | Default limits |

Properties can also be set as environment variables using Spring Boot's naming rules, e.g. `GMC_CORS_ALLOWEDORIGINS`
for `gmc.cors.allowed-origins`.

### Client IP

The client IP is used for sessions, login notifications and node connections. The header from `GMC_CLIENT_IP_HEADER`
is only used if the request comes from an address in `GMC_TRUSTED_PROXIES`; otherwise the address of the connection is
used, so clients cannot fake their IP. With `X-Forwarded-For` the list is read from the right and the first address
that is not a trusted proxy is used. Set `GMC_TRUSTED_PROXIES` to the addresses of your proxies, or to an empty value if
the backend is reached directly.

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
