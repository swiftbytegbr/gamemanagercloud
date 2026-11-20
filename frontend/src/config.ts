// Instance specific settings, set at build time (see README / Dockerfile build arguments).
// Links that are not configured are hidden.
const env = import.meta.env;

const optional = (value: string | undefined) => {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
};

export const appConfig = {
    /** Invite link of a Discord server, shown in the navigation and on the login page. */
    discordUrl: optional(env.VITE_DISCORD_URL),
    /** FAQ or help page, shown in the navigation. */
    faqUrl: optional(env.VITE_FAQ_URL),
    /** API documentation, linked in the team settings (API keys). Defaults to the OpenAPI UI of the backend. */
    docsUrl:
        optional(env.VITE_DOCS_URL) ??
        `https://${env.VITE_API_BASE_URL}/api-docs/ui`,
    /** Daemon installer offered when adding a node. */
    daemonDownloadUrl:
        optional(env.VITE_DAEMON_DOWNLOAD_URL) ??
        "https://github.com/swiftbytegbr/gmc-daemon/releases/latest/download/gmc-daemon-setup.exe",
};
