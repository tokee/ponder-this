# quad
  
## Atoms

### qpiece (`int`)
```
<8 pieceID> upper left
<8 pieceID> upper right
<8 pieceID> lower right
<8 pieceID> lower left
```

### qinner (`long`)
```
<16 unused>
<2 rotation> piece northwest rotation
<2 rotation> piece northeast rotation
<2 rotation> piece southeast rotation
<2 rotation> piece southwest rotation
<2*5 color> qpiece north edge
<2*5 color> qpiece east edge
<2*5 color> qpiece south edge
<2*5 color> qpiece west edge
```

### PieceMap (`byte[256]`)

if set, the piece is available 
```
<byte> if 1, the piece with the same index is available for use
       if 0, the piece is already used on the board
```
# QuadBag

There are 14 distinct QuadBags with some overlap
The QuadBags never changes after construction and acts as base for QuadSets which does change

14 QuadBags (roughly) in order of how many quads they represent:

* NW corner (1,200)
* NE corner
* SE corner
* SW corner
* NW clue (4,000)
* NE clue
* SE clue
* SW clue
* Center clue (4,000)
* N edge (62,800)
* E edge
* S edge
* W edge
* Inner (760,000)

Inner is special as all entries can be rotated. Either pack them in 4-tuples to make it cheap
to mask or do some special processing to make lookup work and rotate when they should be used.

QuadBags share a PieceMap, which is basically a byte[256], where 1 means the piece is enabled.
When accessed, the state of the QuadBag is updated if the PieceMap has changed since last access.


For each of these there are 13 QuadSet: Maps from edge(s) to QuadIDs. The QuadIDs are represented
as bitmaps for dense structures, simple array for sparse structures (NESW, maybe NES/ESW/SWN/WNE)

Maps<edge(s), QuadIDs>:
* N, E, S, W, (4*24)
* NE, SE, SW, NW,(4*576)
* NES, ESW, SWN, WNE (4*13824)
* NESW (331766)

QuadSets share a PieceMap, which is basically a byte[256], where 1 means the piece is enabled.
When accesses, either by iteration or size, the state of the QuadSets is updated if the PieceMap
has changed since last access.
