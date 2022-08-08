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
import dk.ekot.misc.GrowableInts;
import dk.ekot.misc.GrowableLongArray;
import dk.ekot.misc.GrowableLongs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds Quads, packed as longs as defined in {@link QBits}.
 */
public class QuadSet {
    private static final Logger log = LoggerFactory.getLogger(QuadSet.class);

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
    private Bitmap existing;

    public QuadSet(int maxQuads) {  // TODO: Make auto-extending Bitmap
        qpieces = new GrowableInts(maxQuads);
        qinners = new GrowableLongs(maxQuads);
        existing = new Bitmap(maxQuads);
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
    public void addQuad(long qinner) {
        qinners.add(qinner);
    }

    /**
     * @return a deep copy of this QuadSet.
     */
    public QuadSet deepCopy() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Remove all Quads which contains the given piece.
     * @param pieceID an ID from 0 to 255, inclusive.
     * @return true if {@code need} <= size, else false.
     */
    public boolean removePiece(int pieceID) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Remove all Quads which contains any of the given pieces.
     * @param pieceIDs bitmap of size 256, where each set bit signals a pieceID.
     * @return true if {@code need} <= size, else false.
     */
    public boolean removePieces(long[] pieceIDs) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * @return number of free Fields that needs 1 element from this QuadSet.
     */
    public int getNeed() {
        return need;
    }

    /**
     * @return number of Quads in this set.
     */
    public int getSize() {
        return size;
    }
}
