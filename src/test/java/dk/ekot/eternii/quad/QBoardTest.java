package dk.ekot.eternii.quad;

import dk.ekot.eternii.BoardVisualiser;
import junit.framework.TestCase;

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

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testVisualisation() {
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live);

        QField f0_0 = board.getField(0, 0);
        board.placePiece(f0_0.getX(), f0_0.getY(), f0_0.getAvailableQuadIDs().findFirst().getAsInt());

        QField f1_1 = board.getField(1, 1);
        board.placePiece(f1_1.getX(), f1_1.getY(), f1_1.getAvailableQuadIDs().findFirst().getAsInt());

        try {
            Thread.sleep(1000000000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}