#!/bin/sh

die() { echo "$*" 1>&2 ; exit 0; }  # Return 0 to stop docker container

[[ -z "${LOCAL_IP}" ]] && die 'Required env variable $PACKMATE_LOCAL_IP is not set, exiting!'
[[ -z "${IFACE}" ]] && die 'Required env variable $PACKMATE_INTERFACE is not set, exiting!'

echo "Capturing on interface ${IFACE} with IP ${LOCAL_IP}..."
echo "--> DEBUG: Web login is ${WEB_LOGIN}:${WEB_PASSWORD}"
exec java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -jar app.jar --spring.datasource.url="jdbc:postgresql://127.0.0.1:65001/${DB_NAME}" --spring.datasource.username="${DB_USER}" --spring.datasource.password="${DB_PASSWORD}" --interface-name="${IFACE}" --local-ip="${LOCAL_IP}" --account-login="${WEB_LOGIN}" --account-password="${WEB_PASSWORD}" --server.port=65000 --server.address='0.0.0.0'
