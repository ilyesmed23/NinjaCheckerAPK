#!/bin/sh
APP_HOME="${0%/*}"
exec java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
