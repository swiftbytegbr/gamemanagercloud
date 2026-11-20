import axios from "axios";
import { useAuthStore } from "@/store/authStore.ts";
import { toast } from "sonner";
import { router } from "@/Router.tsx";

const API_BASE_DOMAIN = import.meta.env.VITE_API_BASE_URL;
export const API_BASE_URL = `https://${API_BASE_DOMAIN}`;

const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

let lastNetworkToastAt = 0;
let offlineToastShown = false;

if (typeof window !== "undefined") {
    window.addEventListener("online", () => {
        offlineToastShown = false;
        lastNetworkToastAt = 0;
        toast.success("You’re back online.");
    });
    window.addEventListener("offline", () => {
        if (!offlineToastShown) {
            offlineToastShown = true;
            toast.error(
                "You’re offline. To use Game Manager Cloud, please reconnect to the internet.",
            );
        }
    });
}

function toastNetworkErrorThrottled() {
    if (typeof navigator !== "undefined" && !navigator.onLine) {
        if (!offlineToastShown) {
            offlineToastShown = true;
            toast.error(
                "You’re offline. To use Game Manager Cloud, please reconnect to the internet.",
            );
        }
        return;
    }
    const now = Date.now();

    if (now - lastNetworkToastAt < 8000) return;
    lastNetworkToastAt = now;
    toast.error("Network error. Please check your connection.");
}

// 401 errors with these keys should not trigger automatic logout because they
// represent valid intermediate flows (2FA challenges/invalid codes).
const IGNORE_401_LOGOUT_KEYS = new Set([
    "auth.2fa_required",
    "auth.2fa_required_email",
    "auth.2fa_required_totp",
    "auth.invalid_2fa_token",
    "auth.invalid_backup_code",
]);

// 401 errors with these keys should still display their toast message even when
// the user is authenticated (e.g., prompting for 2FA), without logging them out.
const AUTHED_401_TOAST_KEYS = new Set([
    "auth.session_invalid",
    "auth.2fa_required",
    "auth.2fa_required_email",
    "auth.2fa_required_totp",
    "auth.invalid_2fa_token",
    "auth.invalid_backup_code",
]);

axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response) {
            const status = error.response.status;
            const key = error.response.data?.errorKey;
            const isAuthenticated = useAuthStore.getState().isAuthenticated;
            if (
                status != 401 ||
                (status === 401 &&
                    isAuthenticated &&
                    key &&
                    AUTHED_401_TOAST_KEYS.has(key)) ||
                (status === 401 &&
                    !isAuthenticated &&
                    key != "auth.unauthenticated")
            ) {
                if (
                    typeof key === "string" &&
                    key.startsWith("missingPermission.")
                ) {
                    const specific = errorMessages[key];

                    if (specific) {
                        toast.error(String(specific));
                    } else {
                        toast.error(
                            String(errorMessages["missingPermission.generic"]),
                        );
                    }
                } else if (key && key in errorMessages) {
                    const message = errorMessages[key];
                    if (message !== false) toast.error(message as string);
                } else if (status === 400) {
                    toast.error(
                        "The request was invalid: " +
                            error.response.data.message,
                    );
                } else {
                    const errorMessage = errorMessages[`default.${status}`];
                    if (errorMessage !== false)
                        toast.error(errorMessage as string);
                }
            }

            if (
                status === 401 &&
                useAuthStore.getState().isAuthenticated &&
                key != "auth.password_invalid" &&
                !(key && IGNORE_401_LOGOUT_KEYS.has(key))
            ) {
                useAuthStore.getState().logout().then();
                router.navigate({ to: "/login" });
            }
        } else if (error.code === "ERR_NETWORK") {
            toastNetworkErrorThrottled();
        } else {
            toast.error(
                "Oops! An unknown error occurred. Please try again later.",
            );
        }
        return Promise.reject(error);
    },
);

