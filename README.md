# GameManagerCloud

GameManagerCloud (GMC) manages game servers on your own machines. The web app and the backend run on a server; the
GMC daemon runs on each game server host (node) and connects to the backend.

| Folder | Content |
|---|---|
| [`frontend/`](frontend) | Web app (Vite, React), served by Caddy |
| [`backend/`](backend) | Backend (Spring Boot, MongoDB): REST API and WebSockets for web app and daemons |
| [`docker-compose.yml`](docker-compose.yml) | Deployment: web app, backend and MongoDB behind Traefik |
| [`.example.env`](.example.env) | Configuration template |

## Deployment with Docker Compose

### How it works

All of GMC runs on one domain (`GMC_DOMAIN`):

```
Browser / daemon ──HTTPS──> Traefik ──┬── https://<GMC_DOMAIN>/      ──> frontend (Caddy, port 80)
                                      └── https://<GMC_DOMAIN>/api/… ──> backend (port 8080, /api is removed)
                                                                           └── db (MongoDB, internal network only)
```

- The web app is built with `VITE_API_BASE_URL=<GMC_DOMAIN>/api` and calls `https://<GMC_DOMAIN>/api/…` and
  `wss://<GMC_DOMAIN>/api/websocket-frontend`.
- The WebSocket endpoint for daemons is `wss://<GMC_DOMAIN>/api/websocket-nodes`.
- No ports are published; everything is reached through Traefik. MongoDB is only in the internal network.
- The images are built locally from this repository (`docker compose up --build`). Settings of the web app are compiled
  into it, so changes to them need a rebuild.

### Requirements

- Docker with the Compose plugin
- A running Traefik that uses the Docker provider and is attached to a Docker network named `proxy`
- HTTPS for the domain. The routers use the entrypoint `web`; adjust the labels in `docker-compose.yml` to your
  entrypoints and TLS setup. HTTPS is required: the backend sets `Secure` session cookies, and the web app always uses
  `https://` and `wss://`.
- A DNS record for the domain pointing to the server
- Optional: an SMTP server for e-mails

### Installation

```bash
git clone https://github.com/swiftbytegbr/gamemanagercloud.git
cd gamemanagercloud
cp .example.env .env
# edit .env: at least GMC_DOMAIN and SPRING_MONGODB_PASSWORD, and SMTP or GMC_MAIL_ENABLED=false
docker compose up -d --build
```

