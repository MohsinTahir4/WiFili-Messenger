#!/bin/sh
echo "Starting WiFili Web Server for Linux & Windows..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
"$SCRIPT_DIR/gradlew" :desktop:run --args="web"
