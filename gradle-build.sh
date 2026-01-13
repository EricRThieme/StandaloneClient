#!/usr/bin/env bash
set -euo pipefail

# gradle-build.sh
# Loads .env if present and calls ./gradlew with an appropriate -Dorg.gradle.java.home

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

# Load .env if present
if [ -f .env ]; then
  # Only export non-comment lines of the form KEY=VALUE
  set -a
  # shellcheck disable=SC2046
  eval $(grep -v '^\s*#' .env | sed -n 's/\r$//; /./p' | xargs -I{} echo export {} ) || true
  set +a
fi

# Prefer ORG_GRADLE_JAVA_HOME, then JAVA_HOME
GRADLE_JAVA_HOME="${ORG_GRADLE_JAVA_HOME:-${JAVA_HOME:-}}"

if [ -n "$GRADLE_JAVA_HOME" ]; then
  echo "Using Gradle Java home: $GRADLE_JAVA_HOME"
  ./gradlew -Dorg.gradle.java.home="$GRADLE_JAVA_HOME" "$@"
else
  echo "No ORG_GRADLE_JAVA_HOME/JAVA_HOME set; using system java"
  ./gradlew "$@"
fi
