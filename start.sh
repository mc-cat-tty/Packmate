#!/bin/sh
IFACE="$(ls /sys/class/net | grep '^e')"
echo "Capturing on interface ${IFACE} with IP ${LOCAL_IP}..."
exec java -jar app.jar --spring.datasource.url="jdbc:postgresql://127.0.0.1:65001/${DB_NAME}" --spring.datasource.username="${DB_USER}" --spring.datasource.password="${DB_PASSWORD}" --interface-name="${IFACE}" --local-ip="${LOCAL_IP}" --account-login="${WEB_LOGIN}" --account-password="${WEB_PASSWORD}" --server.port=65000
