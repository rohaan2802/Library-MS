#!/bin/sh
set -eu
if [ -n "${SPRING_DATASOURCE_URL:-}" ]; then
  case "$SPRING_DATASOURCE_URL" in
    mysql://*) export SPRING_DATASOURCE_URL="jdbc:mysql://${SPRING_DATASOURCE_URL#mysql://}" ;;
    jdbc:mysql://*) ;;
    *) echo "invalid datasource URL" >&2; exit 1 ;;
  esac
fi
exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC -Xss256k -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=64m -Djava.awt.headless=true -jar /app/app.jar