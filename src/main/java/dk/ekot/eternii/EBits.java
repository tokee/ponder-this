/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.ekot.eternii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles read/write for board field states as well as hashing of edge colors.
 *
 * All board field has a state. A state is a long consisting of the following bits:
 * {@code
 * <1 unused>
 * <9 piece> where 8 lower bits are standard pieces and 0b111111111 is no piece
 * <2 rotation> 0=north, 1=east, 2=south, 3=west
 * <5 color> piece north edge
 * <5 color> piece east edge
 * <5 color> piece south edge
 * <5 color> piece west edge
 * -------- 32 bit above, 32 bit below
 * <8 unused>
 * <4 edges> 0b1000=north, 0b0100=east, 0b0010=south, 0b0001=west
 * <5 color> outer north edge
 * <5 color> outer east edge
 * <5 color> outer south edge
 * <5 color> outer west edge
 * }
 */
public class EBits {
    private static final Logger log = LoggerFactory.getLogger(EBits.class);

    public static final int PIECE_SHIFT = 54;
    public static final long PIECE_MASK = (~(-1L<<9)) << PIECE_SHIFT;

    public static final int ROTATION_SHIFT = 52;
    public static final long ROTATION_MASK = (~(-1L<<2)) << ROTATION_SHIFT;

    // Abstract edges
    public static final int EDGE_SHIFT = 5;
    public static final long EDGE_MASK = ~(-1L<<5);
    public static final long EDGE2_MASK = ~(-1L<<10);
    public static final long EDGE3_MASK = ~(-1L<<15);
    public static final long EDGE4_MASK = ~(-1L<<20);

    // Outer edges
    public static final int EDGES_DEFINED_SHIFT = 20;
    public static final long EDGES_DEFINED_MASK = (~(-1L << 4)) << EDGES_DEFINED_SHIFT;

    public static final int NORTH_EDGE_SHIFT = EDGE_SHIFT*3;
    public static final long NORTH_EDGE_MASK = EDGE_MASK << NORTH_EDGE_SHIFT;
    public static final int EAST_EDGE_SHIFT = EDGE_SHIFT*2;
    public static final long EAST_EDGE_MASK = EDGE_MASK  << EAST_EDGE_SHIFT;
    public static final int SOUTH_EDGE_SHIFT = EDGE_SHIFT;
    public static final long SOUTH_EDGE_MASK = EDGE_MASK << SOUTH_EDGE_SHIFT;
    public static final int WEST_EDGE_SHIFT = 0;
    public static final long WEST_EDGE_MASK = EDGE_MASK;

    // Piece edges
    public static final int PIECE_EDGES_SHIFT = 32;
    public static final long PIECE_EDGE4_MASK = EDGE4_MASK << PIECE_EDGES_SHIFT;

    public static final int PIECE_NORTH_EDGE_SHIFT = NORTH_EDGE_SHIFT + PIECE_EDGES_SHIFT;
    public static final long PIECE_NORTH_EDGE_MASK = NORTH_EDGE_MASK << PIECE_EDGES_SHIFT;
    public static final int PIECE_EAST_EDGE_SHIFT = EAST_EDGE_SHIFT + PIECE_EDGES_SHIFT;
    public static final long PIECE_EAST_EDGE_MASK = EAST_EDGE_MASK << PIECE_EDGES_SHIFT;
    public static final int PIECE_SOUTH_EDGE_SHIFT = SOUTH_EDGE_SHIFT + PIECE_EDGES_SHIFT;
    public static final long PIECE_SOUTH_EDGE_MASK = SOUTH_EDGE_MASK << PIECE_EDGES_SHIFT;
    public static final int PIECE_WEST_EDGE_SHIFT = WEST_EDGE_SHIFT + PIECE_EDGES_SHIFT;
    public static final long PIECE_WEST_EDGE_MASK = WEST_EDGE_MASK << PIECE_EDGES_SHIFT;

    public static final long BLANK_STATE;



    static {
        long state = setPiece(0L,-1);
        state = setRotation(state, 0);
        state = setPieceNorthEdge(state, EPieces.NULL);
        state = setPieceEastEdge(state, EPieces.NULL);
        state = setPieceSouthEdge(state, EPieces.NULL);
        state = setPieceWestEdge(state, EPieces.NULL);

        state = setDefinedOuterEdges(state, 0b0000);
        state = setNorthEdge(state, EPieces.NULL);
        state = setEastEdge(state, EPieces.NULL);
        state = setSouthEdge(state, EPieces.NULL);
        state = setWestEdge(state, EPieces.NULL);
        BLANK_STATE = state;
    }

