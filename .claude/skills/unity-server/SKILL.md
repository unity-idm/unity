---
name: unity-server
description: "Manage the local Unity test server (TstServer) for development and debugging. Use this skill whenever the user mentions starting, stopping, restarting, compiling, or checking the test server, viewing server logs, running Unity locally, or performing backup/restore and database export/import via the REST Admin API. Trigger on references to TstServer, localhost:2443, 'is the server running', 'build and run unity', 'kill the server', 'server won't start', 'export a backup', 'restore from backup', or any server lifecycle operation."
---

# Unity Test Server Management

You manage a local Unity test server (`TstServer`) at `https://localhost:2443`. All operations go through the `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh` script — it handles compilation, PID tracking, health checks, and REST API calls so you don't have to piece those together yourself.

## How to think about operations

Always **check status first** before start or stop. This prevents confusing error messages — starting when already running causes port conflicts, and stopping when not running is a no-op the user didn't intend. The script's `start` command does this automatically, but being explicit helps you give the user a clear picture.

When the user's request maps to multiple operations (e.g., "restart and show me the logs"), decompose into sequential steps rather than guessing a single command.

## Operations

### Check status
```bash
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh status
```
Start here when you're unsure about the server state.

### Compile
```bash
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh compile
```
Uses incremental builds (no `clean`) because full rebuilds take minutes and are rarely needed. Only add `clean` if the user explicitly asks or if you're seeing stale-artifact errors (e.g., class-not-found for code that clearly exists).

If compilation fails, the script prints the last 50 lines of the build log. The full log is at `/tmp/unity-compile-output.log` — read it to diagnose the issue before reporting to the user.

### Start
```bash
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh start
```
Compiles first, then launches in the background. The server takes time to initialize — tell the user it's starting and they can access `https://localhost:2443` once ready. They can monitor progress via the log file.

If the compile step within `start` fails, the server won't launch. Check `/tmp/unity-compile-output.log` for the root cause.

### Stop
```bash
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh stop
```
Sends a graceful shutdown signal. If the process persists afterward, **ask the user** before using `kill -9` — force-killing can leave database locks, temp files, or ports in a dirty state that causes problems on the next start.

If the server isn't running, just tell the user — no action needed.

### Restart

Stop then start, sequentially:
```bash
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh stop && ${CLAUDE_SKILL_DIR}/scripts/tstserver.sh start
```

### Verify health
```bash
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh verify
```
Checks that the server is up and the REST Admin API responds. Useful after start to confirm everything initialized correctly, or when the user reports something seems off.

### View logs

The server log is at `integration-tests/target/unity-server.log`. Use the Read tool to show recent entries. For live monitoring, suggest the user run `tail -f` themselves since it's interactive.

### Backup (export database)
```bash
# All content categories → backup.json
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh backup

# Custom filename
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh backup my-backup.json

# Exclude specific categories via query params
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh backup backup.json "auditLogs=false&idpStatistics=false"
```

Available category filters (all default to `true`): `systemConfig`, `directorySchema`, `users`, `auditLogs`, `signupRequests`, `idpStatistics`.

### Restore (import database)
```bash
# From default backup.json
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh restore

# From custom file
${CLAUDE_SKILL_DIR}/scripts/tstserver.sh restore my-backup.json
```

The restore operation restarts internal endpoints, so the REST endpoint may be destroyed before the HTTP response arrives. The script treats a connection reset as success — this is expected, not an error.

## Quick reference

| Action | Command |
|--------|---------|
| Status | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh status` |
| Compile | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh compile` |
| Start | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh start` |
| Stop | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh stop` |
| Verify | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh verify` |
| Backup | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh backup [file] [params]` |
| Restore | `${CLAUDE_SKILL_DIR}/scripts/tstserver.sh restore [file]` |

## Important context

- **Self-signed certificate**: Browsers will show a security warning — this is expected for local dev.
- **Auth credentials**: REST Admin API uses Basic auth, username `a`, password `a`.
- **Working directory**: The script handles `cd` into `integration-tests/` internally since the server resolves config paths relative to that directory.
- **Port conflicts**: If `start` fails with a port-in-use error but `status` shows no Unity process, something else is occupying port 2443. Let the user know so they can investigate.

## Implementation reference

- Endpoint class: `rest-admin/src/main/java/pl/edu/icm/unity/restadm/BackupRestoreRESTAdmin.java`
- REST API docs: `documentation/src/main/rest-api/rest-api-v1.txt`
