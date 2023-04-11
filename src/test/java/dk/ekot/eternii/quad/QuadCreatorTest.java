package dk.ekot.eternii.quad;

import dk.ekot.eternii.PieceTrackerTest;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class QuadCreatorTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(QuadCreatorTest.class);

    public void testSets() {
        PieceTracker pieceTracker = new PieceTracker();
        QuadBag nw =  QuadCreator.createCorner(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.corner_nw)).trim();
        nw.generateSets();
        QuadBag n =  QuadCreator.createEdges(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.border_n)).trim();
        n.generateSets();
        long edgesNW = nw.getQEdges(0);
        int qpieceNW = nw.getQPiece(0);
        log.debug("NW: " + QBits.toStringFull(qpieceNW, edgesNW));
        log.debug("ColE: {}", QBits.getColE(edgesNW));
        pieceTracker.removeQPiece(qpieceNW);

        QuadEdgeMap edgeMapN = n.getQuadEdgeMap(false, false, false, true);
        long hash = QBits.getHash(0b0100, edgesNW, false);
        log.debug("Got hash {}", hash);

        long hashAlt = QBits.getHash(-1, -1, -1, 38, false);
        log.debug("Got alt hash {}", hashAlt);


        long quadCount = edgeMapN.getAvailableQuadIDs(hash).count();
        log.debug("Found " + quadCount + " possible quads for border_n");
        assertTrue("There should be at least one border_n quad available", quadCount > 0);
    }

}