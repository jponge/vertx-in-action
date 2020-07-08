#!/bin/bash
hey -z $2 \
    -m POST \
    -D auth.json \
    -T application/json \
    -o csv \
    http://$1:4000/api/v1/token \
    > data/hey-run-token-z$2.csv
