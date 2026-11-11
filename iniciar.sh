#!/bin/sh
set -eu
cd -- "$(dirname -- "$0")"
if ! command -v javac >/dev/null 2>&1; then
    echo "Instale o JDK 21 ou superior para compilar e abrir o projeto."
    exit 1
fi
mkdir -p bin
javac -encoding UTF-8 -cp 'lib/*:src' -d bin src/models/*.java src/config/*.java src/dao/*.java src/views/*.java src/views/gui/*.java src/Main.java
exec java -cp 'bin:lib/*' Main
