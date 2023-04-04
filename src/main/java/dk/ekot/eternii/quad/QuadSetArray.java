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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * Holds Quad IDs in an array, using sequential passes for processing.
 */
public class QuadSetArray implements QuadSet {
    private static final Logger log = LoggerFactory.getLogger(QuadSetArray.class);

    final PieceTracker pieceTracker;
    final Bitmap base;
    long lastBaseChangeCounter = -1;
    int available = -1;

    final GrowableInts quadIDs;
    boolean[] mask;

    /**
     *
     * @param freeQuads free quads from a QuadBag.
     * @param pieceTracker the piecemap for the {@link QBoard}.
     */
    public QuadSetArray(Bitmap freeQuads, PieceTracker pieceTracker) {
        this.pieceTracker = pieceTracker;
        this.base = freeQuads;
        quadIDs = new GrowableInts();
        mask = new boolean[0];
    }

    @Override
    public void addQuadID(int quadID) {
        quadIDs.add(quadID);
        lastBaseChangeCounter = -1;
    }

    @Override
    public Stream<Integer> getQuadIDs() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int available() {
        if (lastBaseChangeCounter != base.getChangeCounter()) {
            refreshMask();
            lastBaseChangeCounter = base.getChangeCounter();
        }
        return available;
    }

    private void refreshMask() {
        if (mask.length != quadIDs.size()) {
            mask = new boolean[quadIDs.size()];
        }
        available = 0;
        for (int i = 0 ; i < quadIDs.size() ; i++) {
            if (pieceTracker.pieceIDByteMap[i] == 1) {
                mask[i] = true;
                available++;
            } else {
                mask[i] = false;
            }
        }
    }
}
