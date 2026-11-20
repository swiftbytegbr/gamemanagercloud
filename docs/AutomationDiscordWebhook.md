# Discord Webhook Step

The Discord Webhook automation step sends a message to a Discord channel via a webhook URL. Variable replacement still happens inside the automation system before sending.

## Fields

- `webhookUrl` (string, required): Full Discord webhook URL.
- `content` (string, optional): Plain message content (max ~2000 chars).
- `embed` (object, optional): Simple embed sent alongside the message. If both `content` and `embed` are provided, both are sent in one request.

Embed fields:

- `title` (string, <=256): Embed title.
- `description` (string, <=4096): Embed description.
- `color` (number): Decimal RGB color (e.g., 16711680 for red).
- `authorName` (string, <=256): Author name.
- `authorUrl` (string): Author URL.
- `authorIconUrl` (string): Author image URL (maps to Discord `author.icon_url`).

All `content` and embed fields support message variables like `{automation.name}`, `{targets.names}`, etc. See `docs/AutomationMessageVariables.md`.

## Behavior

- The step performs template replacement for `content` and all embed fields.
- The infrastructure service sends the webhook payload. A 2xx HTTP response marks the step as `SUCCEEDED`; otherwise it is `FAILED` with the HTTP code or error message.

