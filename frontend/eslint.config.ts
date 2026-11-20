import { defineConfig } from "vite";
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-expect-error
import eslintConfigPrettier from "eslint-config-prettier";

export default defineConfig({
    plugins: [eslintConfigPrettier],
});
