#!/bin/bash

# 定义全局变量
JAVA_HOME=/WEB/javaweb/kttest/jdk-21.0.5

APP_NAME="server-1.0.0.jar"

# APP路径为此脚本路径
APP_HOME="$(cd "$(dirname "$0")"; pwd)"
JAR_FILE="${APP_HOME}/${APP_NAME}"
LIBS_DIR="${APP_HOME}/libs"
CONFIG_DIR="${APP_HOME}/config/"
LOG_DIR="${APP_HOME}/logs"
LOG_FILE="${LOG_DIR}/app.log"
MAIN_CLASS="ApplicationKt"
# JVM参数( 定义APPNAME,设置时区为上海，堆内容大小和最大大小，元空间初始大小和最大大小， 新生代和老年代，GC垃圾收集，错误诊断
DEFAULT_PARAMS="--spring.profiles.active=dev  --loader.path=libs" # 默认启动参数
JVM_OPTS="-Dname=$AppName -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError  -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -Xlog:gc"
# 停止服务时，强制关闭服务倒计时：秒（先kill倒计时结束还未结束的任务，强行停止）
FORCE_KILL_TIME=60
# 程序启动后，持续在后台检测程序状态时间,秒
CHECK_TIME=600
# 程序启动时连续检测是否正在运行中的次数
START_CONSOLE_COUNT=5

# 设置 LOG_HOME 供程序内部使用
export LOG_HOME="$LOG_DIR"

# 确保日志目录存在
mkdir -p "$LOG_HOME"

# 构建 classpath，包括主 JAR 和所有依赖 JAR
CLASSPATH="$JAR_FILE"
if compgen -G "${LIBS_DIR}/*.jar" > /dev/null; then
    for jar in "${LIBS_DIR}"/*.jar; do
        CLASSPATH="${CLASSPATH}:${jar}"
    done
else
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Warning: No JAR files found in ${LIBS_DIR}/"  | tee -a "$LOG_FILE" # >> "$LOG_FILE"
fi

# 获取当前运行的应用程序 PID
get_pid() {
    pgrep -f "$APP_NAME"
}

# 检查程序是否已终止
wait_for_termination() {
    local pid=$1
    local timeout=$2
    local elapsed=0
    while kill -0 "$pid" 2>/dev/null; do
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Waiting for application (PID: $pid) to stop..." >> "$LOG_FILE"
        sleep 1
        elapsed=$((elapsed + 1))
        if [ "$elapsed" -ge "$timeout" ]; then
            echo "$(date '+%Y-%m-%d %H:%M:%S') - Application did not stop within $timeout seconds. Force killing..." >> "$LOG_FILE"
            kill -9 "$pid" 2>/dev/null
            break
        fi
    done
}

# 后台检测程序状态
back_check_status() {
    local check_time=0
    local console_output_count=0
    local max_console_output=$START_CONSOLE_COUNT  # 在命令行输出的最大次数

    # 第一个循环：控制台输出最多 5 次
    while [ "$console_output_count" -lt "$max_console_output" ]; do
        sleep 5
        pid=$(get_pid)
        if [ -n "$pid" ]; then
            log_message="$(date '+%Y-%m-%d %H:%M:%S') - Application is running (PID: $pid)."
            echo "$log_message" | tee -a "$LOG_FILE"
            console_output_count=$((console_output_count + 1))
        else
            log_message="$(date '+%Y-%m-%d %H:%M:%S') - Application is not running."
            echo "$log_message" | tee -a "$LOG_FILE"
            return  # 如果程序不运行，则退出函数
        fi
    done

    # 第二个循环：后台运行，不输出到控制台，仅写入日志文件
    (
        while [ "$check_time" -lt "$CHECK_TIME" ]; do
            sleep 5
            pid=$(get_pid)
            if [ -n "$pid" ]; then
                log_message="$(date '+%Y-%m-%d %H:%M:%S') - Application is running (PID: $pid)."
            else
                log_message="$(date '+%Y-%m-%d %H:%M:%S') - Application is not running."
                echo "$log_message" >> "$LOG_FILE"
                return
            fi
            echo "$log_message" >> "$LOG_FILE"  # 仅写入日志文件
            check_time=$((check_time + 5))
        done
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Stop checking after $CHECK_TIME seconds." >> "$LOG_FILE"
    ) &
}

# 启动函数
start_app() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Application is already running (PID: $pid)."  | tee -a "$LOG_FILE"  #  | tee -a "$LOG_FILE" 替换掉 >> "$LOG_FILE"
        exit 1
    fi

    echo "$(date '+%Y-%m-%d %H:%M:%S') - Starting application..." | tee -a "$LOG_FILE"   # >> "$LOG_FILE"
    nohup "$JAVA_HOME/bin/java" \
	$JVM_OPTS \
        -Dconfig.dir="$CONFIG_DIR" \
        -cp "$CLASSPATH" \
        "$MAIN_CLASS" \
        > /dev/null 2>&1 &    # >> /dev/null 2>> "$LOG_FILE" &
         # 将标准输出和标准错误都重定向到 /dev/null 防止 app.log 文件过大
    sleep 2
    # 去检测程序状态
    back_check_status
}

# 停止函数
stop_app() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Stopping application (PID: $pid)..."  | tee -a "$LOG_FILE" # >> "$LOG_FILE"
        kill "$pid"
	wait_for_termination "$pid" $FORCE_KILL_TIME # 等待程序完全停止，超时 force_kill_time  秒后强制终止
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Application stopped."  | tee -a "$LOG_FILE" # >> "$LOG_FILE"
    else
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Application is not running."  | tee -a "$LOG_FILE" # >> "$LOG_FILE"
    fi
}

# 重启函数
restart_app() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Restarting application..."  | tee -a "$LOG_FILE"
    stop_app
    sleep 1
    start_app
}

# 查看状态函数
status_app() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Application is running (PID: $pid)."  | tee -a "$LOG_FILE"
    else
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Application is not running."  | tee -a "$LOG_FILE"
    fi
}

# 测试函数
test_app() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Application is already running (PID: $pid)."  | tee -a "$LOG_FILE"
        exit 1
    fi

    echo "$(date '+%Y-%m-%d %H:%M:%S') - Starting application..."  | tee -a "$LOG_FILE"
    # 执行程序
    "$JAVA_HOME/bin/java" \
	$JVM_OPTS \
        -Dconfig.dir="$CONFIG_DIR" \
        -cp "$CLASSPATH" \
        "$MAIN_CLASS"  | tee -a "$LOG_FILE"
}

# 打印帮助信息
print_help() {
    echo "Usage: $0 {start|stop|restart|status|test}"
}

# 主逻辑
case "$1" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        restart_app
        ;;
    status)
        status_app
        ;;
    test)
        test_app
        ;;
    *)
        print_help
        exit 1
        ;;
esac
