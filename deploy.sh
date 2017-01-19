#!/bin/bash
set -e

### CloudFoundry CLI utilities
CLOUD_DOMAIN=${DOMAIN:-run.pivotal.io}
CLOUD_TARGET=api.${DOMAIN}

function login(){
    cf api | grep ${CLOUD_TARGET} || cf api ${CLOUD_TARGET} --skip-ssl-validation
    cf apps | grep OK || cf login
}

function app_domain(){
    D=`cf apps | grep $1 | tr -s ' ' | cut -d' ' -f 6 | cut -d, -f1`
    echo $D
}

function deploy_service(){
    N=$1
    D=`app_domain $N`
    JSON='{"uri":"http://'$D'"}'
    cf create-user-provided-service $N -p $JSON
}

### Installation

cd `dirname $0`
r=`pwd`
echo $r

## Reset
cf d -f pwa-client
cf d -f pwa-server

cf a
cf s

# Build the server
cd $r && find . -iname pom.xml | xargs -I pom  mvn -DskipTests clean install -f pom

# Deploy the client first
cd $r/client
#rm -rf node_modules
npm install && ng build --prod --aot
python $r/sw.py
cd dist
touch Staticfile
cf push pwa-client --no-start --random-route
cf set-env pwa-client FORCE_HTTPS true

# Stormpath
stormpathApiKeyId=`cat ~/.stormpath/apiKey.properties | grep apiKey.id | cut -f3 -d\ `
stormpathApiKeySecret=`cat ~/.stormpath/apiKey.properties | grep apiKey.secret | cut -f3 -d\ `

# Beer Service
cd $r/server
mvn clean package
cf push -p target/*jar pwa-service --no-start  --random-route
cf set-env pwa-service STORMPATH_API_KEY_ID $stormpathApiKeyId
cf set-env pwa-service STORMPATH_API_KEY_SECRET $stormpathApiKeySecret

# Get the URLs for the client and server
clientUri = https://`app_domain pwa-client`
serverUri = https://`app_domain pwa-client`

echo 'CLIENT_URL: $clientUri'
echo 'SERVER_URL: $serverUri'

# replace the server URL in the client
sed -i -e 's/pwa-service/${serverUri}/g' $r/client/dist/main.*.bundle.js
# replace the client URL in the server
sed -i -e 's/pwa-client/${clientUri}/g' $r/server/src/main/resources/application.properties

# redeploy both client and server
cd $r/client/dist
cf push pwa-client
cd $r/server
mvn package
cf push pwa-server
