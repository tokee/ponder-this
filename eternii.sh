#!/bin/bash

: ${XMX:="8g"}

: ${THREADS:=$(( $(grep -c processor /proc/cpuinfo) / 2))} # Hald the amount of physical
: ${RESET_TIME:=$(( 10 * 60 * 1000))} # Every 10 minutes

java -Xmx${XMX} -cp target/ponder-this-*-SNAPSHOT-jar-with-dependencies.jar dk.ekot.eternii.EHub $THREADS $RESET_TIME | tee -a eternii_manual.log

