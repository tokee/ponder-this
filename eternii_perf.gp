set terminal png size 800, 500
set linetype 1 lc rgb "#0072B2"
set linetype 2 lc rgb "#CC79A7"
set linetype 3 lc rgb "#009E73"
set linetype 4 lc rgb "#D55E00"
set linetype 5 lc rgb "#E69F00"
set linetype 6 lc rgb "#56B4E9"
set linetype 7 lc rgb "#0000ff"
set linetype 8 lc rgb "#000000"
set linetype 9 lc rgb "#ccccff"
set linetype 10 lc rgb "#3333ff"

set output "`echo $IMG`"

set title "Toke's Eternity II solver performance `echo $DAT`"
set xlabel "Valid pieces (max=`echo $MAX_VALID`)"
set ylabel 'Seconds'

set datafile separator "\t"

set logscale y 10
set grid ytics

set xrange [ 0 : 260 ]
set yrange [ 0.01 : ]

plot "`echo $DAT`" using 2:($1/1000) with points lw 1 lc 1 title 'ms'
