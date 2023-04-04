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

import junit.framework.TestCase;

import java.util.Locale;
import java.util.Random;

public class QuadBagTest extends TestCase {

    public void testPerformance() {
        final int quads = 800000;
        final int runs = 100;

        final Random r = new Random(87);
        PieceTracker pm = new PieceTracker();
        QuadBag qb = new QuadBag(pm, QuadBag.BAG_TYPE.inner);
        for (int qid = 0 ; qid < quads ; qid++) {
            qb.addQuad(r.nextInt(), r.nextLong());
        }
        System.out.println("Created fake quad bag with " + quads + " quads");

        for (int run = 1 ; run <= runs ; run++) {
            r.nextBytes(pm.pieceIDByteMap);
            long recalcTime = -System.nanoTime();
            qb.recalculateQPieces();
            recalcTime += System.nanoTime();
            System.out.printf(Locale.ROOT, "run=%2d, time=%6.2f ms, %,10d quads/ms\n",
                              run, recalcTime/1000000.0, quads*1000000L/recalcTime);
        }
    }
}