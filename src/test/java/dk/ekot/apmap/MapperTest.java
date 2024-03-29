package dk.ekot.apmap;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
public class MapperTest extends TestCase {

    @Test
    public void testToString() {
        for (int edge = 3 ; edge <= 5 ; edge++) {
            Mapper map = new Mapper(edge);
            System.out.println(map + " Elements: " + map.validCount());
            System.out.println();
        }
    }

    @Test
    public void testFlattening() {
        for (int edge = 3 ; edge <= 5 ; edge++) {
            Mapper map = new Mapper(edge);
            int flat[] = map.getFlat();
            flat[5] = Mapper.MARKER;
            flat[10] = Mapper.ILLEGAL;
            map.setFlat(flat);
            System.out.println(map + " Elements: " + map.validCount());
            System.out.println();
        }
    }

    @Test
    public void testTriples() {
        for (int edge = 3 ; edge <= 5 ; edge++) {
            Mapper map = new Mapper(edge);

            int[] flat = map.getFlat();
            flat[5] = Mapper.MARKER;
            map.setFlat(flat);
            System.out.println(map + " Elements: " + map.validCount());
            int[][] triples = map.getFlatTriples();
            System.out.println("Triples from 5: " + triplesToString(triples[5]));
            System.out.println();
        }
    }

