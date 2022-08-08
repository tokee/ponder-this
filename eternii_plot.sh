#!/bin/bash

cd eternii/

: ${DATS:="$(ls -r *_perf_*.dat)"}

for DAT in $DATS; do
    IMG="${DAT%.*}.png"
    MAX_VALID=$(tail -n 1 $DAT | cut -d$'\t' -f2)
    if [[ ! -s $IMG ]]; then
        echo "Generating $IMG"
        MAX_VALID=$MAX_VALID DAT=$DAT IMG=$IMG gnuplot ../eternii_perf.gp
    fi
done

