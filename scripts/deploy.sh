#!/usr/bin/env bash

set -euo pipefail

BASE_DIR="${1:?usage: deploy.sh BASE_DIR JAR_PATH [APP_NAME]}"
JAR_PATH="${2:?usage: deploy.sh BASE_DIR JAR_PATH [APP_NAME]}"
APP_NAME="${3:-market-consumer}"

SHARED_DIR="${BASE_DIR}/shared"
CURRENT_DIR="${BASE_DIR}/current"
LOG_DIR="${SHARED_DIR}/logs"
RUN_DIR="${SHARED_DIR}/run"
ENV_FILE="${SHARED_DIR}/app.env"
PID_FILE="${RUN_DIR}/${APP_NAME}.pid"
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:-}"

mkdir -p "${CURRENT_DIR}" "${LOG_DIR}" "${RUN_DIR}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "jar not found: ${JAR_PATH}" >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "missing env file: ${ENV_FILE}" >&2
  exit 1
fi

if [[ -f "${PID_FILE}" ]]; then
  OLD_PID="$(cat "${PID_FILE}")"
  if kill -0 "${OLD_PID}" 2>/dev/null; then
    kill "${OLD_PID}"
    for _ in $(seq 1 30); do
      if ! kill -0 "${OLD_PID}" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    if kill -0 "${OLD_PID}" 2>/dev/null; then
      echo "process did not stop cleanly: ${OLD_PID}" >&2
      exit 1
    fi
  fi
  rm -f "${PID_FILE}"
fi

PREVIOUS_JAR=""
if [[ -L "${CURRENT_DIR}/app.jar" ]]; then
  PREVIOUS_JAR="$(readlink "${CURRENT_DIR}/app.jar")"
fi

ln -sfn "${JAR_PATH}" "${CURRENT_DIR}/app.jar"

set -a
source "${ENV_FILE}"
set +a

nohup "${JAVA_BIN}" ${JAVA_OPTS} -jar "${CURRENT_DIR}/app.jar" >>"${LOG_FILE}" 2>&1 &
NEW_PID=$!
echo "${NEW_PID}" > "${PID_FILE}"

for _ in $(seq 1 30); do
  if kill -0 "${NEW_PID}" 2>/dev/null; then
    exit 0
  fi
  sleep 1
done

echo "new process did not stay up: ${NEW_PID}" >&2
rm -f "${PID_FILE}"

if [[ -n "${PREVIOUS_JAR}" && -f "${PREVIOUS_JAR}" ]]; then
  ln -sfn "${PREVIOUS_JAR}" "${CURRENT_DIR}/app.jar"
  nohup "${JAVA_BIN}" ${JAVA_OPTS} -jar "${CURRENT_DIR}/app.jar" >>"${LOG_FILE}" 2>&1 &
  echo $! > "${PID_FILE}"
fi

exit 1
