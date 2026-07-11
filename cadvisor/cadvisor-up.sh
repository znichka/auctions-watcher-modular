#!/bin/bash
set -o errexit -o nounset -o pipefail -o xtrace
export DOCKER_HOST=${1:-ssh://nas}

CONFIG=template-cadvisor-compose.yml
test -f $CONFIG || CONFIG="../$CONFIG"

ENV_FILE=${2:-../cadvisor-params.env}
test -f $ENV_FILE || ENV_FILE=cadvisor-params.env

docker-compose -f $CONFIG --env-file $ENV_FILE up --detach
