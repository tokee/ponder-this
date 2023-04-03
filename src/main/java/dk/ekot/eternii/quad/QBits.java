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

import dk.ekot.eternii.EPieces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.ekot.eternii.EPieces.QNULL_E;
import static dk.ekot.eternii.EPieces.QNULL_P;

/**
 * Handles quads (2x2 pieces)
 * There are 256 pieces, each with 4 edges.
 * Each qedge is made up of 2 colors from the piece edges, each of those taking 5 bits (24 possible)
 * There are ~800K unique quads, ~4 times as many with rotations.
 *
 * qpieces (int) are represented as compact as possible for quick iteration.
 * {@code
 * <8 pieceID> piece NW
 * <8 pieceID> piece NE
 * <8 pieceID> piece SE
 * <8 pieceID> piece SW
 * }
 *
 * qedges (int),<br/>
 * Colors are presented clockwise, viewed from the center of the quad.<br/>
 * Colors does not follow pieces: col N 1 is the north color of piece NW, but col N 2 is the north color of piece NE.<br/>
 * Notice that for two quad edges to align, the 2 colors on their edges must be compared using cross over.<br/>
 * Notice that there are no rotation of quads as all rotations are stored as individual quads.<br/>
 * General rotation: 0=north, 1=east, 2=south, 3=west.
 * {@code
 * <16 unused>
 * <2 rotation> rot NW
 * <2 rotation> rot NE
 * <2 rotation> rot SE
 * <2 rotation> rot SW
 * <5 color> col N 1
 * <5 color> col N 2
 * <5 color> col E 1
 * <5 color> col E 2
 * <5 color> col S 1
 * <5 color> col S 2
 * <5 color> col W 1
 * <5 color> col W 2
 * }
 */
public class QBits {

    public static final EPieces ETERNII = EPieces.getEternii();

    public static final long PIECE_MASK = 0b11111111L; // 8 bit

    public static final long PIECE_NW_SHIFT = 24;
    public static final long PIECE_NW_MASK = PIECE_MASK << PIECE_NW_SHIFT;
    public static final long PIECE_NE_SHIFT = 16;
    public static final long PIECE_NE_MASK = PIECE_MASK << PIECE_NE_SHIFT;
    public static final long PIECE_SE_SHIFT = 8;
    public static final long PIECE_SE_MASK = PIECE_MASK << PIECE_SE_SHIFT;
    public static final long PIECE_SW_SHIFT = 0;
    public static final long PIECE_SW_MASK = PIECE_MASK << PIECE_SW_SHIFT;

    public static final long ROT_MASK = 0b11L; // 2 bit

    public static final long ROT_NW_SHIFT = 46;
    public static final long ROT_NW_MASK = ROT_MASK << ROT_NW_SHIFT;
    public static final long ROT_NE_SHIFT = 44;
    public static final long ROT_NE_MASK = ROT_MASK << ROT_NE_SHIFT;
    public static final long ROT_SE_SHIFT = 42;
    public static final long ROT_SE_MASK = ROT_MASK << ROT_SE_SHIFT;
    public static final long ROT_SW_SHIFT = 40;
    public static final long ROT_SW_MASK = ROT_MASK << ROT_SW_SHIFT;

    public static final long COL_MASK = 0b11111L; // 5 bit

