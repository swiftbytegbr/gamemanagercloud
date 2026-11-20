/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string;
    readonly VITE_DISCORD_URL?: string;
    readonly VITE_FAQ_URL?: string;
    readonly VITE_DOCS_URL?: string;
    readonly VITE_DAEMON_DOWNLOAD_URL?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
