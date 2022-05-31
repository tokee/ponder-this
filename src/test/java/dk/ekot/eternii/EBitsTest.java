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
public class EBitsTest extends TestCase {

    public void testGetHash() {
        long state = 0b11000000000000001;
        int hash = EBits.getHash( 0b1010, state);
//        System.out.println("North: " + EBits.getNorthEdge(state));
//        System.out.println("South: " + EBits.getSouthEdge(state));

//        System.out.println("Edge mask: " + Long.toBinaryString(EBits.EDGE_MASK));
        assertTrue("Hash should not exceed 32*32 but was " + hash, hash < 1024);
    }

}