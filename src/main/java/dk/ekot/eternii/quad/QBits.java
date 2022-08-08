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
package dk.ekot.eternii.quad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.ekot.eternii.EPieces.QNULL_E;
import static dk.ekot.eternii.EPieces.QNULL_P;

/**
 * Handles quads (2x2 pieces)
 * Each edge is made up of 2 colors, each of those taking 5 bits (24 possible)
 * There are ~800K unique quads = 20 bits
 *
 * Not enough room in a single long, so we divide in inner (piece, rotation, piece edges colors)
 * and outer (defined edges, edge colors)
 *
 * qpieces (int)
 * {@code
 * <8 pieceID> upper left
 * <8 pieceID> upper right
 * <8 pieceID> lower right
 * <8 pieceID> lower left
 * }
 *
 * qinner (long), notice that there are no rotation of quads as all rotations are stored as
 * individual quads:
 * {@code
 * <4 unused>
 * <20 qpieceID>
 * <2*5 color> piece north edge
 * <2*5 color> piece east edge
 * <2*5 color> piece south edge
 * <2*5 color> piece west edge
 * }
 * 
 * qouter (long):
 * {@code
 * <20 unused>
 * <4 edges> 0b1000=north, 0b0100=east, 0b0010=south, 0b0001=west
 * <2*5 color> outer north edge
 * <2*5 color> outer east edge
 * <2*5 color> outer south edge
 * <2*5 color> outer west edge
 * }
 *
 */
public class QBits {
    private static final Logger log = LoggerFactory.getLogger(QBits.class);

    public static final int EDGE_SHIFT = 10;
    public static final long EDGE_MASK = ~(-1L<<EDGE_SHIFT);
    public static final long EDGE2_MASK = ~(-1L<<(2*EDGE_SHIFT));
    public static final long EDGE3_MASK = ~(-1L<<(3*EDGE_SHIFT));
    public static final long EDGE4_MASK = ~(-1L<<(4*EDGE_SHIFT));

    public static final int PIECE_SHIFT = (4*EDGE_SHIFT)+2;
    public static final long PIECE_MASK = (~(-1L<<20)) << PIECE_SHIFT;

    public static final int ROTATION_SHIFT = (4*EDGE_SHIFT);
    public static final long ROTATION_MASK = (~(-1L<<2)) << ROTATION_SHIFT;

    // Outer edges
    public static final int EDGES_DEFINED_SHIFT = EDGE_SHIFT*4;
    public static final long EDGES_DEFINED_MASK = (~(-1L << 4)) << EDGES_DEFINED_SHIFT;

    public static final int NORTH_EDGE_SHIFT = EDGE_SHIFT*3;
    public static final long NORTH_EDGE_MASK = EDGE_MASK << NORTH_EDGE_SHIFT;
    public static final int EAST_EDGE_SHIFT =  EDGE_SHIFT*2;
    public static final long EAST_EDGE_MASK =  EDGE_MASK  << EAST_EDGE_SHIFT;
    public static final int SOUTH_EDGE_SHIFT = EDGE_SHIFT;
    public static final long SOUTH_EDGE_MASK = EDGE_MASK << SOUTH_EDGE_SHIFT;
    public static final int WEST_EDGE_SHIFT =  0;
    public static final long WEST_EDGE_MASK =  EDGE_MASK;

    public static final long BLANK_INNER;
    public static final long BLANK_OUTER;

    static {
        long inner = setPiece(0L, QNULL_P);
        inner = setRotation(inner, 0);
        inner = setNorthEdge(inner, QNULL_E);
        inner = setEastEdge(inner, QNULL_E);
        inner = setSouthEdge(inner, QNULL_E);
        inner = setWestEdge(inner, QNULL_E);
        BLANK_INNER = inner;
    }
    static {
        long state = 0L;
        state = setDefinedEdges(state, 0b0000);
        state = setNorthEdge(state, QNULL_E);
        state = setEastEdge(state, QNULL_E);
        state = setSouthEdge(state, QNULL_E);
        state = setWestEdge(state, QNULL_E);
        BLANK_OUTER = state;
    }

