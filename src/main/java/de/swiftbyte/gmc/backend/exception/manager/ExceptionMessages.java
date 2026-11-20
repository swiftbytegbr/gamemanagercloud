package de.swiftbyte.gmc.backend.exception.manager;

import java.util.Map;

public class ExceptionMessages {
    public static final Map<String, String> ERROR_EXPLANATIONS = Map.ofEntries(
            // General
            Map.entry("general.bad_request", "The request body is not readable."),
            Map.entry("general.method_not_allowed", "The request method is not supported."),
            Map.entry("general.unsupported_media_type", "The request media type is not supported."),
            Map.entry("general.server_error", "An unexpected error occurred."),
            Map.entry("general.not_found", "The requested resource was not found."),
            Map.entry("validation.failed", "The request contains invalid or missing fields."),
            // Common field-level validation keys (prefixed variants)
            Map.entry("validation.invalid_vat_id", "Invalid VAT number."),
            Map.entry("validation.invalid_country_code", "Invalid country code."),
            Map.entry("validation.invalid_postal_code", "Invalid postal/ZIP code."),
            Map.entry("validation.invalid_city", "Invalid city name."),
            Map.entry("validation.invalid_region", "Invalid region/state name."),
            Map.entry("validation.invalid_street", "Invalid street address."),
            Map.entry("validation.address_required", "A complete billing address is required."),
            Map.entry("validation.company_country_mismatch", "Company country must match billing address country."),
            Map.entry("validation.invalid_id", "Invalid identifier format."),
            Map.entry("validation.invalid_name", "Invalid name format."),
            // Backwards-compatibility (non-prefixed)
            Map.entry("invalid_vat_id", "Invalid VAT number."),
            Map.entry("invalid_country_code", "Invalid country code."),
            Map.entry("invalid_postal_code", "Invalid postal/ZIP code."),
            Map.entry("invalid_city", "Invalid city name."),
            Map.entry("invalid_region", "Invalid region/state name."),
            Map.entry("invalid_street", "Invalid street address."),
            Map.entry("address_required", "A complete billing address is required."),
            Map.entry("company_country_mismatch", "Company country must match billing address country."),
            Map.entry("invalid_id", "Invalid identifier format."),
            Map.entry("invalid_name", "Invalid name format."),
            Map.entry("invalid_rcon_command", "Invalid RCON command."),
            Map.entry("xlsx.parse_exception", "The uploaded file is not in a valid format. An error may have occurred in the file."),
            // XLSX field-level violation explanations
            Map.entry("invalid_range", "Invalid range: rangeFrom must be less than or equal to rangeTo."),
            Map.entry("range_unsupported_for_type", "rangeFrom and rangeTo are only supported when Type is INTEGER or FLOAT."),
            Map.entry("range_to_lt_from", "rangeTo must not be less than rangeFrom."),
            Map.entry("appearance_mismatch", "The chosen Appearance is incompatible with the declared Type."),
            Map.entry("must_be_empty_or_subtitle", "Value must be empty or the literal placeholder 'SUBTITLE' (case-insensitive)."),
            // Settings
            Map.entry("settings.ini_too_large", "The INI file exceeds the maximum allowed size of 5 MB."),
            Map.entry("settings.profile_too_large", "The setting profile exceeds the maximum allowed size of 5 MB."),
            Map.entry("settings.copy_targets_contains_source", "The destination list must not include the source profile."),
            Map.entry("settings.teams_not_equal", "All target profiles must be in the same team as the source profile."),

            // Authentication
            Map.entry("auth.credentials_invalid", "Invalid login credentials (wrong email or password)."),
            Map.entry("credentials_invalid", "Invalid login credentials (wrong email or password)."),
            Map.entry("auth.email_already_exists", "Attempt to register or change to an email that is already in use."),
            Map.entry("email_already_exists", "Attempt to register or change to an email that is already in use."),
            Map.entry("auth.invalid_verification_token", "Email verification token is invalid."),
            Map.entry("invalid_token", "The provided verification token is invalid."),
            Map.entry("auth.invalid_reset_token", "Password reset token is invalid."),
            Map.entry("invalid_reset_token", "Password reset token is invalid."),
            Map.entry("auth.send_failed", "Sending an email (verification, reset, etc.) failed."),
            Map.entry("auth.session_invalid", "The session is invalid or has expired."),
            Map.entry("session_invalid", "The session is invalid or has expired."),
            // Explicit auth-prefixed alias to avoid missing explanation warnings
            Map.entry("auth.session_expired", "The session is invalid or has expired."),
            Map.entry("auth.unauthenticated", "Authentication is required to access this resource."),
            Map.entry("auth.2fa_required", "Two-factor authentication is required to complete this action."),
            Map.entry("2fa_required", "Two-factor authentication is required to complete this action."),
            Map.entry("auth.2fa_required_totp", "Two-factor required: enter your TOTP code or a backup code."),
            Map.entry("auth.2fa_required_email", "Two-factor required: enter the code sent by email or a backup code. A new email has been sent."),
            Map.entry("auth.invalid_backup_code", "The provided backup code is invalid."),
            Map.entry("invalid_backup_code", "The provided backup code is invalid."),
            Map.entry("auth.invalid_2fa_token", "The provided two-factor authentication token is invalid."),
            Map.entry("invalid_2fa_token", "The provided two-factor authentication token is invalid."),
            Map.entry("auth.invalid_app_auth", "Invalid application authentication. Please check your credentials."),
            Map.entry("auth.invalid_node_auth", "Invalid node authentication. Please check your credentials."),
            Map.entry("invalid_app_auth", "Invalid application authentication. Please check your credentials."),
            Map.entry("invalid_node_auth", "Invalid node authentication. Please check your credentials."),
            Map.entry("password_invalid", "The current password is invalid."),
            Map.entry("auth.user_disabled", "This account is disabled by an administrator."),
            Map.entry("session_expired", "The session is invalid or has expired."),
            Map.entry("email_already_verified", "Email is already verified."),

            // User
            Map.entry("user.email_already_exists", "Attempt to change to an email that is already in use."),
            Map.entry("user.email_not_verified_for_2fa", "Email must be verified before enabling email 2FA."),
            Map.entry("user.totp_enable_via_verify", "To enable TOTP 2FA, complete setup and verify using the dedicated endpoints."),
            Map.entry("user.twofa_already_enabled", "Two-factor authentication is already enabled. Disable it before enabling another method."),
            Map.entry("user.twofa_not_email", "Two-factor authentication type is not EMAIL."),
            Map.entry("twofa_already_enabled", "Two-factor authentication is already enabled. Disable it before enabling another method."),
            Map.entry("twofa_not_email", "Two-factor authentication type is not EMAIL."),

            // Node
            Map.entry("node.name_already_exists", "Node with the same name already exists in the team."),
            Map.entry("node.invalid_invite_token", "The invite token for node registration is invalid."),
            Map.entry("node.invalid_state", "Node is not in the correct state for the requested operation."),
            Map.entry("node.invalid_backup_settings", "Node backup settings are invalid."),
            // Unprefixed node keys intentionally kept only for non-exception usage
            Map.entry("node_id_missing", "Node identifier is missing."),
            Map.entry("node.task_not_cancelable", "The specified node task cannot be canceled."),
            Map.entry("node.task_id_already_exists", "A node task with this ID already exists."),
            Map.entry("node.task_node_mismatch", "Cannot update task: it belongs to a different node."),
            Map.entry("node.backup_directory_change_in_progress", "Server backup directory change prevents node settings from being updated"),

            // Server
            Map.entry("server.node_is_offline", "Attempt to create a server on an offline node."),
            Map.entry("server.display_name_already_exists", "Server display name already exists in the team."),
            Map.entry("server.unsupported_game_type", "Unsupported game type for this operation."),
            Map.entry("server.is_not_offline", "Operation requires the server to be offline, but it is not."),
            Map.entry("server.is_not_online", "Operation requires the server to be online, but it is not."),
            Map.entry("server.dto_mapping_profile_not_found", "Server setting profile not found during DTO mapping."),
            Map.entry("server.rcon_timeout", "Timed out waiting for RCON response."),
            Map.entry("server.backup_directory_change_in_progress", "Cannot perform backup operations while the node is changing the backups directory. Please wait until the task completes."),
            // Directory handling
            Map.entry("server.directory_required", "A server directory is required but not configured on the node and was not provided."),
            Map.entry("server.invalid_directory", "The provided server directory path is invalid."),
            // Aliases for legacy/unprefixed keys
            Map.entry("server_directory_required", "A server directory is required but not configured on the node and was not provided."),
            Map.entry("invalid_directory", "The provided server directory path is invalid."),
            // Stop-specific messages (unique to stop action).
            Map.entry("server.stop_normal_invalid_state", "Cannot stop gracefully: server must be ONLINE. Try again when ONLINE or use Force Stop if unresponsive."),
            Map.entry("server.stop_force_already_offline", "Cannot force stop: server is already OFFLINE."),
            Map.entry("server.timed_shutdown_already_scheduled", "A timed shutdown is already scheduled for this server on this node."),
            Map.entry("server.timed_restart_already_scheduled", "A timed restart is already scheduled for this server on this node."),
            Map.entry("server.state_unknown", "Server is in an unknown state."),
            Map.entry("server.statistics_not_found", "Statistics not found for the server."),
            // Backward compatibility alias (typo in some places)
            Map.entry("server.statics_not_found", "Statistics not found for the server."),
            Map.entry("server.backups_empty", "No backups found for the server."),

            // Validation (field-level)
            Map.entry("validation.invalid_directory", "Invalid server directory path."),

            // Team
            Map.entry("team.not_found", "The specified team was not found."),
            Map.entry("team.name_already_exists", "Team name already exists."),
            Map.entry("team.email_not_verified", "User's email is not verified for team operations."),
            Map.entry("team.too_many_teams", "User has reached the maximum number of owned teams."),
            Map.entry("team.too_many_nodes", "Team has reached the maximum number of nodes."),
            Map.entry("team.too_many_servers", "Team has reached the maximum number of servers."),
            Map.entry("team.too_many_members", "Team has reached the maximum number of members."),
            Map.entry("team.invitation_invalid", "Team invitation is invalid or missing."),
            Map.entry("team.invitation_expired", "Team invitation has expired (older than 14 days)."),
            Map.entry("team.already_member", "User is already a member of the team."),
            Map.entry("team.not_member", "User is not a member of the team."),
            Map.entry("team.cannot_leave", "Owner cannot leave their own team."),
            Map.entry("team.cannot_change_own_permissions", "User cannot change their own permissions."),
            Map.entry("team.deletion_failed_has_nodes", "Team cannot be deleted while nodes are registered."),
            // Application (team-scoped)
            Map.entry("team.application_already_exists", "An application user with this email already exists."),
            Map.entry("team.application_not_found", "Application user not found."),

            // Verification
            Map.entry("verification.invalid_token", "The provided verification token is invalid."),

            // Forbidden
            Map.entry("missingPermission.admin_only", "Only GMC administrators are allowed to perform this action."),
            Map.entry("missingPermission.admin_twofa_required", "Admin access requires 2FA to be enabled on your account."),
            Map.entry("missingPermission.owner_only", "Only the team owner is allowed to perform this action."),
            Map.entry("missingPermission.generic", "You are not allowed to perform this action. Either you are not a member of the team or you do not have the required permissions."),
            Map.entry("missingPermission.api_account_not_allowed", "API accounts are not allowed to access this resource."),
            // Aliases for forbidden keys used directly
            Map.entry("admin_only", "Only GMC administrators are allowed to perform this action."),
            Map.entry("owner_only", "Only the team owner is allowed to perform this action."),
            Map.entry("api_account_not_allowed", "API accounts are not allowed to access this resource."),

            // Settings aliases
            Map.entry("ini_empty", "The INI content must not be empty."),
            Map.entry("ini_too_large", "The INI file exceeds the maximum allowed size of 5 MB."),
            Map.entry("profile_too_large", "The setting profile exceeds the maximum allowed size of 5 MB."),
            Map.entry("teams_not_equal", "All target profiles must be in the same team as the source profile."),
            Map.entry("game_types_not_equal", "All target profiles must be of the same game type as the source profile."),
            Map.entry("copy_not_supported", "Copying this profile type is not supported."),

            // Helpdesk
            Map.entry("helpdesk_webhook_not_configured", "Helpdesk webhook is not configured."),
            Map.entry("helpdesk_webhook_request_failed", "Failed to send helpdesk request. Please try again later."),
            // Automations
            Map.entry("automation.run_not_deletable", "This automation run is not in a terminal state and cannot be deleted."),
            Map.entry("automation.targets_invalid_team", "One or more explicit target servers are not in the automation's team."),
            Map.entry("automation.trigger_server_ids_invalid_team", "One or more server IDs in the state-change trigger do not belong to the automation's team."),
            Map.entry("automation.targets_trigger_source_only_for_state_change", "The TRIGGER_SOURCE target selector can only be used with a STATE_CHANGE trigger."),
            Map.entry("automation.targets_trigger_source_manual_forbidden", "Automations using the TRIGGER_SOURCE target selector cannot be triggered manually."),
            Map.entry("automation.expression_error", "Failed to render a template expression. Please check syntax and referenced symbols."),
            Map.entry("automation.unknown", "An unknown automation error occurred."),

            // Admin / destructive confirmations
            Map.entry("delete_confirmation_required", "Please type 'delete' to confirm this destructive action."),

            // User deletion constraints
            Map.entry("user.deletion_failed_has_teams", "The user owns one or more teams. Delete or transfer those teams first.")
            // Add more specific missingPermission or automation keys as needed
        );
}
