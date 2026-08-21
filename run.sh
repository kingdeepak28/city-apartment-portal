#!/usr/bin/env bash
# One-command build + run for the Society Document Portal, on any machine.
#
# Usage:
#   ./run.sh              build and start everything (Postgres + backend + frontend)
#   ./run.sh stop         stop everything this script started
#   ./run.sh status       show what's currently running
#   ./run.sh logs [name]  tail logs (backend|frontend|postgres), default: backend
#
# Prefers Docker - works identically on macOS/Linux/Windows-with-Docker-Desktop, with zero other
# dependencies. If Docker isn't available, automatically falls back to downloading a
# self-contained, no-admin-rights-needed toolchain (JDK 17, Maven, Node 20, Postgres 16) into
# ./.toolchain and running everything as plain local processes - exactly how this project's own
# dev environment was originally bootstrapped.
#
# This is for local use only. For a real deployment (a real domain, TLS, real secrets), use
# docker-compose.prod.yml and DEPLOYMENT.md instead.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

RUN_DIR="$ROOT_DIR/.run"
TOOLCHAIN_DIR="${TOOLCHAIN_DIR:-$ROOT_DIR/.toolchain}"
mkdir -p "$RUN_DIR"

DOCKER_APP_URL="http://localhost:8080"
NATIVE_BACKEND_URL="http://localhost:8080"
NATIVE_FRONTEND_URL="http://localhost:5173"
DEMO_CREDS="   Demo admin:  super.admin@societyportal.local / Admin@123
   Demo member: demo.member@societyportal.local / Member@123"

JDK_VERSION="17"
MAVEN_VERSION="3.9.9"
NODE_VERSION="20.18.0"
PG_VERSION="16.4.0"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$1"; }
warn() { printf '\033[1;33m!!\033[0m %s\n' "$1" >&2; }
die()  { printf '\033[1;31mERROR:\033[0m %s\n' "$1" >&2; exit 1; }

# ===========================================================================
# Docker path (preferred)
# ===========================================================================
have_docker() {
  command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1 && docker compose version >/dev/null 2>&1
}

run_docker() {
  log "Docker found - building and starting via Docker Compose (postgres + backend + frontend)..."
  docker compose -f docker-compose.local.yml up --build -d
  log "Waiting for the app to become healthy..."
  local tries=0
  until curl -sf "$DOCKER_APP_URL" >/dev/null 2>&1; do
    tries=$((tries + 1))
    [ "$tries" -gt 90 ] && die "Timed out waiting for the app. Check: docker compose -f docker-compose.local.yml logs"
    sleep 2
  done
  echo
  log "Ready: $DOCKER_APP_URL"
  echo "$DEMO_CREDS"
  echo
  echo "Stop with: ./run.sh stop"
}

stop_docker() {
  docker compose -f docker-compose.local.yml down
}

status_docker() {
  docker compose -f docker-compose.local.yml ps
}

logs_docker() {
  docker compose -f docker-compose.local.yml logs -f --tail=200 "${1:-backend}"
}

# ===========================================================================
# Native fallback (no Docker): self-contained toolchain download + plain processes
# ===========================================================================
detect_platform() {
  local uname_s uname_m
  uname_s="$(uname -s)"
  uname_m="$(uname -m)"
  case "$uname_s" in
    Darwin) OS="darwin"; JDK_OS="mac" ;;
    Linux)  OS="linux"; JDK_OS="linux" ;;
    *) die "Unsupported OS: $uname_s. Install Docker instead (see README) - it works everywhere this script's native fallback doesn't (e.g. Windows outside WSL)." ;;
  esac
  case "$uname_m" in
    x86_64|amd64) ARCH="x64"; PG_ARCH="amd64"; JDK_ARCH="x64" ;;
    arm64|aarch64) ARCH="arm64"; PG_ARCH="arm64v8"; JDK_ARCH="aarch64" ;;
    *) die "Unsupported CPU architecture: $uname_m" ;;
  esac
}

