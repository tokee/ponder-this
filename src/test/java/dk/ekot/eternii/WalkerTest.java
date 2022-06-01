package dk.ekot.eternii;

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

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
public class WalkerTest extends TestCase {

    public void testBestEmpty() {
        EBoard board = EBoard.createEterniiBoard(false);
        {
            List<EBoard.Piece> best = board.getField(0, 0).getBestPieces();
            assertEquals("There should be the right number of potential pieces for upper-left",
                         4, best.size());
            assertTrue("It should be possible to place the first piece",
                       board.placePiece(0, 0, best.get(0).piece));
        }
        {
            List<EBoard.Piece> best = board.getField(0, 0).getBestPieces();
            assertEquals("There should be the right number of potential pieces for upper-right",
                         3, board.getField(15, 0).getBestPieces().size());
            assertTrue("It should be possible to place the second piece",
                       board.placePiece(15, 0, best.get(0).piece));
        }
    }

}