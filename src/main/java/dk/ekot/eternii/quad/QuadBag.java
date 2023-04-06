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
import java.util.stream.IntStream;

/**
 * Holds Quads, packed as ints & longs as defined in {@link QBits}.
 * <p>
 * Once constructed, QuadBag pieces and edges are immutable and can be shared between threads.
 * <p>
 * Quad availability is controlled by {@link #existing} which is NOT shared between threads.
 */
public class QuadBag implements QuadHolder {
    private static final Logger log = LoggerFactory.getLogger(QuadBag.class);

    private final PieceTracker pieceTracker;
    private final BAG_TYPE bagType;
    // n=0b1000, e=0b0100, s=0b0010, w=0b0001
    private final QuadEdgeMap[] qeMaps = new QuadEdgeMap[16];


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
    // TODO: Switch to stateID->bitmap Map-based cache with multiple snapshots
    private Bitmap snapshot = null;
    private Bitmap existing;
    private long existingStateID = -1;
    private int existingAvailable = -1;

    public QuadBag(PieceTracker pieceTracker, BAG_TYPE bagType) {
        this.pieceTracker = pieceTracker;
        this.bagType = bagType;
        qpieces = new GrowableInts();
        qedges = new GrowableLongs();
        existing = new GrowableBitmap(0);
    }

    public QuadBag(PieceTracker pieceTracker, BAG_TYPE bagType,
                   GrowableInts qpieces, GrowableLongs qedges, GrowableBitmap existing) {
        this.pieceTracker = pieceTracker;
        this.bagType = bagType;
        this.qpieces = qpieces;
        this.qedges = qedges;
        this.existing = existing;
    }

    @Override
    public IntStream getAvailableQuadIDs() {
        validateAvailability();
        if (available() == 0) {
            throw new IllegalStateException("No pieces available in quad bag " + bagType);
        }
        return existing.getSetIndicesStream();
    }

    @Override
    public boolean isAvailable(int quadID) {
        validateAvailability();
        return existing.get(quadID);
    }

    /**
     * Get the quad map corresponding to the given colors.
     * The colors are extracted using e.g. {@link QBits#getColN} for the quad in the given direction.
     * If there are no quad, {@code -1} is given.
     *
     * The method automatically ignores irrelevant edges, e.g. the north edge if bagType == border_n.
     * @return the quad set for the given defined border colors.
     */
    public QuadEdgeMap getMap(int colN, int colE, int colS, int colW) {
        return getMapReduced(bagType == BAG_TYPE.border_n || bagType == BAG_TYPE.corner_nw ? -1 : colN,
                             bagType == BAG_TYPE.border_e || bagType == BAG_TYPE.corner_ne ? -1 : colE,
                             bagType == BAG_TYPE.border_s || bagType == BAG_TYPE.corner_se ? -1 : colS,
                             bagType == BAG_TYPE.border_w || bagType == BAG_TYPE.corner_sw ? -1 : colW);
    }

    private QuadEdgeMap getMapReduced(int colN, int colE, int colS, int colW) {
        final int defined = (colN != -1 ? 0b1000 : 0b000) |
                            (colE != -1 ? 0b0100 : 0b000) |
                            (colS != -1 ? 0b0010 : 0b000) |
                            (colW != -1 ? 0b0001 : 0b000);
        return qeMaps[defined];
    }

    /**
     * Trim the holding structures down to size modulo 64, without any room for further quads.
     */
    public QuadBag trim() {
        qpieces = qpieces.trimCopyAlign(64);
        qedges = qedges.trimCopyAlign(64);

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
        existing.set(size++); // Extend existing
        existingStateID = -1; // Invalidate existing
//        if (c++ < 4) {
//            System.out.println("Add " + QBits.toStringFull(qpiece, qedges));
//        }
    }
//    int c = 0;

    /**
     * @return the number of all quads, takes as well as free.
     */
    public int size() {
        return size;
    }

