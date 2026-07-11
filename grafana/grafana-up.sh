#!/bin/bash
set -o errexit -o nounset -o pipefail -o xtrace
export DOCKER_HOST=${1:-ssh://nas}

ALERTING_ENV_FILE=${3:-../grafana-alerting-params.env}
test -f $ALERTING_ENV_FILE || ALERTING_ENV_FILE=grafana-alerting-params.env

# Render the Telegram contact point from its template, substituting real values from
# $ALERTING_ENV_FILE. Done here (not via Grafana's own $__env{} provisioning macro)
# because that macro coerces a numeric-looking chat id into a JSON number and fails
# schema validation. `set +x` avoids echoing the secret into xtrace output.
{ set +o xtrace; } 2>/dev/null
BOT_TOKEN=$(sed -n 's/^TELEGRAM_BOT_TOKEN=//p' $ALERTING_ENV_FILE)
CHAT_ID=$(sed -n 's/^TELEGRAM_CHAT_ID=//p' $ALERTING_ENV_FILE)
sed -e "s/__TELEGRAM_BOT_TOKEN__/$BOT_TOKEN/" -e "s/__TELEGRAM_CHAT_ID__/$CHAT_ID/" \
  provisioning/alerting/contact-points.yaml.template > provisioning/alerting/contact-points.yaml
set -o xtrace

docker build -t local/auctions-grafana .

CONFIG=template-grafana-compose.yml
test -f $CONFIG || CONFIG="../$CONFIG"

ENV_FILE=${2:-../grafana-params.env}
test -f $ENV_FILE || ENV_FILE=grafana-params.env

docker volume create $(sed -n 's/^grafana_volume=//p' $ENV_FILE) || true

docker-compose -f $CONFIG --env-file $ENV_FILE up --detach