    /**
     * @return hash based on which outer edges are set.
     */
    public static int getOuterHash(long state) {
        return getHash(getDefinedEdges(state), state);
    }
    public static int getHash(int defined, long edges) {
        switch (defined) {
            case 0b0000: return 0;

            // Not a real hash as max = 1,023
            case 0b1000: return getNorthEdge(edges);
            case 0b0100: return getEastEdge(edges);
            case 0b0010: return getSouthEdge(edges);
            case 0b0001: return getWestEdge(edges);

            // Real hash as max = 1,048,575
            case 0b1100: return Long.hashCode((edges >> (2 * EDGE_SHIFT)) & EDGE2_MASK);
            case 0b0110: return Long.hashCode((edges >> EDGE_SHIFT) & EDGE2_MASK);
            case 0b0011: return Long.hashCode(edges & EDGE2_MASK);
            case 0b1001: return Long.hashCode(((long) getWestEdge(edges) << EDGE_SHIFT) | getNorthEdge(edges));

            // Not a real hash as max = 1,048,575 (2^20-1)
            case 0b1010: return (getNorthEdge(edges) << EDGE_SHIFT) | getSouthEdge(edges);
            case 0b0101: return (getEastEdge(edges) << EDGE_SHIFT) | getWestEdge(edges);

            // Real hash as max = 1,073,741,823 (2^30-1)
            case 0b1110: return Long.hashCode((edges >> EDGE_SHIFT) & EDGE3_MASK);
            case 0b0111: return Long.hashCode(edges & EDGE3_MASK);
            case 0b1011: return Long.hashCode(((long) getSouthEdge(edges) << (2 * EDGE_SHIFT)) | ((long) getWestEdge(edges) << EDGE_SHIFT) | getNorthEdge(edges));
            case 0b1101: return Long.hashCode(((long) getWestEdge(edges) << (2 * EDGE_SHIFT)) | ((long) getNorthEdge(edges) << EDGE_SHIFT) | getEastEdge(edges));

            // Real hash as max = 1,099,511,627,775 (2^40-1)
            case 0b1111: return Long.hashCode(edges & EDGE4_MASK);
            default: throw new IllegalArgumentException("The edges should never be above 0b1111 (15) but was " + edges);
        }
    }

    /* Basic setters & getters below */

    public static long setPiece(long inner, long piece) {
        return ((piece << PIECE_SHIFT) & PIECE_MASK) | (inner & ~PIECE_MASK);
    }
    public static int getPiece(long inner) {
        return (int) ((inner & PIECE_MASK) >> PIECE_SHIFT);
    }
    public static boolean hasPiece(long inner) {
        return getPiece(inner) != QNULL_P;
    }

    public static long setRotation(long inner, long rotation) {
        return ((rotation << ROTATION_SHIFT) & ROTATION_MASK) | (inner & ~ROTATION_MASK);
    }
    public static int getRotation(long inner) {
        return (int) ((inner & ROTATION_MASK) >> ROTATION_SHIFT);
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
     * @param edges all 4 piece edges (all are defined), where the input edges are positioned to the right.
     */
    public static long setAllPieceEdges(long inner, long edges) {
        return (edges & EDGE4_MASK) | (inner & ~EDGE4_MASK);
    }
    public static long getAllEdges(long state) {
        return state & EDGE4_MASK;
    }
    /**
     * Also updates definedEdges bits.
     * @param edges 4 outer edges (might be QNULL_E)
     */
    public static long setAllOuterEdges(long outer, long edges) {
        outer = (edges & EDGE4_MASK) | (outer & ~EDGE4_MASK);
        return setDefinedEdges(outer, deriveDefinedEdges(edges));
    }

    /**
     * Outer edges that are set.
     */
    public static long setDefinedEdges(long outer, long definedEdges) {
        return ((definedEdges << EDGES_DEFINED_SHIFT) & EDGES_DEFINED_MASK) | (outer & ~EDGES_DEFINED_MASK);
    }
    public static int getDefinedEdges(long outer) {
        return (int) ((outer & EDGES_DEFINED_MASK) >> EDGES_DEFINED_SHIFT);
    }
    public static int countDefinedEdges(long outer) {
        return Long.bitCount(outer & EDGES_DEFINED_MASK);
    }
    public static long updateDefinedEdges(long outer) {
        return setDefinedEdges(outer, deriveDefinedEdges(outer));
    }
    /**
     * @return bitmap of set edges: North, East, South, Vest
     */
    public static int deriveDefinedEdges(long edges) {
        return (getNorthEdge(edges) != QNULL_E  ? 0b1000 : 0) |
               (getEastEdge(edges)  != QNULL_E  ? 0b0100 : 0) |
               (getSouthEdge(edges) != QNULL_E  ? 0b0010 : 0) |
               (getWestEdge(edges)  != QNULL_E  ? 0b0001 : 0);
    }

    /**
     * Shift the {@code 4*<2*5 bits>} edges one edge to the right, moving the rightmost edge in front.
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

    public static String toStringInner(long state) {
        return toString(state, new int[]{2, 20, 2, 10, 10, 10, 10});
    }
    public static String toStringouter(long state) {
        return toString(state, new int[]{20, 4, 10, 10, 10, 10});
    }

    private static String toString(long state, int[] split) {
        String bin = Long.toBinaryString(state);
        while (bin.length() < 64) {
            bin = "0" + bin;
        }
        String form = "";
        for (int len: split) {
            form += bin.substring(0, len) + " ";
            bin = bin.substring(len);
        }
        return form + bin;
    }

}
