# GMC Frontend Rewrite

## Techstack

- Vite
- React
- tailwindcss + shadcn/ui
- TanStack Router
- zustand
- zod

## Dev build

Use Node.js 26 LTS and pnpm 12.16.1 (see `packageManager` in `package.json`).
TypeScript stays on 6.0.3 until `typescript-eslint` supports TypeScript 7.

Install dependencies:

```shell
pnpm install
```

Run:

```shell
pnpm run dev
```

## Prod build:

```shell
pnpm run build
```

## Configuration

Settings are compiled into the web app at build time (Vite `VITE_*` variables, Docker build arguments):

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | – (required) | Backend without protocol: host (`api.example.com`) or host and path (`gmc.example.com/api`, as in `docker-compose.yml`); `https://` and `wss://` are added |
| `VITE_DISCORD_URL` | – (hidden) | Discord invite shown in the navigation and on the login page |
| `VITE_FAQ_URL` | – (hidden) | FAQ or help page shown in the navigation |
| `VITE_DOCS_URL` | `https://<VITE_API_BASE_URL>/api-docs/ui` | API documentation linked in the team settings |
| `VITE_DAEMON_DOWNLOAD_URL` | latest release of `swiftbytegbr/gmc-daemon` | Daemon installer offered when adding a node |

```shell
docker build --build-arg VITE_API_BASE_URL=api.example.com -t gmc-frontend .
```

The image serves the app with Caddy on port 80. The Content-Security-Policy allows connections to `VITE_API_BASE_URL`.

## Legal pages

`public/imprint.html`, `public/privacy-policy.html` and `public/terms-of-service.html` are placeholders. Replace their
content with your own texts (HTML fragments) and rebuild the frontend.
