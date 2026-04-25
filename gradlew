#!/bin/bash

GRADLE_HOME=$(cd $(dirname "$0") && pwd)/gradle/wrapper
CLASSPATH="$GRADLE_HOME/gradle-wrapper.jar"

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
