#!/bin/bash

function pumba_delay {
  echo "💥 Simulating traffic delays on $4 for $1 ($2 / $3)"
  pumba netem \
  --duration $1 \
  --tc-image gaiadocker/iproute2 \
  delay --distribution pareto --time $2 --jitter $3 \
  $4
  echo "🏁 Done with traffic delays on $4"
}

function pumba_stop {
  echo "💥 Stopping $2 for $1"
  pumba stop \
  --restart \
  --duration $1 \
  $2
  echo "🏁 Done stopping $2, should restart now"
}

sleep 60

# 60s

{
  pumba_delay 60s 3000 500 part2-steps-challenge_postgres_1 &
  pumba_delay 60s 3000 500 part2-steps-challenge_mongo_1 &
}
wait

# 120s

sleep 60

# 180s

{
  pumba_stop 60s part2-steps-challenge_postgres_1 &
  pumba_stop 60s part2-steps-challenge_mongo_1 &
}
wait

# 240
