#!/bin/bash

mvn clean package dependency:copy-dependencies

docker build . -t local/auction-watcher

echo "Done!"
