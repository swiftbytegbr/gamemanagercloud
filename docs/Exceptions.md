# Exception Keys & Validation Reference

This document lists all exception keys used in the backend and explains how request validation works. Explanations are
returned in error responses and are managed centrally in `ExceptionMessages`.

Questions about quotas/limits or changing capacities? Please contact GMC Support.

---

## Validation System

The backend uses Jakarta Bean Validation (JSR-380) with Spring:

- Controllers are annotated with `@Validated` to enable parameter validation (path/query params).
- Request bodies are annotated with `@Valid` to validate nested DTOs.
- Custom reusable annotations reduce duplication:
    - `@Identifier`: For IDs. `NotBlank + Size(1,64) + Pattern(^[A-Za-z0-9_-]+$)`.
    - `@LongIdentifier`: For rare long identifiers. `NotBlank + Size(1,128) + Pattern(^[A-Za-z0-9._-]+$)`.
    - `@SafeName`: For human-readable names. `NotBlank + Size(1,64) + Pattern(^[\p{L}\p{N} _.-]+$)`.

Current usage:

- Server, team, node, profile, and backup IDs all use `@Identifier` (≤64 chars). Backup IDs are generated as
  `gmc-back-<uuid>` (~45 chars), so they fit within `@Identifier`.

Error handling for validation:

- Field/object validation errors return HTTP 400 with:
    - `errorKey`: `validation.failed`
    - `message`: concatenated list of invalid fields with reasons
- Path/query parameter violations return HTTP 400 with the same `validation.failed` key.

Response structure (enriched):

```
{
  "errorKey": "validation.failed",
  "message": "personalInformation.name: invalid_name; companyInformation: company_country_mismatch",
  "status": 400,
  "path": "/user/@me/company-info",
  "method": "POST",
  "timestamp": "2025-09-14T12:34:56.789Z",
  "violations": [
    { "type": "field",   "field": "personalInformation.name",  "message": "invalid_name", "code": "SafeName", "rejectedValue": "..." },
    { "type": "object",  "field": "UpdateCompanyInfoRequest",  "message": "company_country_mismatch", "code": "CountryCodeMatch" },
    { "type": "parameter", "field": "page", "message": "type_mismatch", "code": "type_mismatch", "rejectedValue": "abc" }
  ]
}
```

Additional validation keys used in messages:

- `invalid_id`: Invalid identifier format.
- `invalid_name`: Invalid name format.
- `invalid_rcon_command`: Invalid RCON command.
- `address_required`: A complete billing address is required.
- `company_country_mismatch`: Company country must match billing address country.

---

## Permissions

The following permissions are used in the backend and may appear in error keys such as `missingPermission.{permission}`:

- MANAGE_TEAM
- ACCESS_NODES
- MANAGE_NODES
- MANAGE_SERVERS
- ACCESS_SERVERS
- ACCESS_PROFILES
- MANAGE_PROFILES
- VIEW_PROFILES

---

## Authentication (`auth.`)

- `auth.credentials_invalid`: Invalid login credentials (wrong email or password).
- `auth.email_already_exists`: Attempt to register or change to an email that is already in use.
- `auth.invalid_verification_token`: Email verification token is invalid.
- `auth.invalid_reset_token`: Password reset token is invalid.
- `auth.send_failed`: Sending an email (verification, reset, etc.) failed.
- `auth.session_invalid`: The session is invalid or has expired.
- `auth.unauthenticated`: Authentication is required to access this resource.
- `auth.2fa_required`: Two-factor authentication is required to complete this action.
- `auth.invalid_2fa_token`: The provided two-factor authentication token is invalid.
- `auth.invalid_app_auth`: Invalid application authentication. Please check your credentials.
- `auth.invalid_node_auth`: Invalid node authentication. Please check your credentials.

Aliases supported (same meaning): `credentials_invalid`, `email_already_exists`, `invalid_token`, `invalid_reset_token`,
`session_invalid`, `2fa_required`, `invalid_2fa_token`, `invalid_app_auth`, `invalid_node_auth`,
`email_already_verified`, `password_invalid`, `session_expired`.