    /**
     * @return the number of free/available quads based on masking.
     */
    @Override
    public int available() {
        validateAvailability(); // If already done this is a null op
        return existingAvailable;
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

    public void validateAvailability() {
        if (existingStateID == pieceTracker.getStateID()) {
            log.info("validateAvailability for {} had matching statedID={}", bagType, existingStateID);
            return;
        }

        validateAvailability(pieceTracker.pieceIDByteMap, 0, calcBlock(qedges.size()));
        existingStateID = pieceTracker.getStateID();
        existingAvailable = existing.cardinality();
        log.info("validateAvailability for {} showed {}/{} quads available",
                 bagType, available(), size());
    }

    /**
     * Mark all qpieces as live or dead.
     * @param pieceMask an array of length 256, each entry representing the piece with index as ID.
     *                 0 means the piece should be removed, 1 means it should be kept.
     * @param startBlock first block (of 64 pieceID bits), inclusive.
     * @param endBlock last block (of 64 pieceID bits), exclusive.
     */
    private void validateAvailability(byte[] pieceMask, int startBlock, int endBlock) {
        long[] blocks = existing.getBacking();
        for (int blockIndex = startBlock ; blockIndex < endBlock ; blockIndex++) {
            long block = 0L;
            for (int i = 0 ; i < 64 ; i++) {
                block = block << 1;
                int id = (blockIndex << 6) + i;
                final int pieceIDs = qpieces.get(id);
                block |= (pieceMask[pieceIDs & 0xFF] +
                          pieceMask[(pieceIDs >> 8) & 0xFF] +
                          pieceMask[(pieceIDs >> 16) & 0xFF] +
                          pieceMask[(pieceIDs >> 24) & 0xFF]) >> 2; // 4 -> 1
            }
            blocks[blockIndex] = block;
        }

        // Last block might be padded with invalids to align to longs
        blocks[endBlock-1] &= -1L << (qpieces.backingArraySize()-qpieces.size());

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

    // Experimental. Works fine but maybe better to keep the full stack single threaded and use multiple stacks?
    private void recalculateQPiecesParallel() {
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
            validateAvailability(pieceTracker.pieceIDByteMap, startBlock, localEndBlock);
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
        return new QuadBag(pieceTracker, newType, rotQpieces, rotQEdges, blankExisting);
    }

    public int getQPiece(int quadID) {
        return qpieces.get(quadID);
    }
    public long getQEdges(int quadID) {
        return qedges.get(quadID);
    }

    public void generateSets() {
        if (bagType == BAG_TYPE.inner) {
            log.warn("Skipping sets for bagType "+ bagType);
            qeMaps[0b000] = new QuadMapAll(this); // Okay, except this cheap one
            return;
            // TODO: Enable this
        }
        log.info("Generating sets for QuadBag of type " + bagType);
        qeMaps[0b000] = new QuadMapAll(this);
        
        generateEdgeMap(0b1000, QBits.MAX_QCOL_EDGE1);
        generateEdgeMap(0b0100, QBits.MAX_QCOL_EDGE1);
        generateEdgeMap(0b0010, QBits.MAX_QCOL_EDGE1);
        generateEdgeMap(0b0001, QBits.MAX_QCOL_EDGE1);

        generateEdgeMap(0b1100, QBits.MAX_QCOL_EDGE2);
        generateEdgeMap(0b0110, QBits.MAX_QCOL_EDGE2);
        generateEdgeMap(0b0011, QBits.MAX_QCOL_EDGE2);
        generateEdgeMap(0b1001, QBits.MAX_QCOL_EDGE2);

        generateEdgeMap(0b1010, QBits.MAX_QCOL_EDGE2);
        generateEdgeMap(0b0101, QBits.MAX_QCOL_EDGE2);

        generateEdgeMap(0b1110, QBits.MAX_QCOL_EDGE3);
        generateEdgeMap(0b0111, QBits.MAX_QCOL_EDGE3);
        generateEdgeMap(0b1011, QBits.MAX_QCOL_EDGE3);
        generateEdgeMap(0b1101, QBits.MAX_QCOL_EDGE3);

        generateEdgeMap(0b1111, QBits.MAX_QCOL_EDGE4);
    }

    private void generateEdgeMap(int wantedEdges, long maxHash) {
        if (Integer.bitCount(wantedEdges) >= 3) {
            // TODO: enable
            log.warn("Skipping set for edges " + QBits.toStringEdges(wantedEdges) + " for now. Will probably be enabled later");
            return;
        }
        Function<Long, Long> hasher = qedges -> QBits.getHash(wantedEdges, qedges, true);
        if ((bagType.variableEdges() & wantedEdges) == wantedEdges) {
            qeMaps[wantedEdges] = QuadMapFactory.generateMap(this, maxHash, hasher, wantedEdges);
        }
        log.info("Generated map for " + bagType + " edges " + QBits.toStringEdges(wantedEdges));
    }

    public int[] getQpiecesRaw() {
        return qpieces.rawInts();
    }

    public long[] getQedgesRaw() {
        return qedges.rawLongs();
    }
    public BAG_TYPE getType() {
        return bagType;
    }

    /**
     * Get the map suitable for the surrounding set fields.
     * If a surrounding fields is an outer board edge (grey), its value is ignored.
     */
    public QuadEdgeMap getQuadEdgeMap(boolean isFilledNW, boolean isFilledNE,
                                      boolean isFilledSE, boolean isFilledSW) {
        return getQuadEdgeMap((isFilledNW ? 0b1000 : 0) |(isFilledNE ? 0b0100 : 0) |
                              (isFilledSE ? 0b0010 : 0) |(isFilledSW ? 0b0001 : 0));
    }

    /**
     * Get the map suitable for the surrounding set fields.
     * If a surrounding fields is an outer board edge (grey), its value is ignored.
     * @param neighbours n=0b1000, e=0b0100, s=0b0010, w=0b0001.
     */
    public QuadEdgeMap getQuadEdgeMap(int neighbours) {
        // No check for null. Callers are expected never to ask for non-existing maps
        return qeMaps[bagType.variableEdges() & neighbours];
    }
}