    public void testMarkAndRollback() {
        Mapper board = new Mapper(3);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.markAndDeltaExpand(2, 0, true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.markAndDeltaExpand(2, 2, true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.markAndDeltaExpand(3, 1, true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.markAndDeltaExpand(5, 1, true);
//        board.markAndDeltaExpand(3, 3);
        System.out.println("-------------------------------------------------------------------");
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.rollback(true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.rollback(true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.rollback(true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
        board.rollback(true);
        System.out.printf("%s\nMarked=%d, neutrals=%d\n", board, board.marked, board.neutrals);
    }

    private String triplesToString(int[] triples) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0 ; i < triples.length ; i+=3) {
            sb.append(String.format("[%d, %d, %d]", triples[i], triples[i+1], triples[i+2]));
            if (i < triples.length-1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Test
    public void testVisitTriples2() {
        dumpVisitTriples(2, 1, 0);
        dumpVisitTriples(2, 2, 1);
    }
    @Test
    public void testVisitTriples3() {
        dumpVisitTriples(3, 4, 0);
    }
    @Test
    public void testVisitTriplesProblem() {
        dumpVisitTriples(2, 2, 1);
        dumpVisitTriples(2, 1, 0);
        dumpVisitTriples(4, 6, 3);
        dumpVisitTriples(4, 5, 2);

    }
    @Test
    public void testVisitTriplesAll2() {
        //dumpVisitTriplesAll(2);
        dumpVisitTriplesAll(2);
//        dumpVisitTriplesAll(4);
    }

    @Test
    public void testVisitTriplesExperiment2() {
        dumpVisitTriples(2, 1, 0);
        dumpVisitTriples(2, 3, 0);
        dumpVisitTriples(2, 0, 1);
        dumpVisitTriples(2, 2, 1);
        dumpVisitTriples(2, 4, 1);
        dumpVisitTriples(2, 1, 2);
        dumpVisitTriples(2, 3, 2);

    }
    @Test
    public void testVisitTriplesExperiment() {
        dumpVisitTriples(2, 3, 2);
//        System.out.println("***********************");
    }
    @Test
    public void testVisitTriples5() {
        dumpVisitTriples(5, 4, 0);
    }
    private void dumpVisitTriplesAll(int edge) {
        Mapper board = new Mapper(edge);
        board.visitAllXY((x, y) -> {
            System.out.printf("Triples for (%d, %d)\n", x, y);
            dumpVisitTriples(edge, x, y);
        });
    }

    public void dumpVisitTriples(int edge, int x, int y) {
        Mapper board = new Mapper(edge);
        if (board.getQuadratic(x, y) == Mapper.INVALID) {
            throw new IllegalArgumentException("Position(" + x + ", " + y + ") is not valid");
        }
        board.setQuadratic(x, y, Mapper.MARKER);
        System.out.println(board);
        System.out.println("----");
        board.visitTriples(x, y, (posA, posB) -> {
            //board.quadratic[posA] = Mapper.ILLEGAL;
            //board.quadratic[posB] = Mapper.ILLEGAL;
            //board.quadratic[posB] = Mapper.ILLEGAL;
            ++board.priority[posA];
            ++board.priority[posB];
        });
        System.out.println(board);
    }

    public void testVisitAll() {
        dumpVisitAll(2);
        dumpVisitAll(3);
        dumpVisitAll(4);
    }

    private void dumpVisitAll(int edge) {
        Mapper board = new Mapper(edge);
        board.visitAllValid(pos -> ++board.priority[pos]);
        System.out.println(board);
    }
    public void testVisitAllIntersecting() {
        dumpVisitIntersecting(5);

    }
    private void dumpVisitIntersecting(int edge) {
        Mapper board = new Mapper(edge);
        System.out.println(board);
        board.visitAllXY((x, y) -> {
            dumpIntersecting(edge, x, y);
        });
    }

    public void testSpecificIntersecting3() {
        dumpIntersecting(3, 5, 1);
    }

    private void dumpIntersecting(int edge, int x, int y) {
        Mapper b2 = new Mapper(edge);
        b2.setQuadratic(x, y, Mapper.MARKER);
        b2.visitTriplesIntersecting(x, y, (pos1, pos2) -> {
            ++b2.priority[pos1];
            ++b2.priority[pos2];
        });
        System.out.println(b2 + " start(" + x + ", " + y + ")");
    }


    @Test
    public void testVisitTriples3problem() {
        dumpVisitTriples(3, 7, 3);
    }
    public void testMargins() {
//        testMargin(2);
        testMargin(3);
//        testMargin(4);
    }
    public void testMargin(int edge) {
        System.out.println("---");
        Mapper board = new Mapper(edge);
        System.out.println(board);
        for (int y = 0 ; y < board.height ; y++) {
            int marginX = Math.abs(y - (board.height>>1));
//            System.out.printf("origoX-marginX=%d, width=%d, origoX=%d, marginX=%d, maxDistX=%d\n", origoX-marginX, width, origoX, marginX, maxDistX);
            System.out.printf("y=%d, margin=%d\n", y, marginX);
        }
    }

    public void testSpeed() {
        testSpeed(100);
    }
    public void testSpeed(int edge) {
        Mapper board = new Mapper(edge);
        AtomicLong sum = new AtomicLong(0);
        AtomicLong tSum = new AtomicLong(0);
        board.visitAllXY((x, y) -> {
            Mapper inner = new Mapper(edge);
            final long startTime = System.nanoTime();
            inner.visitTriples(x, y, (pos1, pos2) -> sum.addAndGet(pos1+pos2));
            tSum.addAndGet(System.nanoTime()-startTime);
        });
        System.out.printf("edge=%d, time=%dms", edge, tSum.get()/1000000L);
    }

    public void testTopleft() {
        assertEquals("Expected correct count for edge=2", 1, new Mapper(2).getTopLeftPositions().size());
        assertEquals("Expected correct count for edge=3", 2, new Mapper(3).getTopLeftPositions().size());
        assertEquals("Expected correct count for edge=4", 2, new Mapper(4).getTopLeftPositions().size());
        assertEquals("Expected correct count for edge=5", 3, new Mapper(5).getTopLeftPositions().size());
    }

    public void testRotate() {
        //testRotate(3, 2, 0);
        testRotate(5, 7, 1);
        //testRotate(3, 3, 1);
        //testRotate(3, 3, 1);
        //testRotate(3, 6, 0);
    }
    public void testRotate(int edge, int x, int y) {
        System.out.println("----");
        Mapper board = new Mapper(edge);
    //    System.out.println(board);
        board.addVisitedRotated(x, y);
        board.setQuadratic(x, y, Mapper.MARKER);
        System.out.println(board);

    }

    public void testTranslate() {
        int edge = 4;
        Mapper board = new Mapper(edge);
        board.setQuadratic(2, 0, Mapper.MARKER);
        System.out.println(board);
        board.visitAllXY((x, y) -> {
            int centerX = board.width/2;
            int centerY = board.height/2;

            int relX = x-centerX;
            int relY = y-centerY;

            // Adjust horizontal coordinates to be without gaps
            relX = relX>>1; // TODO: Should probably do some trickery every other line here

            System.out.printf("center(%d, %d), pos(%d, %d) -> pos(%d, %d)\n", centerX, centerY, x, y, relX, relY);
        });
    }

    public void testAdjustPriorities() {
        testAdjustPriorities(2);
        testAdjustPriorities(3);
        testAdjustPriorities(4);
        testAdjustPriorities(5);
    }
    public void testAdjustPriorities(int edge) {
        System.out.println("----");
        Mapper board = new Mapper(edge);
        PriorityAdjuster.adjustPrioritiesByTripleCount(board);
        System.out.println(board);
    }

    public void testSetMarker() {
        Mapper board = new Mapper(3);
        board.setMarker(2, 0, true); System.out.println(board); System.out.println("----");
        board.setMarker(4, 0, true); System.out.println(board); System.out.println("----");
        board.setMarker(5, 1, true); System.out.println(board); System.out.println("----");
        board.setMarker(4, 2, true); System.out.println(board); System.out.println("----");
        board.removeMarker(4, 0, true); System.out.println(board); System.out.println("----");
    }

    public void testInitialFill() {
        for (int edge = 2 ; edge < 50 ; edge++) {
            System.out.println("Creating board with edge " + edge);
            new Mapper(edge);
        }
    }

    public void testFromToJSON() {
        Mapper board = new Mapper(3);
        board.setMarker(2, 0, true);
        board.setMarker(4, 0, true);
        board.setMarker(5, 1, true);
        board.setMarker(4, 2, true);
        String before = board.toJSON();
        Mapper toFromJSON = Mapper.fromJSON(before);
        String after = toFromJSON.toJSON();
        assertEquals("Board -> JSON -> board should be the identity", before, after);
    }

    public void testCacheTriples() {
        new Mapper(578).cacheTriplesEachPosTest();
    }

    public void testGetTripleDeltasDebug2() {
        Mapper board = new Mapper(2);
        Mapper.Pair<int[], int[]> deltas = board.getTripleDeltas(2);
        System.out.printf(Locale.ROOT, "edge=%d, width=%d, height=%d, radial=%s, intersect=%s",
                          board.edge, board.width, board.height,
                          Arrays.toString(deltas.first), Arrays.toString(deltas.second));
    }
    public void testGetTripleDeltas() {
        Mapper board = new Mapper(576);
        long memory = 0;
        // Divide by 2 and multiply by 2 as the board is symmetric
        for (int column = 0 ; column < board.width/2 ; column++) {
            Mapper.Pair<int[], int[]> triples = board.getTripleDeltas(column);
            memory += ((long) triples.first.length + triples.second.length) * 4 * 2;
        }
        System.out.println("Total mem: " + memory/1048576 + " MB");
    }

    public void testVisitTriplesCache() {
        {
            Mapper board = new Mapper(2);
            System.out.println(board);
            board.fillTripleRowDeltas();
            board.setQuadratic(2, 1, Mapper.MARKER);

            board.visitTriples(2, 1, (pos1, pos2) -> {
                System.out.printf("calc origo=(%d, %d), pos1=(%d, %d), pos2=(%d, %d)\n",
                                  2, 1, pos1 % board.width, pos1 / board.width, pos2 % board.width, pos2 / board.width);
                board.quadratic[pos1] = Mapper.ILLEGAL;
                board.quadratic[pos2] = Mapper.ILLEGAL;

            });
            System.out.println(board);
        }
        System.out.println("----------");

        {
            Mapper board = new Mapper(2);
            board.fillTripleRowDeltas();
            board.setQuadratic(2, 1, Mapper.MARKER);
            board.visitTriplesCached(2, 1, (pos1, pos2) -> {
                System.out.printf("cached origo=(%d, %d), pos1=(%d, %d), pos2=(%d, %d)\n",
                                  2, 1, pos1 % board.width, pos1 / board.width, pos2 % board.width, pos2 / board.width);
                board.quadratic[pos1] = Mapper.ILLEGAL;
                board.quadratic[pos2] = Mapper.ILLEGAL;
            });
            System.out.println(board);
        }
    }
    public void testVisitTriplesSpeed() {
        int edge = 200;
        int runs = 3;

        Mapper board = new Mapper(edge);
        board.fillTripleRowDeltas();
        AtomicLong sum = new AtomicLong(0);
        //System.out.println(board);
        final int seed = 87;
        final double fraction = 0.001;
        for (int run = 0 ; run < runs ; run++) {
//            System.out.printf("edge=%d, run=%d/%d\n", edge, run+1, runs);
            AtomicLong calcCount = new AtomicLong(0);
            AtomicLong cacheCount = new AtomicLong(0);
            long calcMS = 0;
            long cacheMS = 0;


            calcMS -= System.currentTimeMillis();
            Random random = new Random(seed);
            board.visitAllXY((x, y) -> {
                if (random.nextDouble() > fraction) {
                    return;
                }
                board.visitTriples(x, y, (pos1, pos2) -> {
                    calcCount.incrementAndGet();
//                    System.out.printf("calc origo=(%d, %d), pos1=(%d, %d), pos2=(%d, %d)\n",
//                                      x, y, pos1%board.width, pos1/board.width, pos2%board.width, pos2/board.width);
                    sum.addAndGet(pos1 + pos2 + board.quadratic[pos1] + board.quadratic[pos2]);
                });
            });
            calcMS += System.currentTimeMillis();

//            System.out.println("------------------------");

            cacheMS -= System.currentTimeMillis();
            Random random2 = new Random(seed);
            board.visitAllXY((x, y) -> {
                if (random2.nextDouble() > fraction) {
                    return;
                }
                board.visitTriplesCached(x, y, (pos1, pos2) -> {
                    cacheCount.incrementAndGet();
//                    System.out.printf("cache origo=(%d, %d), pos1=(%d, %d), pos2=(%d, %d)\n",
//                                      x, y, pos1%board.width, pos1/board.width, pos2%board.width, pos2/board.width);
                    sum.addAndGet(pos1 + pos2 + board.quadratic[pos1] + board.quadratic[pos2]);
                });
            });
            cacheMS += System.currentTimeMillis();

//            System.out.println("**************************************************************");
            System.out.printf(Locale.ROOT, "edge=%d, run=%d, calc=%dms (calls=%d), cache=%dms (calls=%d)\n",
                              edge, run, calcMS, calcCount.get(), cacheMS, cacheCount.get());
        }

    }

    public void testSetPriority() {
        Mapper board = new Mapper(2);
        board.setPriorityHex(0, 0, 1);
        board.setPriorityHex(1, 0, 2);
        board.setPriorityHex(0, 1, 3);
        board.setPriorityHex(1, 1, 4);
        board.setPriorityHex(2, 1, 5);
        System.out.println(board);

        Mapper board3 = new Mapper(3);
        board3.setPriorityHex(0, 0, 1);
        board3.setPriorityHex(1, 0, 2);
        board3.setPriorityHex(0, 1, 3);
        board3.setPriorityHex(1, 1, 4);
        board3.setPriorityHex(2, 1, 5);
        board3.setPriorityHex(0, 2, 6);
        System.out.println(board3);
    }

    public void testCornerPriority() {
        Mapper board = new Mapper(30);
        PriorityAdjuster.adjustPrioritiesShape6Corners3Inner(board);
        System.out.println(board);
    }
}