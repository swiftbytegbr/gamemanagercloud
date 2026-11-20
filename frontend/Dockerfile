FROM node:26-bookworm-slim AS build

WORKDIR /app
RUN npm install --global pnpm@12.16.1

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

COPY . .

ARG VITE_MODE=dev
# Instance settings, compiled into the web app (see README.md)
# Host of the backend without protocol, e.g. api.example.com (required)
ARG VITE_API_BASE_URL
# Optional links; empty = hidden (Discord, FAQ) or built-in default (docs, daemon download)
ARG VITE_DISCORD_URL=
ARG VITE_FAQ_URL=
ARG VITE_DOCS_URL=
ARG VITE_DAEMON_DOWNLOAD_URL=
RUN test -n "$VITE_API_BASE_URL" || (echo "Build argument VITE_API_BASE_URL is required, e.g. api.example.com" >&2; exit 1)
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm exec tsc -b && pnpm exec vite build --mode ${VITE_MODE}

FROM caddy:2.11.7-alpine
ARG VITE_API_BASE_URL
# Used in the Content-Security-Policy of the Caddyfile
ENV GMC_API_HOST=${VITE_API_BASE_URL}
COPY --from=build /app/dist /srv
COPY Caddyfile /etc/caddy/Caddyfile
