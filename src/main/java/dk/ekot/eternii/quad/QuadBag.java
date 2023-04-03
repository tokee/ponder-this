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

import dk.ekot.misc.Bitmap;
import dk.ekot.misc.GrowableBitmap;
import dk.ekot.misc.GrowableInts;
import dk.ekot.misc.GrowableLongs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Holds Quads, packed as ints & longs as defined in {@link QBits}.
 * <p>
 * Once constructed, QuadBag pieces and edges are immutable and can be shared between threads.
 * <p>
 * Quad availability is controlled by {@link #existing} which is NOT shared between threads.
 */
public class QuadBag {
    private static final Logger log = LoggerFactory.getLogger(QuadBag.class);

    public static final long MAX_PCOL = 27;
    public static final long MAX_QCOL_EDGE1 = MAX_PCOL * MAX_PCOL;
    public static final long MAX_QCOL_EDGE2 = MAX_QCOL_EDGE1 * MAX_QCOL_EDGE1;
    public static final long MAX_QCOL_EDGE3 = MAX_QCOL_EDGE2 * MAX_QCOL_EDGE1;
    public static final long MAX_QCOL_EDGE4 = MAX_QCOL_EDGE3 * MAX_QCOL_EDGE1;

    private final PieceMap pieceMap;
    private final BAG_TYPE bagType;
    // n=0b1000, e=0b0100, s=0b0010, w=0b0001
    private final QuadMap[] qmaps = new QuadMap[16];

    public enum BAG_TYPE {
        corner_nw, corner_ne, corner_se, corner_sw, 
        border_n, border_e, border_s, border_w, 
        clue_nw, clue_ne, clue_se, clue_sw, clue_c,
        inner;

        /**
         * Border and corner bags have 1 or 2 fixed edges that are always the same (grey).
         * @return which edges of the bag that are NOT fixed, with notation n=0b1000, e=0b0100, s=0b0010, w=0b0001
         */
        public int variableEdges() {
            switch (this) {
                case corner_nw: return 0b0110;
                case corner_ne: return 0b0011;
                case corner_se: return 0b1001;
                case corner_sw: return 0b1100;
                case border_n:  return 0b0111;
                case border_e:  return 0b1011;
                case border_s:  return 0b1101;
                case border_w:  return 0b1110;
                case clue_nw:
                case clue_ne:
                case clue_se:
                case clue_sw:
                case clue_c:
                case inner:
                    return 0b1111;
                default: throw new IllegalStateException("Unknown BAG_TYPE: " + this);
            }
        }
    }
    
    /**
     * Number of Quads in this set.
     */
    private int size = 0;

    private GrowableInts qpieces;
    private GrowableLongs qedges;
    private Bitmap snapshot = null;
    private Bitmap existing;

    public QuadBag(PieceMap pieceMap, BAG_TYPE bagType) {
        this.pieceMap = pieceMap;
        this.bagType = bagType;
        qpieces = new GrowableInts();
        qedges = new GrowableLongs();
        existing = new GrowableBitmap(0);
    }

    public QuadBag(PieceMap pieceMap, BAG_TYPE bagType,
                   GrowableInts qpieces, GrowableLongs qedges, GrowableBitmap existing) {
        this.pieceMap = pieceMap;
        this.bagType = bagType;
        this.qpieces = qpieces;
        this.qedges = qedges;
        this.existing = existing;
    }

    /**
     * Get the quad map corresponding to the given colors.
     * The colors are extracted using e.g. {@link QBits#getColN} for the quad in the given direction.
     * If there are no quad, {@code -1} is given.
     *
     * The method automatically ignores irrelevant edges, e.g. the north edge if bagType == border_n.
     * @return the quad set for the given defined border colors.
     */
    public QuadMap getMap(int colN, int colE, int colS, int colW) {
        return getMapReduced(bagType == BAG_TYPE.border_n || bagType == BAG_TYPE.corner_nw ? -1 : colN,
                             bagType == BAG_TYPE.border_e || bagType == BAG_TYPE.corner_ne ? -1 : colE,
                             bagType == BAG_TYPE.border_s || bagType == BAG_TYPE.corner_se ? -1 : colS,
                             bagType == BAG_TYPE.border_w || bagType == BAG_TYPE.corner_sw ? -1 : colW);
    }

    private QuadMap getMapReduced(int colN, int colE, int colS, int colW) {
        final int defined = (colN != -1 ? 0b1000 : 0b000) |
                            (colE != -1 ? 0b0100 : 0b000) |
                            (colS != -1 ? 0b0010 : 0b000) |
                            (colW != -1 ? 0b0001 : 0b000);
        return qmaps[defined];
    }

    /**
     * Trim the holding structures down to size, without any room for further quads.
     */
    public QuadBag trim() {
        qpieces = qpieces.trimCopy();
        qedges = qedges.trimCopy();
        //verifyUnique();
        generateSets();
        return this;
    }

    /**
     * Iterates quads is this bag, ensuring uniqueness.
     * Slow and with high memory overhead.
     */
    private void verifyUnique() {
        log.info("Verifying uniqueness of quad bag of type " + bagType);
        Set<String> encountered = new HashSet<>();
        for (int i = 0 ; i < size() ; i++) {
            String quad = qpieces.get(i) + "_" + qedges.get(i);
            if (!encountered.add(quad)) {
                throw new IllegalStateException(
                        "Internal error: The QuadBag of type " + bagType + " had a duplicate entry at index " + i +
                        ": " + QBits.toStringFull(qpieces.get(i), qedges.get(i)));
            }
        }
    }

    /**
     * Add a Quad. No checking for duplicates!
     * @param qedges as defined in {@link QBits}.
     */
    public void addQuad(int qpiece, long qedges) {
        qpieces.add(qpiece);
        this.qedges.add(qedges);
        existing.set(size++);
//        if (c++ < 4) {
//            System.out.println("Add " + QBits.toStringFull(qpiece, qedges));
//        }
    }
//    int c = 0;

    public int size() {
        return size;
    }

    /**
     * Remove all Quads which contains any of the marked pieces.
     * @param pieceMask an array of length 256, each entry representing the piece with index as ID.
     *                 0 means the piece should be removed, 1 means it should be kept.
     * @return true if {@code need <= size}, else false.
     */
    public void hideQPieces(byte[] pieceMask) {
        for (int i = 0 ; i < size ; i++) {
            final int pieceIDs = qpieces.get(i);
            if (pieceMask[pieceIDs & 0xFF] +
                pieceMask[(pieceIDs >> 8) & 0xFF] +
                pieceMask[(pieceIDs >> 16) & 0xFF] +
                pieceMask[(pieceIDs >> 24) & 0xFF] != 4 &&
                existing.get(i)) {
                existing.clear(i);
            }
        }
    }

    /**
     * Add all Quads which contains any of the marked pieces.
     * @param pieceMask an array of length 256, each entry representing the piece with index as ID.
     *                 0 means the piece should be removed, 1 means it should be kept.
     * @return true if {@code need <= size}, else false.
     */
    public void showQPieces(byte[] pieceMask) {
        for (int i = 0 ; i < size ; i++) {
            final int pieceIDs = qpieces.get(i);
            if (pieceMask[pieceIDs & 0xFF] +
                pieceMask[(pieceIDs >> 8) & 0xFF] +
                pieceMask[(pieceIDs >> 16) & 0xFF] +
                pieceMask[(pieceIDs >> 24) & 0xFF] == 4 &&
                !existing.get(i)) {
                existing.set(i);
            }
        }
    }

    /**
     * Take a snapshot of marked/unmarked quads.
     * @see #rollback()
     */
    public void snapshot() {
        if (snapshot == null || snapshot.size() < size) {
            snapshot = new Bitmap(size);
        }
        existing.copy(snapshot);
    }

    /**
     * Restore to last snapshot.
     * @see #snapshot()
     */
    public void rollback() {
        if (snapshot == null) {
            throw new IllegalStateException("Snapshot has not been performed");
        }
        snapshot.copy(existing);
    }

    public void recalculateQPieces() {
        recalculateQPieces(pieceMap.pieceIDByteMap, 0, calcBlock(qedges.size()));
    }

    public void recalculateQPiecesParallel() {
        final int segmentBlocks = 512;
        int endBlock = calcBlock(qedges.size());
        int segments = endBlock/segmentBlocks;
        if (segments*segmentBlocks < endBlock) {
            ++segments;
        }
        int[] startBlocks = new int[segments+1];
        for (int i = 0 ; i < segments ; i++) {
            startBlocks[i] = i*segmentBlocks;
        }
        final int lastStartBlock = startBlocks[segments-1];
        Arrays.stream(startBlocks).parallel().forEach(startBlock -> {
            int localEndBlock = startBlock == lastStartBlock ? endBlock+1 : startBlock+segmentBlocks;
            recalculateQPieces(pieceMap.pieceIDByteMap, startBlock, localEndBlock);
        });
    }

    private int calcBlock(int pos) {
        int blocks = pos >>> 6;
        if (blocks << 6 < pos) {
            return blocks+1;
        }
        return blocks;
    }

    /**
     * Mark all qpieces as live or dead.
     * @param pieceMask an array of length 256, each entry representing the piece with index as ID.
     *                 0 means the piece should be removed, 1 means it should be kept.
     * @param startBlock first block (of 64 pieceID bits), inclusive.
     * @param endBlock last block (of 64 pieceID bits), exclusive.
     */
    private void recalculateQPieces(byte[] pieceMask, int startBlock, int endBlock) {
        long[] blocks = existing.getBacking();
        for (int blockIndex = startBlock ; blockIndex < endBlock ; blockIndex++) {
            long block = 0L;
            for (int i = 0 ; i < 64 ; i++) {
                int id = (blockIndex << 6) + i;
                final int pieceIDs = qpieces.get(id);
                // TODO: Sanity check this hack. Why should it sum to exactly 4?
                block |= (pieceMask[pieceIDs & 0xFF] +
                          pieceMask[(pieceIDs >> 8) & 0xFF] +
                          pieceMask[(pieceIDs >> 16) & 0xFF] +
                          pieceMask[(pieceIDs >> 24) & 0xFF]) >> 2; // 4 -> 1
                block = block << 1;
            }
            blocks[blockIndex] = block;
        }
/*        // Last block might not be full
        long block = 0;
        for (int i = 0 ; i < 64 ; i++) {
            int id = ((endBlock-1) << 6) + i;
            if (id < size) {
                final int pieceIDs = qpieces.get(id);
                block |= (pieceMask[pieceIDs & 0xFF] +
                          pieceMask[(pieceIDs >> 8) & 0xFF] +
                          pieceMask[(pieceIDs >> 16) & 0xFF] +
                          pieceMask[(pieceIDs >> 24) & 0xFF]) >> 2; // 4 -> 1
            }
            block = block << 1;
        }
        blocks[endBlock-1] = block;

  */
/*        for (int i = 0 ; i < size ; i++) {
            final int pieceIDs = qpieces.get(i);
            boolean live = pieceMask[pieceIDs & 0xFF] +
                           pieceMask[(pieceIDs >> 8) & 0xFF] +
                           pieceMask[(pieceIDs >> 16) & 0xFF] +
                           pieceMask[(pieceIDs >> 24) & 0xFF] == 4;
            if (live) {
                existing.set(i);
            } else {
                existing.clear(i);
            }
        } */
    }

    /**
     * Creates a new QuadBag from this by rotating all pieces 90 degrees clockwise.
     */
    public QuadBag rotClockwise() {
        GrowableInts rotQpieces = qpieces.trimCopy();
        for (int i = 0 ; i < rotQpieces.size() ; i++) {
            rotQpieces.rawInts()[i] = QBits.rotQPieceClockwise(rotQpieces.rawInts()[i]);
        }
        GrowableLongs rotQEdges = qedges.trimCopy();
        for (int i = 0 ; i < rotQEdges.size() ; i++) {
            rotQEdges.rawLongs()[i] = QBits.rotQEdgesClockwise(rotQEdges.rawLongs()[i]);
        }
        GrowableBitmap blankExisting = new GrowableBitmap(existing.size());
        BAG_TYPE newType;
        switch (bagType) {
            case corner_nw:
                newType = BAG_TYPE.corner_ne;
                break;
            case corner_ne:
                newType = BAG_TYPE.corner_se;
                break;
            case corner_se:
                newType = BAG_TYPE.corner_sw;
                break;
            case corner_sw:
                newType = BAG_TYPE.corner_nw;
                break;
            case border_n:
                newType = BAG_TYPE.border_e;
                break;
            case border_e:
                newType = BAG_TYPE.border_s;
                break;
            case border_s:
                newType = BAG_TYPE.border_w;
                break;
            case border_w:
                newType = BAG_TYPE.border_n;
                break;
            case inner:
                newType = BAG_TYPE.inner;
                break;
            default: throw new IllegalStateException("Unable to rotate QuadBag of type " + bagType);
        };
        return new QuadBag(pieceMap, newType, rotQpieces, rotQEdges, blankExisting);
    }

    public int getQPiece(int index) {
        return qpieces.get(index);
    }
    public long getQEdges(int index) {
        return qedges.get(index);
    }

    public void generateSets() {
        // TODO: Ensure trimmed growables!
        log.info("Generating sets for QuadBag of type " + bagType);

        generateSet(0b1000, MAX_QCOL_EDGE1, qedges -> (long) QBits.getColInvN(qedges));
        generateSet(0b0100, MAX_QCOL_EDGE1, qedges -> (long) QBits.getColInvE(qedges));
        generateSet(0b0010, MAX_QCOL_EDGE1, qedges -> (long) QBits.getColInvS(qedges));
        generateSet(0b0001, MAX_QCOL_EDGE1, qedges -> (long) QBits.getColInvW(qedges));

        generateSet(0b1100, MAX_QCOL_EDGE2,
                    qedges -> QBits.getColInvN(qedges)*MAX_QCOL_EDGE1 + QBits.getColInvE(qedges));
        generateSet(0b0110, MAX_QCOL_EDGE2,
                    qedges -> QBits.getColInvE(qedges)*MAX_QCOL_EDGE1 + QBits.getColInvS(qedges));
        generateSet(0b0011, MAX_QCOL_EDGE2,
                    qedges -> QBits.getColInvS(qedges)*MAX_QCOL_EDGE1 + QBits.getColInvW(qedges));
        generateSet(0b1001, MAX_QCOL_EDGE2,
                    qedges -> QBits.getColInvW(qedges)*MAX_QCOL_EDGE1 + QBits.getColInvN(qedges));

        generateSet(0b1010, MAX_QCOL_EDGE2,
                    qedges -> QBits.getColInvN(qedges)*MAX_QCOL_EDGE1 + QBits.getColInvS(qedges));
        generateSet(0b0101, MAX_QCOL_EDGE2,
                    qedges -> QBits.getColInvE(qedges)*MAX_QCOL_EDGE1 + QBits.getColInvW(qedges));

        generateSet(0b1110, MAX_QCOL_EDGE3,
                    qedges -> QBits.getColInvN(qedges)*MAX_QCOL_EDGE2 +
                              QBits.getColInvE(qedges)*MAX_QCOL_EDGE1 +
                              QBits.getColInvS(qedges));
        generateSet(0b0111, MAX_QCOL_EDGE3,
                    qedges -> QBits.getColInvE(qedges)*MAX_QCOL_EDGE2 +
                              QBits.getColInvS(qedges)*MAX_QCOL_EDGE1 +
                              QBits.getColInvW(qedges));
        generateSet(0b1011, MAX_QCOL_EDGE3,
                    qedges -> QBits.getColInvS(qedges)*MAX_QCOL_EDGE2 +
                              QBits.getColInvW(qedges)*MAX_QCOL_EDGE1 +
                              QBits.getColInvN(qedges));
        generateSet(0b1101, MAX_QCOL_EDGE3,
                    qedges -> QBits.getColInvW(qedges)*MAX_QCOL_EDGE2 +
                              QBits.getColInvN(qedges)*MAX_QCOL_EDGE1 +
                              QBits.getColInvE(qedges));

        generateSet(0b1111, MAX_QCOL_EDGE4,
                    qedges -> QBits.getColInvN(qedges)*MAX_QCOL_EDGE3 +
                              QBits.getColInvE(qedges)*MAX_QCOL_EDGE2 +
                              QBits.getColInvS(qedges)*MAX_QCOL_EDGE1 +
                              QBits.getColInvW(qedges));
    }

    private void generateSet(int wantedEdges, long maxHash, Function<Long, Long> hasher) {
        if ((bagType.variableEdges() & wantedEdges) == wantedEdges) {
            qmaps[wantedEdges] = QuadMapFactory.generateMap(maxHash, qpieces.rawInts(), qedges.rawLongs(), hasher);
        }
    }
}
