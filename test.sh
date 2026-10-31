#!/usr/bin/env bash
set -e

if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@21" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
    elif [ -d "/opt/homebrew/opt/openjdk" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk"
    fi
fi

JAVAC_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

mkdir -p bin
"$JAVAC_CMD" -d bin -cp "lib/*" $(find src/main/java src/test/java -name "*.java")
"$JAVA_CMD" -cp "bin:lib/*" com.example.gemini.TestRunner