# Postgres's inner archive names the CPU slightly differently per platform - see run.sh's own
# verification notes; this mapping was checked against the actual published artifacts.
pg_inner_name() {
  if [ "$OS" = "darwin" ]; then
    [ "$ARCH" = "x64" ] && echo "postgres-darwin-x86_64.txz" || echo "postgres-darwin-arm_64.txz"
  else
    [ "$ARCH" = "x64" ] && echo "postgres-linux-x86_64.txz" || echo "postgres-linux-arm_64.txz"
  fi
}

ensure_jdk() {
  # Pin to exactly Java 17 (this project's pom.xml <java.version>), not "17 or newer": verified
  # hands-on that this app hangs indefinitely at startup (Tomcat never binds its port, zero log
  # output) under a much newer JDK (tested with OpenJDK 26) despite that JDK reporting a normal
  # version string - a "good enough" range check would have silently picked a broken runtime.
  if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -qE '"17\.'; then
    JAVA_HOME="$(dirname "$(dirname "$(command -v java)")")"
    log "Using system Java: $(command -v java)"
    return
  fi
  local dir="$TOOLCHAIN_DIR/jdk"
  if [ ! -d "$dir" ]; then
    # Adoptium's binary API names macOS "mac", not "darwin" (unlike the Node/Postgres downloads
    # below, which both do use "darwin") - using $OS here 404s on every Mac.
    log "Downloading JDK $JDK_VERSION (Temurin, $JDK_OS/$JDK_ARCH)..."
    mkdir -p "$dir"
    curl -fL -o /tmp/jdk.tar.gz "https://api.adoptium.net/v3/binary/latest/$JDK_VERSION/ga/$JDK_OS/$JDK_ARCH/jdk/hotspot/normal/eclipse"
    tar xzf /tmp/jdk.tar.gz -C "$dir" --strip-components=1
    rm -f /tmp/jdk.tar.gz
  fi
  if [ "$OS" = "darwin" ] && [ -d "$dir/Contents/Home" ]; then
    JAVA_HOME="$dir/Contents/Home"
  else
    JAVA_HOME="$dir"
  fi
}

ensure_maven() {
  if command -v mvn >/dev/null 2>&1; then
    MVN_BIN="$(command -v mvn)"
    log "Using system Maven: $MVN_BIN"
    return
  fi
  local dir="$TOOLCHAIN_DIR/maven"
  if [ ! -d "$dir" ]; then
    log "Downloading Maven $MAVEN_VERSION..."
    mkdir -p "$dir"
    curl -fL -o /tmp/maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
    tar xzf /tmp/maven.tar.gz -C "$dir" --strip-components=1
    rm -f /tmp/maven.tar.gz
  fi
  MVN_BIN="$dir/bin/mvn"
}

ensure_node() {
  if command -v node >/dev/null 2>&1 && node -v | grep -qE '^v(2[0-9]|[3-9][0-9])\.'; then
    NODE_BIN_DIR="$(dirname "$(command -v node)")"
    log "Using system Node: $(command -v node)"
    return
  fi
  local dir="$TOOLCHAIN_DIR/node"
  if [ ! -d "$dir" ]; then
    log "Downloading Node $NODE_VERSION ($OS/$ARCH)..."
    mkdir -p "$dir"
    curl -fL -o /tmp/node.tar.gz "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-$OS-$ARCH.tar.gz"
    tar xzf /tmp/node.tar.gz -C "$dir" --strip-components=1
    rm -f /tmp/node.tar.gz
  fi
  NODE_BIN_DIR="$dir/bin"
}