    /**
     * @return hash based on which outer edges are set.
     */
    public static int getOuterHash(long state) {
        return getHash(getExistingOuterEdges(state), state);
    }
    public static int getHash(int setEdges, long edges) {
        switch (setEdges) {
            // We hope that the compiler is clever enough to see that these are all the numbers from 0-15 (inclusive)
            // and optimizes to a jump-switch

            case 0b0000: throw new IllegalArgumentException("The edges should never be zero when calculating hash");

            case 0b1000: return getNorthEdge(edges);
            case 0b0100: return getEastEdge(edges);
            case 0b0010: return getSouthEdge(edges);
            case 0b0001: return getWestEdge(edges);

            case 0b1100: return (int) ((edges >> (2 * EDGE_SHIFT)) & EDGE2_MASK);
            case 0b0110: return (int) ((edges >> EDGE_SHIFT) & EDGE2_MASK);
            case 0b0011: return (int) (edges & EDGE2_MASK);
            case 0b1001: return (getWestEdge(edges) << EDGE_SHIFT) | getNorthEdge(edges);

            case 0b1010: return (getNorthEdge(edges) << EDGE_SHIFT) | getSouthEdge(edges);
            case 0b0101: return (getEastEdge(edges) << EDGE_SHIFT) | getWestEdge(edges);

            case 0b1110: return (int) ((edges >> EDGE_SHIFT) & EDGE3_MASK);
            case 0b0111: return (int) (edges & EDGE3_MASK);
            case 0b1011: return (getSouthEdge(edges) << (2*EDGE_SHIFT)) | (getWestEdge(edges) << EDGE_SHIFT) | getNorthEdge(edges);
            case 0b1101: return (getEastEdge(edges) << (2*EDGE_SHIFT)) | (getNorthEdge(edges) << EDGE_SHIFT) | getEastEdge(edges);

            case 0b1111: return (int) (edges & EDGE4_MASK);
            default: throw new IllegalArgumentException("The edges should never be above 0b1111 (15) but was " + edges);
        }
    }
    
    
    /* Basic setters & getters below */

    public static long setPieceFull(long state, long piece, long rotation, long edges) {
        state = setPiece(state, piece);
        state = setRotation(state, rotation);
        state = setPieceAllEdges(state, edges);
        return state;
    }

    // Precedence chart:
    // https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
    
    /**
     * Sets piece ID only.
     */
    public static long setPiece(long state, long piece) {
        return ((piece << PIECE_SHIFT) & PIECE_MASK) | (state | ~PIECE_MASK);
    }
    public static int getPiece(long state) {
        long piece = (state & PIECE_MASK) >> PIECE_SHIFT;
        return (state & PIECE_MASK) == PIECE_MASK ? -1 : (int)piece;
    }
    public static boolean hasPiece(long state) {
        return (state & PIECE_MASK) != PIECE_MASK;
    }

    public static long setRotation(long state, long rotation) {
        return ((rotation << ROTATION_SHIFT) & ROTATION_MASK) | (state & ~ROTATION_MASK);
    }
    public static int getRotation(long state) {
        return (int) ((state & ROTATION_MASK) >> ROTATION_SHIFT);
    }

    public static long setPieceNorthEdge(long state, long edge) {
        return ((edge << PIECE_NORTH_EDGE_SHIFT) & PIECE_NORTH_EDGE_MASK) | (state & ~PIECE_NORTH_EDGE_MASK);
    }
    public static int getPieceNorthEdge(long state) {
        return (int) ((state & PIECE_NORTH_EDGE_MASK) >> PIECE_NORTH_EDGE_SHIFT);
    }
    public static long setPieceEastEdge(long state, long edge) {
        return ((edge << PIECE_EAST_EDGE_SHIFT) & PIECE_EAST_EDGE_MASK) | (state & ~PIECE_EAST_EDGE_MASK);
    }
    public static int getPieceEastEdge(long state) {
        return (int) ((state & PIECE_EAST_EDGE_MASK) >> PIECE_EAST_EDGE_SHIFT);
    }
    public static long setPieceSouthEdge(long state, long edge) {
        return ((edge << PIECE_SOUTH_EDGE_SHIFT) & PIECE_SOUTH_EDGE_MASK) | (state & ~PIECE_SOUTH_EDGE_MASK);
    }
    public static int getPieceSouthEdge(long state) {
        return (int) ((state & PIECE_SOUTH_EDGE_MASK) >> PIECE_SOUTH_EDGE_SHIFT);
    }
    public static long setPieceWestEdge(long state, long edge) {
        return ((edge << PIECE_WEST_EDGE_SHIFT) & PIECE_WEST_EDGE_MASK) | (state & ~PIECE_WEST_EDGE_MASK);
    }
    public static int getPieceWestEdge(long state) {
        return (int) ((state & PIECE_WEST_EDGE_MASK) >> PIECE_WEST_EDGE_SHIFT);
    }

