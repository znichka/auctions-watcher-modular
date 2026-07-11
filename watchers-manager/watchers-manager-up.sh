#!/bin/bash
set -o errexit -o nounset -o pipefail -o xtrace
export DOCKER_HOST=${1:-ssh://nas}

mvn clean package -DskipTests
docker build -t local/auctions-manager .


CONFIG=template-manager-compose.yml
test -f $CONFIG || CONFIG="../$CONFIG"

ENV_FILE=${2:-../nas-prod-params.env}
test -f $ENV_FILE || ENV_FILE=nas-prod-params.env

docker-compose -f $CONFIG --env-file $ENV_FILE up --detach

