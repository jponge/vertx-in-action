#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

for project in chapter*; do
  cd $project
  if [ -f build.gradle.kts ]; then
    echo ">>> Building ${project}"
    ./gradlew -q build
    echo ">>> Cleaning ${project}"
    ./gradlew -q clean
  fi
  cd ..
done
