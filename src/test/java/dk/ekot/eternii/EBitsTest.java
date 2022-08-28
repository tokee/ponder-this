package dk.ekot.eternii;

import junit.framework.TestCase;

import static dk.ekot.eternii.EPieces.NULL_E;

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
public class EBitsTest extends TestCase {

    public void testGetHash() {
        long state = 0b11000000000000001;
        int hash = EBits.getHash( 0b1010, state);
//        System.out.println("North: " + EBits.getNorthEdge(state));
//        System.out.println("South: " + EBits.getSouthEdge(state));

//        System.out.println("Edge mask: " + toBin(EBits.EDGE_MASK));
        assertTrue("Hash should not exceed 32*32 but was " + hash, hash < 1024);
    }

    public void testGeneralBits() {
        long state = EBits.BLANK_STATE;
        //System.out.println(EBits.toString(state) + " blank");
        state = EBits.setNorthEdge(state, 12);
        state = EBits.setEastEdge(state, 13);
        state = EBits.setSouthEdge(state, NULL_E);
        state = EBits.setWestEdge(state, 15);
        //System.out.println(EBits.toString(state) + " edges");
        state = EBits.setDefinedEdges(state, 0b1010);
        //System.out.println(EBits.toString(state) + " defined");
        state = EBits.setPiece(state, 255);
        //System.out.println(EBits.toString(state) + " piece");
        state = EBits.setRotation(state, 2);
        //System.out.println(EBits.toString(state) + " rotation");
        state = EBits.setPieceNorthEdge(state, 5);
        state = EBits.setPieceEastEdge(state, 6);
        state = EBits.setPieceSouthEdge(state, 7);
        state = EBits.setPieceWestEdge(state, 8);
        //System.out.println(EBits.toString(state) + " piece edges");

        for (int i = 0 ; i < 4 ; i++) { // 4 times 90 degrees = origo
            state = EBits.shiftEdgesRight(state);
        }

        assertEquals("Rotation should work", 2, EBits.getRotation(state));
        assertEquals("Piece should work", 255, EBits.getPiece(state));
        assertEquals("DefinedEdges should work", 0b1010, EBits.getDefinedEdges(state));
        assertEquals("North set/get should work", 12, EBits.getNorthEdge(state));
        assertEquals("East set/get should work", 13, EBits.getEastEdge(state));
        assertEquals("South set/get should work", NULL_E, EBits.getSouthEdge(state));
        assertEquals("West set/get should work", 15, EBits.getWestEdge(state));
        assertEquals("North piece set/get should work", 5, EBits.getPieceNorthEdge(state));
        assertEquals("East piece set/get should work", 6, EBits.getPieceEastEdge(state));
        assertEquals("South piece set/get should work", 7, EBits.getPieceSouthEdge(state));
        assertEquals("West piece set/get should work", 8, EBits.getPieceWestEdge(state));
        assertEquals("Derive defined edges should work", 0b1101, EBits.deriveDefinedEdges(state));

        state = EBits.shiftEdgesRight(state);
        assertEquals("Derive defined edges should work after rotation", 0b1110, EBits.deriveDefinedEdges(state));

        //System.out.println(EBits.toString(state) + " pre switch");
        long piece = EBits.getPieceAllEdges(state);
        long outer = EBits.getAllEdges(state);
        state = EBits.setPieceAllEdges(state, outer);
        //System.out.println(EBits.toString(state) + " switch piece");
        state = EBits.setAllEdges(state, piece);
        //System.out.println(EBits.toString(state) + " switch outer");
        //System.out.println("outer:  " + Long.toBinaryString(EBits.getAllEdges(state)) + " " + EBits.getAllEdges(state) + " exp=" + piece);
        //System.out.println("piece: " + Long.toBinaryString(EBits.getPieceAllEdges(state)) + " " + EBits.getPieceAllEdges(state) + " exp=" + outer);
        assertEquals("Switching edges piece should work", piece, EBits.getAllEdges(state));
        assertEquals("Switching edges outer should work", outer, EBits.getPieceAllEdges(state));
        assertEquals("DefinedEdges should work post switch", 0b1111, EBits.getDefinedEdges(state));

    }

    public void testInnerOuterMatch() {
        long state = EBits.BLANK_STATE;
        state = EBits.setNorthEdge(state, 12);
        state = EBits.setEastEdge(state, 13);
        state = EBits.updateDefinedEdges(state);
        assertEquals("Defined edges should be correct", 0b1100, EBits.getDefinedEdges(state));
        {
            long inner = EBits.setPieceNorthEdge(EBits.BLANK_STATE, 3);
            inner = EBits.setPieceEastEdge(inner, 13);
            inner = EBits.setPieceSouthEdge(inner, 5);
            inner = EBits.setPieceWestEdge(inner, 4);
            inner = inner >> 32;
            assertFalse(EBits.innerEdgesMatchesSetOuter(state, inner));
        }
        {
            long inner = EBits.setPieceNorthEdge(EBits.BLANK_STATE, 12);
            inner = EBits.setPieceEastEdge(inner, 13);
            inner = EBits.setPieceSouthEdge(inner, 5);
            inner = EBits.setPieceWestEdge(inner, 4);
            inner = inner >> 32;
            assertTrue(EBits.innerEdgesMatchesSetOuter(state, inner));
        }
    }

    public void testUpperLeft() {
        EPieces pieces = EPieces.getEternii();
        long state = 4616189613769261024L;
        for (int rot = 0 ; rot < 4 ; rot++) {
            System.out.println("rot=" + rot + ": " + EBits.innerEdgesMatchesSetOuter(state, pieces.getEdges(0, rot)));
        }
    }
//
}