ensure_postgres() {
  if command -v pg_ctl >/dev/null 2>&1 && command -v initdb >/dev/null 2>&1; then
    PG_BIN_DIR="$(dirname "$(command -v pg_ctl)")"
    log "Using system PostgreSQL: $PG_BIN_DIR"
    return
  fi
  local dir="$TOOLCHAIN_DIR/pgsql"
  if [ ! -d "$dir" ]; then
    log "Downloading PostgreSQL $PG_VERSION ($OS/$PG_ARCH, self-contained server binaries)..."
    mkdir -p "$dir"
    local artifact="embedded-postgres-binaries-$OS-$PG_ARCH"
    local jar_url="https://repo1.maven.org/maven2/io/zonky/test/postgres/$artifact/$PG_VERSION/$artifact-$PG_VERSION.jar"
    local inner
    inner="$(pg_inner_name)"
    curl -fL -o /tmp/pg.jar "$jar_url"
    ( cd /tmp && unzip -o -q pg.jar "$inner" )
    tar xf "/tmp/$inner" -C "$dir"
    rm -f /tmp/pg.jar "/tmp/$inner"
  fi
  PG_BIN_DIR="$dir/bin"
}

start_postgres() {
  local pgdata="$RUN_DIR/pgdata"
  local sock="$RUN_DIR/pgsocket"
  mkdir -p "$sock"
  if [ ! -d "$pgdata" ]; then
    log "Initializing local PostgreSQL data directory..."
    "$PG_BIN_DIR/initdb" -D "$pgdata" -U society_admin --auth=trust >"$RUN_DIR/pg-initdb.log" 2>&1
    # The embedded Postgres distribution downloaded above ships only the server itself
    # (postgres/initdb/pg_ctl) - no psql or createdb client binaries - so the app's database has
    # to be created through the server's own single-user mode instead, and only here, before the
    # postmaster below ever starts and claims the data directory's lock (single-user mode needs
    # exclusive access to it).
    log "Creating society_portal database..."
    echo "CREATE DATABASE society_portal;" | "$PG_BIN_DIR/postgres" --single -D "$pgdata" postgres >"$RUN_DIR/pg-createdb.log" 2>&1
  fi
  if "$PG_BIN_DIR/pg_ctl" -D "$pgdata" status >/dev/null 2>&1; then
    log "PostgreSQL already running"
  else
    log "Starting PostgreSQL on port 5432..."
    "$PG_BIN_DIR/pg_ctl" -D "$pgdata" -l "$RUN_DIR/postgres.log" -o "-k $sock -p 5432" start
  fi
}

stop_postgres() {
  local pgdata="$RUN_DIR/pgdata"
  [ -d "$pgdata" ] || return 0
  ensure_postgres
  "$PG_BIN_DIR/pg_ctl" -D "$pgdata" stop -m fast >/dev/null 2>&1 || true
}