## User (`user.`)

- `user.email_already_exists`: Attempt to change to an email that is already in use.

## Node (`node.`)

- `node.name_already_exists`: Node with the same name already exists in the team.
- `node.invalid_invite_token`: The invite token for node registration is invalid.
- `node.invalid_state`: Node is not in the correct state for the requested operation.
- `node.invalid_backup_settings`: Node backup settings are invalid.

## Server (`server.`)

- `server.node_is_offline`: Attempt to create a server on an offline node.
- `server.display_name_already_exists`: Server display name already exists in the team.
- `server.is_not_offline`: Operation requires the server to be offline, but it is not.
- `server.is_not_online`: Operation requires the server to be online, but it is not.
- `server.state_unknown`: Server is in an unknown state.
- `server.backups_empty`: No backups available for the server.
- `server.unsupported_game_type`: Unsupported game type for this operation.

## Team (`team.`)

- `team.not_found`: The specified team was not found.
- `team.name_already_exists`: Team name already exists.
- `team.email_not_verified`: User's email is not verified for team operations.
- `team.too_many_teams`: User has reached the maximum number of owned teams.
- `team.too_many_nodes`: Team has reached the maximum number of nodes.
- `team.too_many_servers`: Team has reached the maximum number of servers.
- `team.too_many_members`: Team has reached the maximum number of members.
- `team.invitation_invalid`: Team invitation is invalid or missing.
- `team.invitation_expired`: Team invitation has expired (older than 14 days).
- `team.already_member`: User is already a member of the team.
- `team.not_member`: User is not a member of the team.
- `team.cannot_leave`: Owner cannot leave their own team.
- `team.cannot_change_own_permissions`: You cannot change your own permissions.
- `team.deletion_failed_has_nodes`: Team cannot be deleted while nodes are registered.
- `team.application_already_exists`: An application user with this email already exists.
- `team.application_not_found`: Application user not found.

## Verification (`verification.`)

- `verification.invalid_token`: The provided verification token is invalid.

## Forbidden (`missingPermission.`)

- `missingPermission.admin_only`: Only GMC administrators are allowed to perform this action.
- `missingPermission.owner_only`: Only the team owner is allowed to perform this action.
- `missingPermission.{permission}`: User is missing the required permission (dynamic).
- `missingPermission.generic`: You are not allowed to perform this action. Either you are not a member of the team or
  you do not have the required permissions.
- `missingPermission.api_account_not_allowed`: API accounts are not allowed to access this resource.

Aliases: `admin_only`, `owner_only`, `api_account_not_allowed`.

## General (`general.`)

- `general.bad_request`: The request body is not readable.
- `general.method_not_allowed`: The request method is not supported.
- `general.unsupported_media_type`: The request media type is not supported.
- `general.server_error`: An unexpected error occurred.
- `general.not_found`: The requested resource was not found.

## XLSX (`xlsx.`)

- `xlsx.parse_exception`: The uploaded file is not in a valid format. An error may have occurred in the file.

## Settings (`settings.`)

- `settings.ini_too_large`: The INI file exceeds the maximum allowed size of 500 KB.
- `settings.profile_too_large`: The setting profile exceeds the maximum allowed size of 1 MB.
- `settings.copy_targets_contains_source`: The destination list must not include the source profile.
- `settings.teams_not_equal`: All target profiles must be in the same team as the source profile.

## Helpdesk (`helpdesk.`)

- `helpdesk_webhook_not_configured`: Helpdesk webhook is not configured.
- `helpdesk_webhook_request_failed`: Failed to send helpdesk request. Please try again later.

---

Notes

- For server, node, and team exceptions, use only the namespaced keys. Aliases without prefixes were removed from
  `ExceptionMessages` to avoid duplicates because the exception classes already add the prefix.
- Validation failures always use `validation.failed` with a combined message.
