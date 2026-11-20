# Automations – Frontend Integration Guide

This document explains the complete Automation system for frontend developers: data models, REST endpoints, WebSocket topics, life‑cycle semantics, validations, and error codes. You can rely on just this file.

## Overview
- An Automation defines a trigger (manual/scheduled/state change), a target selector (which servers it applies to), and an ordered workflow of steps.
- When a trigger fires, the backend creates an Automation Run. The run advances step‑by‑step and emits updates over WebSocket (STOMP). The lifecycle is persisted in MongoDB and also accessible via REST.

## Authentication & Permissions
- All endpoints require authentication.
- Required permissions per operation:
  - Read automations and runs: `ACCESS_AUTOMATIONS` for the team.
  - Manage automations (create/edit/delete/trigger manual; cancel/delete runs): `MANAGE_AUTOMATIONS` for the team.

## Data Models

### Automation
- Fields:
  - `id: string`
  - `teamId: string`
  - `name: string`
  - `description?: string`
  - `enabled: boolean`
  - `createdAt: ISO-8601 string`
  - `updatedAt: ISO-8601 string`
  - `runTimeoutSec?: number` (max allowed by server; default applied if null)
  - `targets?: TargetSelector` (polymorphic; null means “manual selection”)
  - `trigger: AutomationTrigger` (polymorphic)
  - `workflow: AutomationStep[]` (ordered)

### TargetSelector (polymorphic)
- Use a `type` field to discriminate:
  - `EXPLICIT`: `{ "type": "EXPLICIT", "ids": string[] }`
  - `DYNAMIC`: `{ "type": "DYNAMIC", "selectors": { field, op, value }[] }`
    - `field`: `NODE_ID | GAME_TYPE | STATE | DISPLAY_NAME`
    - `op`: `EQ | REGEX | CONTAINS`
    - `value`: string
  - `TRIGGER_SOURCE`: `{ "type": "TRIGGER_SOURCE" }`
    - Only valid when `trigger.type === STATE_CHANGE`.
    - Run targets are the single server id from the triggering event.

### AutomationTrigger (polymorphic)
- Use a `type` field to discriminate:
  - `MANUAL`: `{ "type": "MANUAL" }`
  - `SCHEDULED`: `{ "type": "SCHEDULED", "cron": string }`
    - Cron is in UTC. Backend computes and manages `nextFireAt` internally.
  - `STATE_CHANGE`: `{ "type": "STATE_CHANGE", "newState": GameServerState, "serverIds": string[] }`
    - `serverIds` must be non‑empty and belong to the same `teamId`.

### AutomationStep (polymorphic)
- Common: `{ "type": AutomationStepType, "timeoutSec"?: number }`
- Types and fields:
  - `START`: `{ "type": "START" }`
  - `STOP`: `{ "type": "STOP", "message"?: string, "delayMinutes"?: number, "forceStop": boolean }`
  - `RESTART`: `{ "type": "RESTART", "message"?: string, "delayMinutes"?: number }`
  - `UPDATE`: `{ "type": "UPDATE" }`
  - `BACKUP`: `{ "type": "BACKUP", "backupName"?: string }`
  - `SETTING_SYNC`: `{ "type": "SETTING_SYNC", "profileId": string, "gameSettingKeys"?: map, "gameUserSettingKeys"?: map, "hyphenParamKeys"?: string[], "questionMarkParamKeys"?: string[], "gmcSettingKeys"?: string[] }`
  - `RCON_COMMAND`: `{ "type": "RCON_COMMAND", "command": string }`
  - `CONDITION`: `{ "type": "CONDITION", "state": GameServerState }` (marks targets SUCCEEDED if current state matches, else SKIPPED)
  - `TIMED_WAIT`: `{ "type": "TIMED_WAIT", "waitMinutes": number }`
  - `STATE_WAIT`: `{ "type": "STATE_WAIT", "desiredState": GameServerState }`
  - `NOTIFY`: `{ "type": "NOTIFY", "title"?: string, "message"?: string, "severity"?: NotificationSeverity }`
  - `DISCORD_WEBHOOK`: `{ "type": "DISCORD_WEBHOOK", "webhookUrl": string, "content"?: string, "embed"?: DiscordEmbed }`

