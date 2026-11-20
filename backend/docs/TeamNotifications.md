# Team Notifications (Discord Webhook)

- Endpoint: `GET /team/{teamId}/notification-settings`
  - Permission: `MANAGE_TEAM`
  - Response: `{ discordWebhookUrl?: string, discordNotificationsMinSeverity?: NotificationSeverity }`

- Endpoint: `POST /team/{teamId}/notification-settings`
  - Permission: `MANAGE_TEAM`
  - Body: `{ discordWebhookUrl?: string, discordNotificationsMinSeverity?: NotificationSeverity }`
  - Notes:
    - `discordWebhookUrl` is optional; omit or set empty to disable forwarding.
    - `discordNotificationsMinSeverity` defaults to `ERROR` when omitted.

- Behavior:
  - All team notifications continue to broadcast to `/user/queue/notifications`.
  - If configured and the minimum severity is met, a Discord embed is sent:
    - Embed description contains the notification text.
    - Embed color reflects severity (INFO blue, WARNING yellow, ERROR red, CRITICAL dark red).
