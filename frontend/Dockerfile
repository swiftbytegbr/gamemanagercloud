FROM node:26-bookworm-slim AS build

WORKDIR /app
RUN npm install --global pnpm@12.16.1

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

COPY . .

ARG VITE_MODE=dev
ARG VITE_API_BASE_URL
RUN apt-get update
RUN apt-get install ca-certificates -y
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    --mount=type=secret,id=SENTRY_AUTH_TOKEN \
    if [ -f /run/secrets/SENTRY_AUTH_TOKEN ]; then \
        export SENTRY_AUTH_TOKEN="$(cat /run/secrets/SENTRY_AUTH_TOKEN)"; \
    fi && \
    pnpm exec tsc -b && pnpm exec vite build --mode ${VITE_MODE}

FROM caddy:2.11.7-alpine
COPY --from=build /app/dist /srv
COPY Caddyfile /etc/caddy/Caddyfile