### AutomationRun
- Fields:
  - `id: string`
  - `automationId: string`
  - `teamId: string`
  - `status: "QUEUED" | "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED" | "TIMEOUTED"`
  - `createdAt: ISO-8601 string`
  - `startedAt?: ISO-8601 string`
  - `finishedAt?: ISO-8601 string`
  - `scheduledFireAt?: ISO-8601 string`
  - `workflowSize: number`
  - `cancelRequested: boolean`
  - `targetIds: { [serverId: string]: boolean }` (value true = globally skipped for subsequent steps)
  - `currentStep?: StepState`
  - `stepHistory: StepState[]`

StepState:
- `{ type: AutomationStepType, workflowIndex: number, startedAt?: string, finishedAt?: string, wakeAt?: string, waitingFor?: GameServerState, perTargetStatus?: { [serverId]: { state: AutomationStepStatus, message?: string, completedAt?: string } } }`
- Per‑target states: `QUEUED | PENDING | SUCCEEDED | FAILED | SKIPPED`.

## REST API

Base: all paths below relative to the backend base URL.

### Automations
- Get one
  - `GET /automation/{automationId}`
  - Permission: `ACCESS_AUTOMATIONS` (by automation.teamId)
  - Response: `Automation`

- List by team
  - `GET /automation/by-team/{teamId}`
  - Permission: `ACCESS_AUTOMATIONS`
  - Response: `Automation[]` ordered by `updatedAt` desc

- Create
  - `POST /automation/create`
  - Permission: `MANAGE_AUTOMATIONS` (by request.teamId)
  - Body: `AutomationCreateRequest`
    - `{ teamId, name, description?, enabled, runTimeoutSec?, targets?, workflow, trigger }`
  - Response: `Automation`
  - Emits WS: `/user/queue/automation/create` (AutomationCreatedPacket)

- Edit
  - `POST /automation/{automationId}/edit`
  - Permission: `MANAGE_AUTOMATIONS`
  - Body: `AutomationUpdateRequest` (same fields except `teamId`)
  - Response: `Automation`
  - Emits WS: `/user/queue/automation/update` (AutomationUpdatedPacket)

- Delete
  - `POST /automation/{automationId}/delete`
  - Permission: `MANAGE_AUTOMATIONS`
  - Response: empty
  - Emits WS: `/user/queue/automation/delete` (AutomationDeletedPacket)

- Trigger manual
  - `POST /automation/{automationId}/trigger-manual`
  - Permission: `MANAGE_AUTOMATIONS`
  - Body (optional): `{ targetIds?: string[], nextFireAt?: ISO-8601 }`
    - If `nextFireAt` present: run is queued to start at that time.
    - If `targetIds` provided: overrides selector with those ids.
    - Forbidden if automation uses `TRIGGER_SOURCE` selector. Returns error `targets_trigger_source_manual_forbidden`.
  - Response: `AutomationRun` (created run in QUEUED or RUNNING)
  - Emits WS: `/user/queue/automation-run/create` (AutomationRunCreatedPacket)

### Automation Runs
- Get one
  - `GET /automation-run/{runId}`
  - Permission: `ACCESS_AUTOMATIONS` (by run.teamId)
  - Response: `AutomationRun`

- List by team (paged)
  - `GET /automation-run/by-team/{teamId}`
  - Permission: `ACCESS_AUTOMATIONS`
  - Query params:
    - `page` (default 0)
    - `size` (default 20, max 200)
    - `automationId` (optional filter)
    - `status` (CSV; e.g., `RUNNING,FAILED`)
    - `createdFrom` (ISO-8601)
    - `createdTo` (ISO-8601)
  - Response: Spring Page: `{ content: AutomationRun[], totalElements, totalPages, size, number, ... }` sorted by `createdAt` desc

- Cancel a run
  - `POST /automation-run/{runId}/cancel`
  - Permission: `MANAGE_AUTOMATIONS`
  - Response: updated `AutomationRun`
  - Behavior:
    - If currently waiting (timed/state wait), cancels immediately (sets `CANCELED`).
    - Otherwise marks `cancelRequested=true` and cancels at next step boundary.
  - Emits WS: `/user/queue/automation-run/update` (AutomationRunUpdatedPacket)

- Delete a terminal run
  - `POST /automation-run/{runId}/delete`
  - Permission: `MANAGE_AUTOMATIONS`
  - Only allowed when status is one of `SUCCEEDED | FAILED | CANCELED | TIMEOUTED`. Otherwise error `run_not_deletable`.
  - Response: empty
  - Emits WS: `/user/queue/automation-run/delete` (AutomationRunDeletedPacket)

