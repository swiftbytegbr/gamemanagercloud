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
