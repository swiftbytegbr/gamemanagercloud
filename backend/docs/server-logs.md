# Server Logs: Shared Guide (Daemon + Frontend)

This document explains how server logs are identified, sent from daemons, stored by the backend, and consumed by frontends.

## Overview
- Logs are stored as a single MongoDB document per logical file and capped to a tail window (default 2 MiB).
- Logs are appended in chunks with explicit byte offsets. The backend detects gaps and requests backfill.
- Frontend lists logs via lightweight metadata queries and can fetch full documents when needed.
- Retention uses a TTL on `updatedAt` (7 days after last write).

## Identity and Data Model
- Identity (three-key): `serverId`, `filename`, `createdAt` (UTC). Each document also has an `_id` used in HTTP paths.
- Collection: `server_logs`
  - Fields: `_id`, `serverId`, `teamId`, `filename`, `createdAt`, `updatedAt`, `sizeBytes`, `content` (capped text).
  - Indexes: unique `{serverId, filename, createdAt}`, `{serverId, updatedAt}`, TTL on `updatedAt` (expire after 7d).

## Retention
- TTL: Documents expire 7 days after their `updatedAt`. Each accepted append refreshes `updatedAt`.

## Daemon → Backend Protocol
- Chunk packet: `ServerLogChunkPacket`
  - Fields: `serverId`, `filename`, `createdAt`, `content` (UTF-8), `offset` (UTF-8 byte position).
  - Destination (STOMP): `/server/log-chunk`

- Offset rules (append-only):
  - `offset == currentSize` → accepted and appended.
  - `offset > currentSize` → gap detected. Backend sends `ServerLogBackfillRequestPacket` and ignores the chunk.
  - `offset < currentSize` → ignored (no overwrite-in-place).

- Backfill request: `ServerLogBackfillRequestPacket`
  - Fields: `serverId`, `filename`, `createdAt`, `offset`, `length`.
  - Destination (to daemon): `/server/log-backfill-request`
  - Daemon should respond by sending the requested range as normal chunks (may split into multiple messages).

- Chunk sizing & flow:
  - Recommended chunk size: 32–128 KiB (64 KiB typical). Coalesce if you send very frequently.
  - Large gaps: Backend currently requests the full missing range; daemon should split into regular chunks.
  - Tail cap: Backend keeps only the last 2 MiB; older bytes fall off the front. Backfilling regions that have fallen out of the tail window becomes unnecessary.

- File rotation/truncation:
  - Rotation creates a new document via a new `createdAt` for the same `filename` and `serverId`.
  - Truncation-in-place should be represented as a new `createdAt` (new document). Do not reuse the prior key.

## Backend Behavior (summary)
- Merge logic (current implementation):
  - Reads the existing document, validates offset rules, merges payload bytes into the correct position, tail-caps to 2 MiB, updates `sizeBytes` and `updatedAt`, then saves.
  - On gap: sends backfill request and ignores the chunk.
- Broadcasts a STOMP event to the team on accepted append: destination `/user/queue/server/log-write`, payload is `ServerLogChunkPacket`.

## Frontend API
- List logs (metadata only by default):
  - `GET /server/{serverId}/logs?includeContent=false&latestOnly=true`
    - `includeContent=false` (default): excludes `content` from the query response.
    - `latestOnly=true` (default): returns only the newest log per `filename`.
  - To fetch all generations: `latestOnly=false`.
  - To include full content in the list (heavier): `includeContent=true`.

- Get one log (full document):
  - `GET /server/{serverId}/logs/{logId}` → returns the entire document including `content`.

- Real-time updates:
  - Subscribe to `/user/queue/server/log-write` for incremental updates.
  - When receiving a message:
    - If viewing the same log and `offset == current content length`, you can optimistically append `content` to your view.
    - Otherwise, re-fetch the document to ensure consistency.

## Error and Edge Cases
- Gaps: If the daemon sends a higher offset than the backend `currentSize`, the backend requests backfill for `[currentSize, offset)` and ignores the out-of-order chunk.
- Large backfills: Daemon should send in its normal chunk size. Backend may request gaps even if parts fall outside the tail window; daemon can drop those if content is no longer relevant.
- Concurrency: Multiple appends for the same log may arrive; the backend uses read-modify-write and last-save-wins. Daemons should avoid racing on the same file key.

## Performance Notes
- Cost per write grows with current log size (full-document rewrite up to 2 MiB). Expect tens to low hundreds of writes/sec per backend instance when logs are at cap.
- Recommendations if needed later: reduce tail cap, coalesce chunks on daemon, batch appends, consider switching to atomic append via Mongo pipeline updates.

## Daemon Checklist
- Track `serverId`, `filename` (absolute), and `createdAt` for identity.
- Maintain a byte offset in UTF-8 and send it with each chunk.
- On backfill request, send the specified range via chunks and resume normal streaming.
- Start a new `createdAt` for rotated/truncated files.

## Frontend Checklist
- Default list: call with `includeContent=false&latestOnly=true` for fast metadata.
- Load details by `logId` when needed to get `content`.
- Listen to `/user/queue/server/log-write` and update the UI (append or refetch).
