#!/bin/bash
# 优化后的启动脚本（满足：app.log 仅记录 test 模式下的全部程序输出，
# 其他模式下仅记录脚本自身的日志，程序输出重定向到 /dev/null）
set -o errexit
set -o nounset
set -o pipefail

# =========================
# 配置（按需修改）
# =========================
# 优先使用完整 java 可执行路径
JAVA_PATH="/WEB/jdk-21.0.2/bin/java"
APP_JAR="application-1.0.0.jar"                # jar 文件名或相对/绝对路径
APP_NAME="application-prod"                     # 应用名称
LOG_DIR="./logs"
LOG_FILE="${LOG_DIR}/app.log"
JAVA_OPTS="-Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8" # 其他 JVM 参数
FORCE_KILL_TIME=60      # 优雅停止等待秒数，超时后强制 kill -9
CHECK_TIME=600          # 后台检测总时长（秒）
CHECK_INTERVAL=5        # 后台检测间隔（秒）

# 默认启用 Spring Boot debug
ENABLE_DEBUG="${ENABLE_DEBUG:-true}"

# 默认启动参数（包含 profile、loader.path；根据 ENABLE_DEBUG 决定是否加 --debug）
DEFAULT_PARAMS_BASE="--spring.profiles.active=prod --loader.path=libs"
if [[ "${ENABLE_DEBUG}" == "true" ]]; then
  DEFAULT_PARAMS="${DEFAULT_PARAMS_BASE} --debug"
else
  DEFAULT_PARAMS="${DEFAULT_PARAMS_BASE}"
fi


# =========================
# 辅助函数
# =========================
timestamp() { date '+%Y-%m-%d %H:%M:%S'; }

# 脚本日志：脚本自身的所有事件（start/stop/status/restart/test 启动/退出 信息）
# 会被写入 ${LOG_FILE}。注意：在 start/restart/stop/status 等模式下，
# Java 程序的 stdout/stderr 被重定向到 /dev/null（不写入 app.log）。
log() {
  local msg="$1"
  echo "$(timestamp) - ${msg}" | tee -a "$LOG_FILE"
}