const errorMessages: Record<string, string | boolean> = {
    "default.400": "The request was invalid. (400)",
    "default.401": "Your session has expired. Please log in again.",
    "default.403": "You do not have permission to access this resource. (403)",
    "default.404": false,
    "default.500": "An internal server error occurred. (500)",

    "auth.credentials_invalid": "Your email or password is incorrect.",
    "auth.password_invalid": "The provided password is invalid.",
    "auth.email_already_exists": "An account with this email already exists.",
    "auth.invalid_verification_token":
        "The provided verification code is invalid.",
    "auth.invalid_reset_token": "The provided password reset token is invalid.",
    "auth.send_failed":
        "Failed to send the verification email. Please try again later.",
    "auth.session_invalid": "Your session has expired. Please log in again.",
    "auth.unauthenticated": "Authentication is required. Please log in.",
    "auth.2fa_required":
        "Two‑factor authentication is required. A code was sent to your email.",
    "auth.2fa_required_email": false,
    "auth.2fa_required_totp": false,
    "auth.invalid_2fa_token":
        "The provided two‑factor authentication code is invalid.",
    "auth.invalid_backup_code": "The backup code is invalid or has been used.",
    "auth.invalid_app_authentication":
        "The provided app authentication token is invalid.",

    "auth.session_expired": "Your session has expired. Please log in again.",

    "verification.invalid_token": "The provided verification code is invalid.",

    "user.email_already_exists": "This email address is already in use.",
    "user.email_not_verified_for_2fa":
        "Please verify your email before enabling email 2FA.",
    "user.twofa_already_enabled":
        "Disable your current 2FA method before enabling another.",
    "user.totp_enable_via_verify":
        "Finish the authenticator verification to enable TOTP.",

    "team.name_already_exists": "A team with this name already exists.",
    "team.invitation_invalid": "The provided invitation token is invalid.",
    "team.invitation_expired": "This team invitation has expired.",
    "team.not_found": "The specified team was not found.",
    "team.email_not_verified":
        "Please verify your email to perform this action.",
    "team.too_many_teams":
        "You have reached the maximum number of owned teams.",
    "team.too_many_nodes": "This team has reached the maximum number of nodes.",
    "team.too_many_servers":
        "This team has reached the maximum number of servers.",
    "team.already_member": "You are already a member of this team.",
    "team.not_member": "You are not a member of this team.",
    "team.cannot_leave": "The team owner cannot leave their own team.",
    "team.cannot_change_own_permission":
        "You cannot change your own permissions.",
    "team.deletion_failed_has_nodes":
        "The team cannot be deleted while nodes are registered.",

    "node.invalid_state": "The node is in an invalid state.",
    "node.name_already_exists": "A node with this name already exists.",
    "node.invalid_invite_token":
        "The node registration invite token is invalid.",
    "node.invalid_backup_settings": "The node backup settings are invalid.",

    "server.state_unknown":
        "The server state is unknown. Please check if the node is online.",
    "server.node_is_offline":
        "The selected node is currently offline. Please start it first.",
    "server.is_not_online":
        "The server needs to be in the ONLINE state to perform this action.",
    "server.display_name_already_exists":
        "A server with this name already exists.",
    "server.is_not_offline":
        "The server needs to be in the OFFLINE state to perform this action. This can happen when the server is currently running or the node is offline. Please check the server and node status or force delete the server.",
    "server.backups_empty": "No backups are available for this server.",

    // Forbidden / permissions
    "missingPermission.owner_only":
        "Only the team owner is allowed to perform this action.",
    "missingPermission.generic": "You are not allowed to perform this action.",
    "missingPermission.api_account_not_allowed":
        "API accounts are not allowed to access this resource.",

    // General category
    "general.bad_request": "The request body is not readable.",
    "general.method_not_allowed": "The request method is not supported.",
    "general.unsupported_media_type":
        "The request media type is not supported.",
    "general.server_error": "An unexpected error occurred.",
    "general.not_found": false,

    // Settings
    "settings.ini_too_large":
        "The INI file exceeds the maximum allowed size of 500 KB.",
    "settings.profile_too_large":
        "The setting profile exceeds the maximum allowed size of 1 MB.",
    "settings.copy_targets_contains_source":
        "The destination list must not include the source profile.",
    "settings.teams_not_equal":
        "All target profiles must be in the same team as the source profile.",
};

export default axiosClient;
