import path from "node:path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { compression } from "vite-plugin-compression2";

const vendorChunks = {
    react: ["react", "react-dom"],
    tanstack: [
        "@tanstack/react-query",
        "@tanstack/react-router",
        "@tanstack/react-table",
    ],
    lucid: ["lucide-react"],
    forms: ["zod", "react-hook-form", "@hookform/resolvers"],
    api: ["axios", "@stomp/stompjs"],
    radix: [
        "@radix-ui/react-checkbox",
        "@radix-ui/react-dialog",
        "@radix-ui/react-dropdown-menu",
        "@radix-ui/react-label",
        "@radix-ui/react-popover",
        "@radix-ui/react-select",
        "@radix-ui/react-separator",
        "@radix-ui/react-slot",
        "@radix-ui/react-tabs",
    ],
    date: ["date-fns"],
    state: ["zustand"],
    cmdk: ["cmdk"],
    theming: ["next-themes"],
    sanitize: ["dompurify"],
    notifications: ["sonner"],
    utility: ["class-variance-authority", "clsx", "tailwind-merge"],
};

const plugins = [react(), tailwindcss(), compression()];

// https://vite.dev/config/
export default defineConfig({
    plugins,
    resolve: {
        alias: {
            "@": path.resolve(import.meta.dirname, "./src"),
        },
    },
    build: {
        rolldownOptions: {
            output: {
                codeSplitting: {
                    groups: Object.entries(vendorChunks).map(
                        ([name, packages]) => ({
                            name,
                            test: (id: string) =>
                                packages.some((pkg) =>
                                    id.includes(`/node_modules/${pkg}/`),
                                ),
                        }),
                    ),
                },
            },
        },
    },
});
