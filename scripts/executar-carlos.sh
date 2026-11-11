#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p bin
mapfile -d '' fontes_carlos < <(find src -name '*.java' -print0)
javac -encoding UTF-8 -d bin "${fontes_carlos[@]}"
java -cp 'bin:lib/*' Main --cli
