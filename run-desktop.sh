#!/bin/sh
echo "Starting LanChat Desktop..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
"$SCRIPT_DIR/gradlew" :desktop:run