    /**
     * @return hash based on which outer edges are set.
     */
/*    public static int getOuterHash(int state) {
        return getHash(getDefinedEdges(state), state);
    }
    public static int getHash(int defined, int edges) {
        switch (defined) {
            case 0b0000: return 0;

            // Not a real hash as max = 1,023
            case 0b1000: return getNorthEdge(edges);
            case 0b0100: return getEastEdge(edges);
            case 0b0010: return getSouthEdge(edges);
            case 0b0001: return getWestEdge(edges);

            // Real hash as max = 1,048,575
            case 0b1100: return int.hashCode((edges >>> (2 * EDGE_SHIFT)) & EDGE2_MASK);
            case 0b0110: return int.hashCode((edges >>> EDGE_SHIFT) & EDGE2_MASK);
            case 0b0011: return int.hashCode(edges & EDGE2_MASK);
            case 0b1001: return int.hashCode(((int) getWestEdge(edges) << EDGE_SHIFT) | getNorthEdge(edges));

            // Not a real hash as max = 1,048,575 (2^20-1)
            case 0b1010: return (getNorthEdge(edges) << EDGE_SHIFT) | getSouthEdge(edges);
            case 0b0101: return (getEastEdge(edges) << EDGE_SHIFT) | getWestEdge(edges);

            // Real hash as max = 1,073,741,823 (2^30-1)
            case 0b1110: return int.hashCode((edges >>> EDGE_SHIFT) & EDGE3_MASK);
            case 0b0111: return int.hashCode(edges & EDGE3_MASK);
            case 0b1011: return int.hashCode(((int) getSouthEdge(edges) << (2 * EDGE_SHIFT)) | ((int) getWestEdge(edges) << EDGE_SHIFT) | getNorthEdge(edges));
            case 0b1101: return int.hashCode(((int) getWestEdge(edges) << (2 * EDGE_SHIFT)) | ((int) getNorthEdge(edges) << EDGE_SHIFT) | getEastEdge(edges));

            // Real hash as max = 1,099,511,627,775 (2^40-1)
            case 0b1111: return int.hashCode(edges & EDGE4_MASK);
            default: throw new IllegalArgumentException("The edges should never be above 0b1111 (15) but was " + edges);
        }
    }
  */

    public static int createQPiece(int nwP, int neP, int seP, int swP) {
        return (nwP << 24) | (neP << 16) | (seP << 8) | swP;
    }
    public static long createQEdges(int nwRot, int neRot, int seRot, int swRot,
                                    int nwP, int neP, int seP, int swP) {
        return (((long)nwRot) << 46) | (((long)neRot) << 44) | (((long)seRot) << 42) | (((long)swRot) << 40) |
               (((long)ETERNII.getTop(   nwP, nwRot)) << 35) |
               (((long)ETERNII.getTop(   neP, neRot)) << 30) |
               (((long)ETERNII.getRight( neP, neRot)) << 25) |
               (((long)ETERNII.getRight( seP, seRot)) << 20) |
               (((long)ETERNII.getBottom(seP, seRot)) << 15) |
               (((long)ETERNII.getBottom(swP, swRot)) << 10) |
               (((long)ETERNII.getLeft(  swP, swRot)) <<  5) |
                ((long)ETERNII.getLeft(  nwP, nwRot));
    }

    /**
     * Rotate a qpiece clockwise.
     */
    public static int rotQPieceClockwise(int qpiece) {
        return  (qpiece >>> 8) |         // piece NW + NE + SE
               ((qpiece & 0xFF) << 24); // piece SW
    }
    public static int rotQPieceCounterClockwise(int qpiece) {
        return  (qpiece << 8) |         // piece NW + SE + SW
               ((qpiece >>> 24) & 0xFF); // piece NW
    }
    public static long rotQEdgesClockwise(long qedges) {
        return
               // Extract= ((qedges >>> 46) & 0b11L)
               // Rotate = (oldrot+1) & 0b11L
               // Assign = newrot << 44
                (((((qedges >>> 46) & 0b11L)+1) & 0b11L) << 44) | // rot NW -> NE
                (((((qedges >>> 44) & 0b11L)+1) & 0b11L) << 42) | // rot NE -> SE
                (((((qedges >>> 42) & 0b11L)+1) & 0b11L) << 40) | // rot SE -> SW
                (((((qedges >>> 40) & 0b11L)+1) & 0b11L) << 46) | // rot SW -> NW
                  (((qedges >>> 10) & 0b11111_11111__11111_11111__11111_11111L)) | // col NW + NE + SE
                   ((qedges & 0b11111_11111L) << 30); // col SW
    }
    public static long rotQEdgesCounterClockwise(long qedges) {
        return
               // Extract= ((qedges >>> 46) & 0b11L)
               // Rotate = (oldrot+1) & 0b11L
               // Assign = newrot << 44
                (((((qedges >>> 44) & 0b11L)+1) & 0b11L) << 46) | // rot NE -> NW
                (((((qedges >>> 42) & 0b11L)+1) & 0b11L) << 44) | // rot SE -> NE
                (((((qedges >>> 40) & 0b11L)+1) & 0b11L) << 42) | // rot SW -> SW
                (((((qedges >>> 46) & 0b11L)+1) & 0b11L) << 40) | // rot NW -> SW
                  (((qedges << 10) & 0b11111_11111__11111_11111__11111_11111_00000_00000L)) | // col NE + SE + SW
                   ((qedges >>> 30) & 0b11111_11111L); // col NW
    }

