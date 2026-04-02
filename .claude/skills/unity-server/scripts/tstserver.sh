#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
INTEGRATION_TESTS_DIR="$REPO_ROOT/integration-tests"
LOG_FILE="$INTEGRATION_TESTS_DIR/target/unity-server.log"
COMPILE_LOG="/tmp/unity-compile-output.log"
SERVER_URL="https://localhost:2443"
REST_ADMIN_URL="$SERVER_URL/rest-admin/v1"
AUTH="a:a"

get_pid() {
	lsof -ti TCP:2443 -sTCP:LISTEN 2>/dev/null || true
}

cmd_status() {
	local pid
	pid=$(get_pid)
	if [ -n "$pid" ]; then
		echo "TstServer is running (PID: $pid)"
		echo "URL: $SERVER_URL"
	else
		echo "TstServer is not running"
	fi
}

cmd_compile() {
	echo "Compiling Unity (incremental build)..."
	cd "$REPO_ROOT"
	if mvn -T 1C -pl integration-tests -am install -DskipTests -Dgpg.skip=true > "$COMPILE_LOG" 2>&1; then
		echo "Compilation successful"
		rm -f "$COMPILE_LOG"
	else
		echo "Compilation FAILED. See log: $COMPILE_LOG"
		tail -50 "$COMPILE_LOG"
		exit 1
	fi
}

cmd_start() {
	local pid
	pid=$(get_pid)
	if [ -n "$pid" ]; then
		echo "TstServer is already running (PID: $pid). Stop it first."
		exit 1
	fi

	cmd_compile

	echo "Starting TstServer..."
	cd "$INTEGRATION_TESTS_DIR"
	nohup mvn exec:exec -Dexec.executable=java \
      -Dexec.classpathScope=test \
      "-Dexec.args=-classpath %classpath pl.edu.icm.unity.test.integration.TstServer" \
      > /dev/null 2>&1 &

	echo "Waiting for TstServer to bind to port 2443..."
	local attempts=0
	local max_attempts=60
	while [ $attempts -lt $max_attempts ]; do
		pid=$(get_pid)
		if [ -n "$pid" ]; then
			echo "TstServer started (PID: $pid)"
			echo "URL: $SERVER_URL (may take a moment to fully initialize)"
			echo "Logs: $LOG_FILE"
			return
		fi
		sleep 2
		attempts=$((attempts + 1))
	done

	echo "TstServer failed to start within $((max_attempts * 2))s. Check logs: $LOG_FILE"
	exit 1
}

cmd_stop() {
	local pid
	pid=$(get_pid)
	if [ -z "$pid" ]; then
		echo "TstServer is not running"
		return
	fi

	echo "Stopping TstServer (PID: $pid)..."
	echo "$pid" | xargs kill
	sleep 2

	pid=$(get_pid)
	if [ -n "$pid" ]; then
		echo "Process still running (PID: $pid). Use 'kill -9 $pid' to force kill."
		exit 1
	else
		echo "TstServer stopped"
	fi
}

cmd_verify() {
	local pid
	pid=$(get_pid)
	if [ -z "$pid" ]; then
		echo "TstServer is not running"
		exit 1
	fi

	echo "Verifying server health..."
	if curl -sk -o /dev/null -w "%{http_code}" -u "$AUTH" "$REST_ADMIN_URL/db-dump?systemConfig=false&directorySchema=false&users=false&auditLogs=false&signupRequests=false&idpStatistics=false" | grep -q "200"; then
		echo "Server is healthy and responding at $SERVER_URL"
	else
		echo "Server is running but REST Admin API is not responding yet"
		echo "Check logs: $LOG_FILE"
		exit 1
	fi
}

cmd_backup() {
	local output="${1:-backup.json}"
	shift 2>/dev/null || true
	local params="$*"

	echo "Exporting backup to $output..."
	local url="$REST_ADMIN_URL/db-dump"
	if [ -n "$params" ]; then
		url="$url?$params"
	fi

	if curl -sk -u "$AUTH" "$url" -o "$output"; then
		echo "Backup saved to $output"
	else
		echo "Backup failed"
		exit 1
	fi
}

cmd_restore() {
	local input="${1:-backup.json}"

	if [ ! -f "$input" ]; then
		echo "File not found: $input"
		exit 1
	fi

	echo "Restoring from $input..."
	# Import may cause connection reset as server restarts endpoints — treat both as success
	curl -sk -u "$AUTH" -X POST "$REST_ADMIN_URL/db-dump" \
		-H "Content-Type: application/json" \
		-d @"$input" || true
	echo "Restore request sent. Server endpoints may restart."
}

usage() {
	cat <<'EOF'
Usage: tstserver.sh <command> [args]

Commands:
  start              Compile and start the TstServer
  stop               Stop the running TstServer
  compile            Compile Unity (incremental build)
  verify             Check if the server is healthy and responding
  backup [file] [params]  Export database backup (default: backup.json)
                          params: query string e.g. "auditLogs=false&idpStatistics=false"
  restore [file]     Import database from backup (default: backup.json)
  status             Check if TstServer is running

Server URL: https://localhost:2443
Logs:       integration-tests/target/unity-server.log
EOF
}

case "${1:-}" in
	start)   cmd_start ;;
	stop)    cmd_stop ;;
	compile) cmd_compile ;;
	verify)  cmd_verify ;;
	backup)  shift; cmd_backup "$@" ;;
	restore) shift; cmd_restore "$@" ;;
	status)  cmd_status ;;
	*)       usage; exit 1 ;;
esac