## Triggers and Target Resolution
- Manual triggers: targets come from `override targetIds` if provided, otherwise from `targets` selector (EXPLICIT/DYNAMIC or null for “manual selection”).
- Scheduled triggers: `cron` evaluated by backend; targets resolved from selector.
- State change triggers:
  - Backend matches automations by `newState` and `serverIds`.
  - If selector is `TRIGGER_SOURCE`, run targets are exactly the triggering `serverId`.
  - Otherwise, selector EXPLICIT/DYNAMIC is used to resolve targets at run start.

## WebSocket (STOMP)
- Endpoint: `/websocket-frontend` (requires authentication)
- Broker prefixes: `/topic`, `/queue`; app prefix `/app`
- Subscribe to user queues:
  - `/user/queue/automation/create` → `{ automation }`
  - `/user/queue/automation/update` → `{ automation }`
  - `/user/queue/automation/delete` → `{ automationId, teamId }`
  - `/user/queue/automation-run/create` → `{ run }`
  - `/user/queue/automation-run/update` → `{ run }` (on any persisted change)
  - `/user/queue/automation-run/delete` → `{ runId, teamId }`
  - `/user/queue/notifications` → `{ notification }` (team notifications)

## Notifications Generated by Runs
- When a run completes:
  - `SUCCEEDED` → INFO (`AUTOMATION_RUN_SUCCEEDED`)
  - `FAILED` → WARNING (`AUTOMATION_RUN_FAILED`)
  - `CANCELED` → INFO (`AUTOMATION_RUN_CANCELED`)
  - `TIMEOUTED` → WARNING (`AUTOMATION_RUN_TIMEOUTED`)
- Shipped to `/user/queue/notifications` and emailed per user settings.
- If a team has configured a Discord webhook with a minimum severity threshold, notifications meeting the threshold are also forwarded to that webhook.

## Validation Rules and Errors
- Common validation happens on create/edit of automations and on manual triggers.
- Target selectors:
  - `EXPLICIT`: all ids must belong to the same team, else `targets_invalid_team`.
  - `DYNAMIC`: validated structurally; values interpreted at runtime.
  - `TRIGGER_SOURCE`: only allowed with `STATE_CHANGE` trigger; otherwise `targets_trigger_source_only_for_state_change`.
- State change trigger: `serverIds` must be non‑empty and belong to the team, else `automation.state_trigger_servers_required` or `trigger_server_ids_invalid_team`.
- Manual trigger:
  - If automation uses `TRIGGER_SOURCE`, manual triggering is forbidden: `targets_trigger_source_manual_forbidden`.
- Run deletion: if status not terminal, `run_not_deletable`.
- Step/timeout constraints: step and run `timeoutSec` are capped by backend; invalid values return constraint messages like `automation.step_timeout_too_long` or `automation.run_timeout_too_long`.

## Lifecycle and Status Semantics
- Creation: run enters `QUEUED` (scheduled) or `RUNNING` (immediate/manual).
- Steps:
  - Each step sets per‑target `QUEUED`/`PENDING` and updates to `SUCCEEDED`/`FAILED`/`SKIPPED`.
  - If all non‑skipped targets fail in a step, the run ends `FAILED`.
  - If cancellation was requested, run cancels at step boundary.
  - Wait steps (`TIMED_WAIT`/`STATE_WAIT`) resume via scheduler/event and then advance automatically.
- Timeouts:
  - Run or step timeouts transition the run to `TIMEOUTED` and emit a notification.

## Typical Client Flows
1) List automations for a team, render detail view; subscribe to `/user/queue/automation/*` for live updates.
2) Trigger manual runs via POST (if allowed). Optionally pass `targetIds` or schedule with `nextFireAt`.
3) Track runs list with `GET /automation-run/by-team/{teamId}` and subscribe to `/user/queue/automation-run/*` for live updates.
4) Cancel a running run; delete runs once terminal.
5) Show notifications stream from `/user/queue/notifications`.

## Notes
- All timestamps are ISO‑8601 (UTC).
- Pagination defaults: page=0, size=20, max size=200.
- CORS and security are configured; WebSocket requires an authenticated session.
