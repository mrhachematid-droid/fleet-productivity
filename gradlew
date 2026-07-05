#!/bin/sh

dir=`dirname "$0"`
exec java -jar "$dir/gradle/wrapper/gradle-wrapper.jar" "$@"