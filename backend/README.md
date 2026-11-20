# GmcBackend

## Default limits

Default limits apply to new teams and users. `0` means unlimited. They are stored in MongoDB (`config` collection,
id `defaultLimits`) and can be changed via the admin API (`/admin/limits`). Environment variables that are set override
the stored value on every start; variables that are not set keep the stored value.

| Variable | Built-in default | Limit |
|---|---|---|
| `GMC_LIMIT_USER_MAX_OWNED_TEAMS` | 0 | Teams a user can own |
| `GMC_LIMIT_TEAM_MAX_MEMBERS` | 0 | Members per team |
| `GMC_LIMIT_TEAM_MAX_NODES` | 0 | Nodes per team |
| `GMC_LIMIT_TEAM_MAX_SERVERS` | 0 | Servers per team |
| `GMC_LIMIT_SERVER_MAX_BACKUPS` | 0 | Backups kept per server (oldest are deleted) |
| `GMC_LIMIT_SERVER_MAX_RCON_COMMANDS` | 0 | Saved RCON commands per server |
| `GMC_LIMIT_SERVER_STATISTICS_MAX_SIZE` | 200 | Statistics entries per server (stored in one document, keep it bounded) |
| `GMC_LIMIT_SERVER_MAX_LOGS` | 0 | Log files per server |

Limits are copied into a team when it is created, so changed defaults only affect new teams. Limits of existing teams
can be changed per team via the admin API.
