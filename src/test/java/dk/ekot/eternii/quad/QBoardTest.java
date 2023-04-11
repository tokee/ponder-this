package dk.ekot.eternii.quad;

import dk.ekot.eternii.BoardVisualiser;
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
public class QBoardTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(QBoardTest.class);

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testVisualisation() {
        QBoard board = new QBoard();
    //    BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live);

        log.info("Board created. Placing pieces");
        placeFirstAvailable(board, 0, 0);
        placeFirstAvailable(board, 0, 1);
        placeFirstAvailable(board, 0, 2);
        placeFirstAvailable(board, 0, 3);
        placeFirstAvailable(board, 1, 0);
        placeFirstAvailable(board, 1, 1);
       /* try {
            Thread.sleep(1000000000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/

    }

    private void placeFirstAvailable(QBoard board, int x, int y) {
        log.debug("Placing first available at ({}, {})", x, y);
        QField field = board.getField(x, y);
        int quadID = field.getAvailableQuadIDs().findFirst().getAsInt();
        log.debug("First available quad ID for ({}, {}) was {}", x, y, quadID);
        boolean allNeedsSatisfied = board.placePiece(x, y, quadID);
        assertTrue("All needs must be satisfied", allNeedsSatisfied);
        //log.info("allNeedsSatisfied = " + allNeedsSatisfied);
    }
}