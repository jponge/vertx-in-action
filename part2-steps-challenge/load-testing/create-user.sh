#!/bin/bash
http http://localhost:4000/api/v1/register \
  username="loadtesting-user" \
  password="13tm31n" \
  email="loadtester@my.tld" \
  deviceId="p-123456-abcdef" \
  city="Lyon" \
  makePublic:=true

for n in `seq 10`; do
  http http://localhost:3002/ingest \
    deviceId="p-123456-abcdef" \
    deviceSync:=$n \
    stepsCount:=1200
done
