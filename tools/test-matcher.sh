#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
javac -encoding UTF-8 -d "$TEMP" "$ROOT/app/src/main/java/io/github/pingjing/app/RuleMatcher.java" "$ROOT/tools/RuleMatcherTest.java"
java -cp "$TEMP" RuleMatcherTest