build_backend() {
  local jar="$ROOT_DIR/backend/target/backend.jar"
  if [ -f "$jar" ] && [ -z "$(find "$ROOT_DIR/backend/src" "$ROOT_DIR/backend/pom.xml" -newer "$jar" -type f 2>/dev/null)" ]; then
    log "Backend already built and up to date, skipping build"
    return
  fi
  log "Building backend (mvn package)..."
  ( cd "$ROOT_DIR/backend" && JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" "$MVN_BIN" -q package -DskipTests )
}

start_backend() {
  local pidfile="$RUN_DIR/backend.pid"
  if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    log "Backend already running (pid $(cat "$pidfile"))"
    return
  fi
  log "Starting backend on $NATIVE_BACKEND_URL..."
  # Each assignment/command on its own line, deliberately - cramming "VAR=x cmd & echo $!" onto
  # one line is ambiguous (& binds looser than &&) and was found, hands-on, to capture the wrong
  # PID (this script's own PID, not java's) into the pidfile, so `stop`/`status` controlled the
  # wrong process entirely.
  (
    cd "$ROOT_DIR/backend"
    export DB_URL="jdbc:postgresql://localhost:5432/society_portal"
    export DB_USERNAME="society_admin"
    export DB_PASSWORD="society_admin"
    export JWT_SECRET="local-dev-only-secret-change-before-any-real-use-min-32-bytes"
    export CORS_ORIGINS="$NATIVE_FRONTEND_URL"
    export FRONTEND_URL="$NATIVE_FRONTEND_URL"
    export MAIL_ENABLED="false"
    export SMS_ENABLED="false"
    nohup "$JAVA_HOME/bin/java" -jar target/backend.jar >"$RUN_DIR/backend.log" 2>&1 &
    echo $! > "$pidfile"
  )
  log "Waiting for backend to become healthy..."
  local tries=0
  until curl -sf "$NATIVE_BACKEND_URL/actuator/health" >/dev/null 2>&1; do
    tries=$((tries + 1))
    [ "$tries" -gt 90 ] && die "Backend didn't start in time. Check $RUN_DIR/backend.log"
    sleep 2
  done
}

start_frontend() {
  local pidfile="$RUN_DIR/frontend.pid"
  if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    log "Frontend already running (pid $(cat "$pidfile"))"
    return
  fi
  (
    cd "$ROOT_DIR/frontend"
    export PATH="$NODE_BIN_DIR:$PATH"
    if [ ! -d node_modules ]; then
      log "Installing frontend dependencies (npm install)..."
      npm install --no-audit --no-fund
    fi
    log "Starting frontend dev server on $NATIVE_FRONTEND_URL..."
    nohup npm run dev >"$RUN_DIR/frontend.log" 2>&1 &
    echo $! > "$pidfile"
  )
  log "Waiting for frontend to respond..."
  local tries=0
  until curl -sf "$NATIVE_FRONTEND_URL" >/dev/null 2>&1; do
    tries=$((tries + 1))
    [ "$tries" -gt 60 ] && die "Frontend didn't start in time. Check $RUN_DIR/frontend.log"
    sleep 1
  done
}

run_native() {
  warn "Docker not found - falling back to a self-contained local toolchain (no admin rights needed)."
  detect_platform
  ensure_jdk
  ensure_maven
  ensure_node
  ensure_postgres
  start_postgres
  build_backend
  start_backend
  start_frontend
  echo
  log "Ready: $NATIVE_FRONTEND_URL  (API: $NATIVE_BACKEND_URL)"
  echo "$DEMO_CREDS"
  echo
  echo "Stop with: ./run.sh stop"
  echo "Logs: ./run.sh logs [backend|frontend|postgres]"
}

stop_native() {
  for name in backend frontend; do
    local pidfile="$RUN_DIR/$name.pid"
    if [ -f "$pidfile" ]; then
      local pid; pid="$(cat "$pidfile")"
      if kill -0 "$pid" 2>/dev/null; then
        log "Stopping $name (pid $pid)..."
        kill "$pid" 2>/dev/null || true
      fi
      rm -f "$pidfile"
    fi
  done
  if [ -d "$RUN_DIR/pgdata" ]; then
    log "Stopping PostgreSQL..."
    stop_postgres
  fi
}

status_native() {
  for name in backend frontend; do
    local pidfile="$RUN_DIR/$name.pid"
    if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
      echo "$name: running (pid $(cat "$pidfile"))"
    else
      echo "$name: stopped"
    fi
  done
  if [ -d "$RUN_DIR/pgdata" ]; then
    ensure_postgres
    "$PG_BIN_DIR/pg_ctl" -D "$RUN_DIR/pgdata" status 2>/dev/null || true
  fi
}

logs_native() {
  local name="${1:-backend}"
  local file="$RUN_DIR/$name.log"
  [ "$name" = "postgres" ] && file="$RUN_DIR/postgres.log"
  [ -f "$file" ] || die "No log file for '$name' yet"
  tail -f "$file"
}

# ===========================================================================
# Entry point
# ===========================================================================
cmd="${1:-up}"
case "$cmd" in
  up|start|"")
    if have_docker; then run_docker; else run_native; fi
    ;;
  stop|down)
    if have_docker; then stop_docker; else stop_native; fi
    ;;
  status)
    if have_docker; then status_docker; else status_native; fi
    ;;
  logs)
    if have_docker; then logs_docker "${2:-}"; else logs_native "${2:-}"; fi
    ;;
  *)
    die "Unknown command: $cmd (expected: up|stop|status|logs)"
    ;;
esac
