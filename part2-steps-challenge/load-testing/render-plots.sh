#!/bin/bash

mkdir -p plots
for f in data/hey-run*.csv; do
    target=plots/plot-$(basename $f .csv)
    echo $target
    ./plot-hey-data.py $f $target.png 600
    ./plot-hey-data.py $f $target.pdf 600
done
