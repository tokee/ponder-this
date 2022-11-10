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

/**
 * Holds Quads, packed as ints & longs as defined in {@link QBits}.
 *
 * Quads are never removed but instead masked.
 */
public class QuadBag {
    private static final Logger log = LoggerFactory.getLogger(QuadBag.class);
    private final PieceMap pieceMap;

    /**
     * Number of free Fields that needs 1 element from this QuadSet.
     */
    private int need = 0;

    /**
     * Number of Quads in this set.
     */
    private int size = 0;

    private GrowableInts qpieces;
    private GrowableLongs qinners;
    private Bitmap snapshot = null;
    private Bitmap existing;

    public QuadBag(PieceMap pieceMap) {
        this.pieceMap = pieceMap;
        qpieces = new GrowableInts();
        qinners = new GrowableLongs();
        existing = new GrowableBitmap(0);
    }

    /**
     * Trim the holding structures down to size, without any room for further quads.
     */
    public void trim() {
        // TODO: Implement this
        log.warn("trim not implemented yet!");
    }

    /**
     * Add a Quad. No checking for duplicates!
     * @param qinner af defined in {@link QBits}.
     */
    public void addQuad(int qpiece, long qinner) {
        qpieces.add(qpiece);
        qinners.add(qinner);
        existing.set(size++);
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
        recalculateQPieces(pieceMap.pieceIDByteMap, 0, calcBlock(qinners.size()));
    }

    public void recalculateQPiecesParallel() {
        final int segmentBlocks = 512;
        int endBlock = calcBlock(qinners.size());
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
}
