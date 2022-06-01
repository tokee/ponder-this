package dk.ekot.eternii;

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
public class PieceTrackerTest extends TestCase {
    public static final String CORNERS = "[0, 1, 2, 3]";
    public static final String EDGES = "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59]";


    public void testFoundProblems() {
        EPieces pieces = EPieces.getEternii();
        PieceTracker tracker = new PieceTracker(pieces);
        pieces.allPieces().forEach(tracker::add);

        // TODO: Reimplement these tests

        assertEquals("The top left corner should give the expected pieces",
                     CORNERS, tracker.getBestMatching(EBits.setAllEdges(
                        EBits.BLANK_STATE, 0, (int) EPieces.NULL_E, (int) EPieces.NULL_E, 0)).toString());

        assertEquals("The edge color should give the expected pieces",
                     EDGES, tracker.getBestMatching(EBits.setAllEdges(
                        EBits.BLANK_STATE, 0, (int) EPieces.NULL_E, (int) EPieces.NULL_E, (int) EPieces.NULL_E)).toString());

    }
}