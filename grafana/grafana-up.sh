#!/bin/bash
set -o errexit -o nounset -o pipefail -o xtrace
export DOCKER_HOST=${1:-ssh://nas}

docker build -t local/auctions-grafana .

CONFIG=template-grafana-compose.yml
test -f $CONFIG || CONFIG="../$CONFIG"

ENV_FILE=${2:-../grafana-params.env}
test -f $ENV_FILE || ENV_FILE=grafana-params.env

docker volume create $(sed -n 's/^grafana_volume=//p' $ENV_FILE) || true

docker-compose -f $CONFIG --env-file $ENV_FILE up --detach
