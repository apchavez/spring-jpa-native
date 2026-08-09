#!/usr/bin/env bash
# Runs backend/mobile build/test inside official images (JDK 21 for the api,
# node:20 for the mobile app), matching CI exactly, so nothing beyond Docker
# needs to be installed locally (no Java/Gradle/Node needed on the host).
# Usage: ./docker/dev.sh <api-test|mobile-test|shell>
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
ROOT="$(pwd -W 2>/dev/null || pwd)"

JDK_IMAGE="eclipse-temurin:21-jdk"
NODE_IMAGE="node:20"
CMD="${1:-api-test}"
DOCKER_TTY=""
[[ "$CMD" == "shell" ]] && DOCKER_TTY="-it"

run_api() {
  # -v docker.sock: ./gradlew check uses Testcontainers (Postgres), which needs
  # to talk to the host's Docker daemon to spin up its own throwaway container.
  docker run --rm ${DOCKER_TTY:-} \
    -v "$ROOT/api":/workspace -w /workspace \
    -v spring-jpa-native-gradle-cache:/root/.gradle \
    -v /var/run/docker.sock:/var/run/docker.sock \
    "$JDK_IMAGE" bash -c "chmod +x gradlew && $1"
}

run_mobile() {
  docker run --rm ${DOCKER_TTY:-} \
    -v "$ROOT/mobile":/workspace -w /workspace \
    -v spring-jpa-native-npm-cache:/root/.npm \
    "$NODE_IMAGE" bash -c "$1"
}

case "$CMD" in
  api-test)   run_api "./gradlew check" ;;
  mobile-test) run_mobile "npm ci && npx jest --coverage --ci && npx tsc --noEmit" ;;
  shell)      run_api "bash" ;;
  *) echo "Usage: $0 {api-test|mobile-test|shell}" >&2; exit 1 ;;
esac
