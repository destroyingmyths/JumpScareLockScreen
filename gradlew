#!/bin/sh
# Gradle wrapper script for Termux / Unix
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
JAVA_HOME="${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}"

exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
