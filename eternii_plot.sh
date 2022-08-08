#!/bin/bash

cd eternii/

: ${DATS:="$(ls -r *_perf_*.dat)"}

for DAT in $DATS; do
    IMG="${DAT%.*}.png"
    if [[ ! -s $IMG ]]; then
        echo "Generating $IMG"
        DAT=$DAT IMG=$IMG gnuplot ../eternii_perf.gp
    fi
done