Replace the legal pages before going public (see [Legal pages](#legal-pages)). Check the startup with
`docker compose logs -f backend`; the backend logs the active default limits.

### Admin account

There is no setup step that creates an admin. Register a normal account in the web app, then set its account type in
the database:

```bash
docker compose exec db sh -c 'mongosh -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin "$MONGO_INITDB_DATABASE"'
```

```js
db.users.updateOne({ email: "you@example.com" }, { $set: { accountType: "ADMIN" } })
```

Admins can use the admin API under `/api/admin/…` (e.g. default limits under `/api/admin/limits`).

### Update

```bash
git pull
docker compose up -d --build
```

### Backup

The data is in the volume `db-data`. A dump of the database:

```bash
docker compose exec -T db sh -c 'mongodump -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --db "$MONGO_INITDB_DATABASE" --archive' > gmc-backup.archive
```

Restore with `mongorestore --archive` in the same way (`docker compose exec -T db sh -c 'mongorestore … --archive' < gmc-backup.archive`).

## Configuration

All settings are in `.env` (template: [`.example.env`](.example.env)). Values marked "rebuild" are compiled into the
web app and need `docker compose up -d --build`; for all others `docker compose up -d` is enough.

### Required

| Variable | Description |
|---|---|
| `GMC_DOMAIN` | Domain of the instance, e.g. `gmc.example.com` (rebuild) |
| `SPRING_MONGODB_DATABASE` | Database name |
| `SPRING_MONGODB_USERNAME` | Database user (created as MongoDB root user on the first start) |
| `SPRING_MONGODB_PASSWORD` | Database password |

The MongoDB user is only created when the volume is empty. Changing the credentials later does not change the user in
the database.

### E-mail

| Variable | Default | Description |
|---|---|---|
| `GMC_MAIL_ENABLED` | `true` | `false` disables all e-mails; no SMTP server is needed then |
| `SPRING_MAIL_HOST` | `localhost` | SMTP server |
| `SPRING_MAIL_PORT` | `587` | SMTP port |
| `SPRING_MAIL_USERNAME` | – | SMTP user, also the sender address |
| `SPRING_MAIL_PASSWORD` | – | SMTP password |
| `SPRING_MAIL_SMTP_AUTH` | `true` | SMTP authentication |
| `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | `true` | STARTTLS |

Without e-mail, addresses are accepted as verified on registration. Password reset by e-mail, e-mail 2FA and bug reports
are not available; users who already use e-mail 2FA log in with a backup code.

### Instance

| Variable | Default | Description |
|---|---|---|
| `GMC_OPERATOR_NAME` | – | Operator shown in the e-mail footer |
| `GMC_CONTACT_EMAIL` | – | Contact address in the e-mail footer and in security e-mails; without it they refer to the administrator of the instance |
| `GMC_HELPDESK_EMAIL` | – | Recipient of bug reports from the web app; empty disables bug reports |
| `GMC_DISCORD_URL` | – | Discord invite in the web app; empty hides it (rebuild) |
| `GMC_FAQ_URL` | – | FAQ link in the web app; empty hides it (rebuild) |
| `GMC_DOCS_URL` | `https://<GMC_DOMAIN>/api/api-docs/ui` | API documentation linked in the team settings (rebuild) |
| `GMC_DAEMON_DOWNLOAD_URL` | latest release of `swiftbytegbr/gmc-daemon` | Daemon installer offered when adding a node (rebuild) |
| `GMC_APPLICATIONS_EMAIL_DOMAIN` | `applications.invalid` | Domain of the generated logins of API applications; never receives e-mail |

### Network and security

| Variable | Default | Description |
|---|---|---|
| `GMC_TRUSTED_PROXIES` | `10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8,::1/128,fc00::/7` | IPs or CIDR ranges whose client IP header is trusted. The default covers Traefik in a Docker network |
| `GMC_CLIENT_IP_HEADER` | `X-Forwarded-For` | Header with the client IP; `CF-Connecting-IP` behind Cloudflare |
| `GMC_CORS_ALLOWED_ORIGINS` | `https://<GMC_DOMAIN>` | Comma-separated origins allowed to call the API |
| `GMC_COOKIE_DOMAIN` | – | Domain of the session cookie; empty = the host of the request |

The client IP is used for sessions, login notifications and node connections. The header is only read for requests
from a trusted proxy; otherwise the address of the connection is used.

### Default limits

Limits for new teams and users; `0` means unlimited. Unset variables keep the value stored in the database, which can
also be changed via the admin API. Limits are copied into a team when it is created.

| Variable | Built-in default | Limit |
|---|---|---|
| `GMC_LIMIT_USER_MAX_OWNED_TEAMS` | 0 | Teams a user can own |
| `GMC_LIMIT_TEAM_MAX_MEMBERS` | 0 | Members per team |
| `GMC_LIMIT_TEAM_MAX_NODES` | 0 | Nodes per team |
| `GMC_LIMIT_TEAM_MAX_SERVERS` | 0 | Servers per team |
| `GMC_LIMIT_SERVER_MAX_BACKUPS` | 0 | Backups kept per server |
| `GMC_LIMIT_SERVER_MAX_RCON_COMMANDS` | 0 | Saved RCON commands per server |
| `GMC_LIMIT_SERVER_STATISTICS_MAX_SIZE` | 200 | Statistics entries per server |
| `GMC_LIMIT_SERVER_MAX_LOGS` | 0 | Log files per server |

### Other

| Variable | Default | Description |
|---|---|---|
| `SENTRY_DSN` | – | Optional error reporting of the backend |

## Legal pages

`frontend/public/imprint.html`, `frontend/public/privacy-policy.html` and `frontend/public/terms-of-service.html` are
placeholders. Replace their content with your own texts (HTML fragments without `<html>`/`<body>`) and rebuild:
`docker compose up -d --build`.

## Development

See [`backend/README.md`](backend/README.md) and [`frontend/README.md`](frontend/README.md).
