package dk.ekot.eternii.quad;

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
public class QBitsTest extends TestCase {

    public void testQPiece() {
        int qpiece = QBits.createQPiece(207, 1, 2, 3); // Causes overflow to negative
        assertEquals("NW should be as stated", 207, QBits.getPieceNW(qpiece));
    }

    public void testHashes() {
        assertEquals("Hash-S should work as expected",
                     800, QBits.getHash(-1, -1, 800, -1, false));
        assertEquals("Hash-W should work as expected",
                     800, QBits.getHash(-1, -1, -1, 800,  false));
        assertEquals("Hash-N should work as expected",
                     800, QBits.getHash(800, -1, -1, -1, false));
    }

    public void testCompactHashes() {
        assertEquals("Hash-S should work as expected",
                     800, QBits.getHash(0b0010, ((long)800) << 10, false));
    }

    public void testBitShift() {
        long qedgesSansRot =
                (((long)801 << 30) |
                (((long)802) << 20) |
                (((long)803) << 10) |
                ((long)804));
        assertEquals("N should be as expected", 801, QBits.getColN(qedgesSansRot));
        assertEquals("E should be as expected", 802, QBits.getColE(qedgesSansRot));
        assertEquals("S should be as expected", 803, QBits.getColS(qedgesSansRot));
        assertEquals("W should be as expected", 804, QBits.getColW(qedgesSansRot));
    }

    public void testDefined() {
        int qedgeN = -1;
        int qedgeE = -1;
        int qedgeS = 800;
        int qedgeW = -1;
        int defined =
                (qedgeN == -1 ? 0 : 0b1000) |
                (qedgeE == -1 ? 0 : 0b0100) |
                (qedgeS == -1 ? 0 : 0b0010) |
                (qedgeW == -1 ? 0 : 0b0001);
        assertEquals("Defined should be as expected", 0b0010, defined);
    }
}