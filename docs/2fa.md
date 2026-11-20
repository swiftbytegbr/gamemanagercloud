# Two-Factor Authentication (2FA)

Short guide for frontend devs integrating 2FA: Email codes, TOTP (authenticator apps), and one‑time Backup Codes. New:
- `backupCode` login field for backup codes.
- Backup codes are generated once when 2FA is activated and shown exactly once.

## Email 2FA

- Purpose: One-time code sent by email when logging in.
- Trigger at login: `POST /auth/login` without a valid `twoFACode` for users with `twoFAType = EMAIL`.
- Enable:
    - `POST /user/@me/2fa/email/enable`
    - Requires verified email; fails if any 2FA method is already enabled.
    - Response on first activation includes backup codes once: `{ backupCodesGenerated: true, backupCodes: ["XXXX-XXXX-XXXX", ...] }`.
    - A confirmation email is sent when 2FA is enabled or disabled.
- Disable:
    - `POST /user/@me/2fa/email/disable` with `{ twoFACode: "123456" }` or `{ backupCode: "XXXX-XXXX-XXXX" }`.
    - Requires a valid email 2FA code or a backup code in the request.

## Disable 2FA

- TOTP: `POST /user/@me/2fa/totp/disable` with `{ twoFACode: "123456" }` or `{ backupCode: "XXXX-XXXX-XXXX" }`.
- Email: `POST /user/@me/2fa/email/disable` with `{ twoFACode: "123456" }` or `{ backupCode: "XXXX-XXXX-XXXX" }`.
- A valid current 2FA code or a backup code is required to disable.

## TOTP 2FA (Authenticator App)

- Purpose: 6‑digit codes from apps like Google Authenticator, 1Password, or Authy.
- Setup:
    - `POST /user/@me/2fa/totp/setup` → `{ secret, provisioningUri, issuer, accountName }`.
    - Fails if another 2FA method is already enabled. User must disable first.
    - Show a QR using `provisioningUri` or the `secret` for manual entry.
    - User enters a code from the app → `POST /user/@me/2fa/totp/verify` with `{ "code": "123456" }` to enable TOTP.
- Login:
    - If 2FA is enabled and `twoFAType = TOTP`, login requires a code.
    - Retry `POST /auth/login` with `twoFACode` set to the 6‑digit TOTP code (string).
    - Alternatively, a backup code can be used by sending `backupCode` with the exact code (e.g., `"ABCD-EF12-3456"`).
- Disable:
    - `POST /user/@me/2fa/totp/disable` with `{ twoFACode: "123456" }` or `{ backupCode: "XXXX-XXXX-XXXX" }`.
    - Disabling clears the TOTP secret and any remaining backup codes.
    - A confirmation email is sent when 2FA is enabled or disabled.

## What is a provisioning URI?

- It’s an `otpauth://` link encoding all data the authenticator app needs.
- Format: `otpauth://totp/{issuer}:{account}?secret=BASE32&issuer={issuer}&algorithm=SHA1&digits=6&period=30`.
- QR Code: Render this URI as a QR; scanning it configures the account in the app.
- Fields:
    - `secret`: Base32-encoded TOTP secret shared between server and app.
    - `issuer`: Display name of the service (defaults to `GMC`).
    - `account`: Usually the user’s email.
    - `digits`, `period`, `algorithm`: Defaults are 6 digits, 30s, SHA1.

## Backup Codes

- Purpose: One‑time recovery codes to access your account when you cannot receive a 2FA code (email/TOTP unavailable).
- Format: 12 characters shown as `XXXX-XXXX-XXXX` (hex), case‑insensitive. Each code is single‑use.
- Generation: Shown once when 2FA is first activated (either Email or TOTP). You cannot retrieve them later.
- Storage: Never stored in plaintext. Only strong, salted password hashes are stored server‑side.
- Usage: On login, send the code via `backupCode`. Do not put backup codes in `twoFACode`.
  - Also accepted when changing 2FA type or disabling TOTP/2FA as authorization.
- Lifecycle: Using a code consumes it. Disabling 2FA clears any remaining backup codes.

## Common Notes

- Error keys: `auth.2fa_required_totp` (enter TOTP or backup code), `auth.2fa_required_email` (enter email code or backup; a new email is sent), `auth.invalid_backup_code` (backup code invalid), `auth.invalid_2fa_token` (code invalid),
  `user.email_not_verified_for_2fa` (email must be verified before enabling email 2FA),
  `user.twofa_already_enabled` (disable current 2FA before enabling another),
  `user.totp_enable_via_verify` (enable TOTP via setup + verify).
- `twoFACode` (string) is used for email/TOTP numeric codes.
- `backupCode` (string) is the single field name used anywhere a backup code can be provided (login or disable endpoints).
- Issuer label defaults to `GMC` in authenticator apps.
- Switching rules: There is no direct type change endpoint. Enable/disable each method explicitly. If one method is enabled, the other cannot be enabled until the current one is disabled. Disabling requires a valid code or a backup code; if using EMAIL and no code is provided, the server responds with `auth.2fa_required` and sends a code.