    /**
     * @param edges all 4 piece edges (all are defined)
     */
    public static long setPieceAllEdges(long state, long edges) {
        return ((edges << PIECE_EDGES_SHIFT) & PIECE_EDGE4_MASK) | (state & ~PIECE_EDGE4_MASK);
    }
    /**
     * Alsp updates definedEdges bits.
     * @param edges 4 outer edges (might be -1)
     */
    public static long setAllEdges(long state, long edges) {
        state = (edges & EDGE4_MASK) | (state & ~EDGE4_MASK);
        return setDefinedOuterEdges(state, getDefinedEdges(edges));
    }
    
    @Deprecated
    public static long setAllEdges(long state, int north, int east, int south, int west) {
        state = setNorthEdge(state, north);
        state = setEastEdge(state, east);
        state = setSouthEdge(state, south);
        state = setWestEdge(state, west);
        long defined = (north == -1 ? 0 : 0b1000) +
                       (east ==  -1 ? 0 : 0b0100) +
                       (south == -1 ? 0 : 0b0010) +
                       (west ==  -1 ? 0 : 0b0001);

        return setDefinedOuterEdges(state, defined);
    }

    public static long setDefinedOuterEdges(long state, long edges) {
        return ((edges << EDGES_DEFINED_SHIFT) & EDGES_DEFINED_MASK) | (state & EDGES_DEFINED_MASK);
    }
    public static int getExistingOuterEdges(long state) {
        return (int) ((state & EDGES_DEFINED_MASK) >> EDGES_DEFINED_SHIFT);
    }
    
    public static long setNorthEdge(long state, long edge) {
        return ((edge << NORTH_EDGE_SHIFT) & NORTH_EDGE_MASK) | (state & ~NORTH_EDGE_MASK);
    }
    public static int getNorthEdge(long state) {
        return (int) ((state & NORTH_EDGE_MASK) >> NORTH_EDGE_SHIFT);
    }
    public static long setEastEdge(long state, long edge) {
        return ((edge << EAST_EDGE_SHIFT) & EAST_EDGE_MASK) | (state & ~EAST_EDGE_MASK);
    }
    public static int getEastEdge(long state) {
        return (int) ((state & EAST_EDGE_MASK) >> EAST_EDGE_SHIFT);
    }
    public static long setSouthEdge(long state, long edge) {
        return ((edge << SOUTH_EDGE_SHIFT) & SOUTH_EDGE_MASK) | (state & ~SOUTH_EDGE_MASK);
    }
    public static int getSouthEdge(long state) {
        return (int) ((state & SOUTH_EDGE_MASK) >> SOUTH_EDGE_SHIFT);
    }
    public static long setWestEdge(long state, long edge) {
        return ((edge << WEST_EDGE_SHIFT) & WEST_EDGE_MASK) | (state & ~WEST_EDGE_MASK);
    }
    public static int getWestEdge(long state) {
        return (int) ((state & WEST_EDGE_MASK) >> WEST_EDGE_SHIFT);
    }

    /**
     * Shift the {@code 4*<5 bits>} edges one edge to the right, moving the rightmost edge in front.
     * All other bits are kept in place.
     * @param edges
     * @return
     */
    public static long shiftEdgesRight(long edges) {
        long onlyEdges = edges & EDGE4_MASK;
        long nonEdges = edges & ~EDGE4_MASK;

        long shifted = onlyEdges >> EDGE_SHIFT;
        long rightmost = (onlyEdges & EDGE_MASK) << (3*EDGE_SHIFT);
        long finalEdges = shifted | rightmost;
        return nonEdges | finalEdges;
    }

    /**
     * @return bitmap of set edges: North, East, South, Vest
     */
    public static int getDefinedEdges(long edges) {
        return ((edges & NORTH_EDGE_MASK) != NORTH_EDGE_MASK ? 0b1000 : 0) |
               ((edges & EAST_EDGE_MASK)  != EAST_EDGE_MASK  ? 0b0100 : 0) |
               ((edges & SOUTH_EDGE_MASK) != SOUTH_EDGE_MASK ? 0b0010 : 0) |
               ((edges & WEST_EDGE_MASK)  != WEST_EDGE_MASK  ? 0b0001 : 0);
    }
}
