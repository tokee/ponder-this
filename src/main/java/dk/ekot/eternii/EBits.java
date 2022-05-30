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
    public static final long PIECE_MASK = ~(1L<<9);

    public static final int ROTATION_SHIFT = 52;
    public static final long ROTATION_MASK = ~(1L<<2);

    public static final int PIECE_EDGES_SHIFT = 32;

    public static final int OUTER_EDGES_SHIFT = 20;
    public static final long OUTER_EDGES_MASK = ~(1L<<4);
    
    public static final int EDGE_SHIFT = 5; // Basic edge shift
    public static final long EDGE_MASK = ~(1L<<5);
    public static final long EDGE2_MASK = ~(1L<<10);
    public static final long EDGE3_MASK = ~(1L<<15);
    public static final long EDGE4_MASK = ~(1L<<20);

    public static final int NORTH_EDGE_SHIFT = EDGE_SHIFT*3;
    public static final int EAST_EDGE_SHIFT = EDGE_SHIFT*2;
    public static final int SOUTH_EDGE_SHIFT = EDGE_SHIFT;
    public static final int WEST_EDGE_SHIFT = 0;

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
    
    public static long setPiece(long state, long piece) {
        return ((piece & PIECE_MASK) << PIECE_SHIFT) | state;
    }
    public static int getPiece(long state) {
        long piece = (state >> PIECE_SHIFT) & PIECE_MASK;
        return piece == PIECE_MASK ? -1 : (int)piece;
    }
    
    public static long setRotation(long state, long rotation) {
        return ((rotation & ROTATION_MASK) << ROTATION_SHIFT) | state;
    }
    public static int getRotation(long state) {
        return (int) ((state >> ROTATION_SHIFT) & ROTATION_MASK);
    }

    public static long setPieceNorthEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << (PIECE_EDGES_SHIFT + NORTH_EDGE_SHIFT)) | state;
    }
    public static int getPieceNorthEdge(long state) {
        return (int) ((state >> (PIECE_EDGES_SHIFT + NORTH_EDGE_SHIFT)) & EDGE_MASK);
    }
    public static long setPieceEastEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << (PIECE_EDGES_SHIFT + EAST_EDGE_SHIFT)) | state;
    }
    public static int getPieceEastEdge(long state) {
        return (int) ((state >> (PIECE_EDGES_SHIFT + EAST_EDGE_SHIFT)) & EDGE_MASK);
    }
    public static long setPieceSouthEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << (PIECE_EDGES_SHIFT + SOUTH_EDGE_SHIFT)) | state;
    }
    public static int getPieceSouthEdge(long state) {
        return (int) ((state >> (PIECE_EDGES_SHIFT + SOUTH_EDGE_SHIFT)) & EDGE_MASK);
    }
    public static long setPieceWestEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << (PIECE_EDGES_SHIFT + WEST_EDGE_SHIFT)) | state;
    }
    public static int getPieceWestEdge(long state) {
        return (int) ((state >> (PIECE_EDGES_SHIFT + WEST_EDGE_SHIFT)) & EDGE_MASK);
    }
    
    public static long setOuterEdges(long state, long edges) {
        return ((edges & OUTER_EDGES_MASK) << OUTER_EDGES_SHIFT) | state;
    }
    public static int getExistingOuterEdges(long state) {
        return (int) ((state >> OUTER_EDGES_SHIFT) & OUTER_EDGES_MASK);
    }
    
    public static long setNorthEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << NORTH_EDGE_SHIFT) | state;
    }
    public static int getNorthEdge(long state) {
        return (int) ((state >> NORTH_EDGE_SHIFT) & EDGE_MASK);
    }
    public static long setEastEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << EAST_EDGE_SHIFT) | state;
    }
    public static int getEastEdge(long state) {
        return (int) ((state >> EAST_EDGE_SHIFT) & EDGE_MASK);
    }
    public static long setSouthEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << SOUTH_EDGE_SHIFT) | state;
    }
    public static int getSouthEdge(long state) {
        return (int) ((state >> SOUTH_EDGE_SHIFT) & EDGE_MASK);
    }
    public static long setWestEdge(long state, long edge) {
        return ((edge & EDGE_MASK) << WEST_EDGE_SHIFT) | state;
    }
    public static int getWestEdge(long state) {
        return (int) ((state >> WEST_EDGE_SHIFT) & EDGE_MASK);
    }

    /**
     * Shift the {@code 4*<5 bits>} edges one edge to the right, moving the rightmost edge in front.
     * All other bits are kept in place.
     * @param edges
     * @return
     */
    public static long shiftEdgesRight(long edges) {
        long onlyEdges = edges & EDGE4_MASK;
        long shifted = onlyEdges >> EDGE_SHIFT;
        long rightmost = (onlyEdges & EDGE_MASK) << (3*EDGE_SHIFT);
        long finalEdges = shifted | rightmost;
        return ((edges >> (4*EDGE_SHIFT)) << (4*EDGE_SHIFT)) | finalEdges;
    }
}