# 确保环境变量和文件路径正确
# 包括：APP_HOME, APP_JAR, LOG_DIR, LOG_FILE, JAVA_BIN
# APP_HOME：脚本所在目录
# APP_JAR：JAR 文件绝对路径
# LOG_DIR：日志目录
# LOG_FILE：日志文件
# JAVA_BIN：Java 可执行文件路径
ensure_env() {
  APP_HOME="$(cd "$(dirname "$0")" && pwd)"
  if [[ "$APP_JAR" != /* ]]; then
    APP_JAR="${APP_HOME%/}/${APP_JAR}"
  fi
  mkdir -p "$LOG_DIR"
  touch "$LOG_FILE" 2>/dev/null || {
    echo "$(timestamp) - ERROR: Cannot write to log file ${LOG_FILE}" >&2
    exit 1
  }

  if [[ -n "${JAVA_PATH:-}" && -x "${JAVA_PATH}" ]]; then
    JAVA_BIN="${JAVA_PATH}"
  elif [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    JAVA_BIN="${JAVA_HOME}/bin/java"
  else
    JAVA_BIN="$(command -v java || true)"
    if [[ -z "$JAVA_BIN" ]]; then
      echo "$(timestamp) - ERROR: java not found in PATH and JAVA_PATH/JAVA_HOME not set correctly." | tee -a "$LOG_FILE"
      exit 1
    fi
  fi
}

# 获取 PID：优先通过 JAR 路径/名称匹配
get_pid() {
  local pid
  pid="$(pgrep -f -- "${APP_JAR}" || true)"
  if [[ -z "$pid" ]]; then
    pid="$(pgrep -f -- "${APP_NAME}" || true)"
  fi
  echo "$pid" | head -n1 | tr -d '[:space:]'
}

is_running() {
  local pid
  pid="$(get_pid)"
  if [[ -n "$pid" ]] && ps -p "$pid" -o comm= | grep -qiE 'java|java.exe'; then
    return 0
  fi
  return 1
}

wait_for_termination() {
  local pid=$1
  local timeout=${2:-$FORCE_KILL_TIME}
  local elapsed=0
  while kill -0 "$pid" 2>/dev/null; do
    if (( elapsed >= timeout )); then
      log "Application did not stop within ${timeout}s, force killing PID ${pid}"
      kill -9 "$pid" 2>/dev/null || true
      break
    fi
    log "Waiting for application (PID: $pid) to stop... (${elapsed}s)"
    sleep 1
    elapsed=$((elapsed + 1))
  done
}

background_check() {
  (
    local elapsed=0
    while (( elapsed < CHECK_TIME )); do
      sleep "$CHECK_INTERVAL"
      pid="$(get_pid)"
      if [[ -n "$pid" ]]; then
        echo "$(timestamp) - Application is running (PID: $pid)." >> "$LOG_FILE"
      else
        echo "$(timestamp) - Application is not running." >> "$LOG_FILE"
        break
      fi
      elapsed=$((elapsed + CHECK_INTERVAL))
    done
    echo "$(timestamp) - Background check finished (total ${elapsed}s)." >> "$LOG_FILE"
  ) &
}

# =========================
# 操作函数
# =========================
start() {
  ensure_env
  if is_running; then
    pid="$(get_pid)"
    log "Application is already running (PID: $pid)"
    return 0
  fi

  if [[ ! -f "$APP_JAR" ]]; then
    log "ERROR: JAR file not found: ${APP_JAR}"
    return 1
  fi

  log "Starting application (background). Note: program stdout/stderr -> /dev/null"
  # 用户要求：其他模式下程序输出不要写入 app.log，像这样重定向：
  nohup "$JAVA_BIN" $JAVA_OPTS -jar "$APP_JAR" $DEFAULT_PARAMS "$@" > /dev/null 2>&1 &
  sleep 1
  pid="$(get_pid || true)"
  if [[ -n "$pid" ]]; then
    log "Application started (PID: $pid)."
    background_check
    return 0
  else
    log "Failed to start application. Check system processes for details."
    return 2
  fi
}

stop() {
  ensure_env
  local pid
  pid="$(get_pid || true)"
  if [[ -z "$pid" ]]; then
    log "Application is not running."
    return 0
  fi
  log "Stopping application (PID: $pid) gracefully..."
  kill "$pid" 2>/dev/null || true
  wait_for_termination "$pid" "$FORCE_KILL_TIME"
  if kill -0 "$pid" 2>/dev/null; then
    log "Application (PID: $pid) still present after grace period. Doing kill -9."
    kill -9 "$pid" 2>/dev/null || true
  fi
  log "Application stopped."
  return 0
}

restart() {
  ensure_env
  log "Restarting application..."
  stop
  sleep 1
  start "$@"
}

status() {
  ensure_env
  if is_running; then
    pid="$(get_pid)"
    log "Application is running (PID: $pid)"
  else
    log "Application is not running"
  fi
}

test() {
  # test 模式：在前台运行（不使用 nohup），并将程序全部输出（stdout/stderr）写入 app.log，同时显示到终端
  ensure_env
  if is_running; then
    pid="$(get_pid)"
    log "Application is already running (PID: $pid). Aborting test run."
    return 1
  fi

  if [[ ! -f "$APP_JAR" ]]; then
    log "ERROR: JAR file not found: ${APP_JAR}"
    return 1
  fi

  log "Starting application in test (foreground) mode. Streaming program output to console and ${LOG_FILE}"
  # 将 Java 的 stdout/stderr 通过 tee -a 追加到 app.log，并在终端显示
  "$JAVA_BIN" $JAVA_OPTS -jar "$APP_JAR" $DEFAULT_PARAMS "$@" 2>&1 | tee -a "$LOG_FILE"
  local rc=${PIPESTATUS[0]:-0}
  log "Test run exited with status ${rc}"
  return $rc
}

print_usage() {
  cat <<EOF
Usage: $0 {start|stop|restart|status|test} [additional-params]
  start   - start application in background (program output -> /dev/null). Script events -> ${LOG_FILE}
  stop    - stop application (graceful, then SIGKILL after ${FORCE_KILL_TIME}s). Events -> ${LOG_FILE}
  restart - restart application
  status  - show whether application is running. Events -> ${LOG_FILE}
  test    - run application in foreground and stream ALL program output to console and ${LOG_FILE}
Any arguments after the command will be appended to the java command line.
EOF
}

# =========================
# 主入口
# =========================
if [[ $# -lt 1 ]]; then
  print_usage
  exit 1
fi

cmd="$1"
shift || true

case "$cmd" in
  start)
    start "$@"
    ;;
  stop)
    stop
    ;;
  restart)
    restart "$@"
    ;;
  status)
    status
    ;;
  test)
    test "$@"
    ;;
  *)
    print_usage
    exit 1
    ;;
esac