    public static int getPieceNW(int qpiece) {
        return qpiece >>> 24;
    }
    public static int getPieceNE(int qpiece) {
        return (qpiece >>> 16) & 0b11111111;
    }
    public static int getPieceSE(int qpiece) {
        return (qpiece >>> 8) & 0b11111111;
    }
    public static int getPieceSW(int qpiece) {
        return qpiece & 0b11111111;
    }

    // Rotations for the 4 pieces making up the quad
    public static int getRotNW(long qedges) {
        return (int) ((qedges >>> 46) & 0b11L);
    }
    public static int getRotNE(long qedges) {
        return (int) ((qedges >>> 44) & 0b11L);
    }
    public static int getRotSE(long qedges) {
        return (int) ((qedges >>> 42) & 0b11L);
    }
    public static int getRotSW(long qedges) {
        return (int) ((qedges >>> 40) & 0b11L);
    }

    // Direct colors, as seen from the center of the quad, clockwise
    public static int getColN(long qedges) {
        return (int) ((qedges >>> 30) & 0b11111_11111L);
    }
    public static int getColE(long qedges) {
        return (int) ((qedges >>> 20) & 0b11111_11111L);
    }
    public static int getColS(long qedges) {
        return (int) ((qedges >>> 10) & 0b11111_11111L);
    }
    public static int getColW(long qedges) {
        return (int) (qedges & 0b11111_11111L);
    }

    // Inverse colors, as seen from outside of the quad, counterclockwise
    public static int getColInvN(long qedges) {
        return (int)  (((qedges >>> 35) & 0b11111) |
                      (((qedges >>> 30) & 0b11111)) << 5);
    }
    public static int getColInvE(long qedges) {
        return (int)  (((qedges >>> 25) & 0b11111) |
                      (((qedges >>> 20) & 0b11111)) << 5);
    }
    public static int getColInvS(long qedges) {
        return (int)  (((qedges >>> 15) & 0b11111) |
                      (((qedges >>> 10) & 0b11111)) << 5);
    }
    public static int getColInvW(long qedges) {
        return (int)  (((qedges >>> 5) & 0b11111) |
                      ((qedges & 0b11111)) << 5);
    }

    public static String toStringFull(int qpiece, long qedges) {
        return   "nw " + ETERNII.toDisplayString(getPieceNW(qpiece), getRotNW(qedges)) +
               ", ne " + ETERNII.toDisplayString(getPieceNE(qpiece), getRotNE(qedges)) +
               ", se " + ETERNII.toDisplayString(getPieceSE(qpiece), getRotSW(qedges)) +
               ", sw " + ETERNII.toDisplayString(getPieceSW(qpiece), getRotSW(qedges));
    }
    public static String toStringQPiece(int qpiece) {
        return toString(qpiece, new int[]{8, 8, 8, 8});
    }
    public static String toStringQEdges(long qedges) {
        return toString(qedges, new int[]{16, 2, 2, 2, 2, 5, 5, 5, 5, 5, 5, 5, 5});
    }
    private static String toString(int state, int[] split) {
        return toString(state, 32, split);
    }
    private static String toString(long state, int[] split) {
        return toString(state, 64, split);
    }
    private static String toString(long state, int significantBits, int[] split) {
        String bin = Long.toBinaryString(state);
        while (bin.length() < significantBits) {
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
