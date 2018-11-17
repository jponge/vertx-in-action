#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

GRADLE_PROJECTS=('chapter1' 'chapter2' 'chapter3' 'chapter4')

for project in "${GRADLE_PROJECTS[@]}"; do
  cd $project
  echo ">>> Building ${project}"
  ./gradlew -q build
  echo ">>> Cleaning ${project}"
  ./gradlew -q clean
  cd ..
done
