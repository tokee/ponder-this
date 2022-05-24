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
package dk.ekot.apmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public class PriorityAdjuster {
    private static final Logger log = LoggerFactory.getLogger(PriorityAdjuster.class);

    public enum FILLER {
        sixCorners, centerGood, centerBad, tripleCount, neutral, random, centerBadSixCorners;
        static FILLER getDefault() {
            return centerBadSixCorners;
        }
    }

    public static void adjustPriorities(Mapper board, FILLER filler) {
        switch (filler) {
            case centerBad:
                adjustPrioritiesCenterBad(board);
                break;
            case centerGood:
                adjustPrioritiesCenterGood(board);
                break;
            case sixCorners:
                adjustPrioritiesShape6Corners3Inner(board);
                break;
            case tripleCount:
                adjustPrioritiesByTripleCount(board);
                break;
            case neutral:
                break;
            case random:
                adjustPrioritiesRandom(board);
                break;
            case centerBadSixCorners:
                adjustPrioritiesCenterBad6Corners3Inner(board);
                break;
            default: throw new UnsupportedOperationException("Priority filler '" + filler + "' not implemented yet");
        }
    }

    private static void adjustPrioritiesRandom(Mapper board) {
        int seed = new Random().nextInt();
        System.out.println("adjustPrioritiesRandom seed=" + seed);
        Random random = new Random(seed);
        List<Integer> values = IntStream.range(0, board.valids).boxed().collect(Collectors.toList());
        Collections.shuffle(values, random);
        AtomicInteger index = new AtomicInteger(0);
        board.streamAllValid().forEach(pos -> board.priority[pos] = values.get(index.getAndIncrement()));
    }

    /**
     * Adjust all priorities so that the center position is worst and the edges are best.
     */
    public static void adjustPrioritiesCenterBad(Mapper board) {
        adjustPrioritiesCenterBad(board, 2);
    }
    public static void adjustPrioritiesCenterBad(Mapper board, int factor) {
        final int centerX = board.width/2;
        final int centerY = board.height/2;
        board.visitAllXY((x, y) -> {
            int distToCenter = (int) Math.sqrt(Math.pow(Math.abs(centerX - x), 2)/2 +
                                               Math.pow(Math.abs(centerY-y), 2));
            //System.out.printf("pos(%d, %d), center(%d, %d), dist=%d\n", x, y, centerX, centerY, distToCenter);
            board.adjustPriority(x, y, (board.edge-distToCenter)*factor);
        });
    }

    /**
     * Adjust all priorities so that best places are the six corners and three inner areas.
     */
    public static void adjustPrioritiesShape6Corners3Inner(Mapper board) {
        final int GOOD = 0;
        final int BAD = 1000;

        // Fill with bad
        board.visitAllXY((x, y) -> {
            board.adjustPriority(x, y, BAD);
        });

        set6Corners(board, GOOD);
        set3Areas(board, GOOD);
    }

    /**
     * Combination of centerBad and sixCorners3Areas
     */
    public static void adjustPrioritiesCenterBad6Corners3Inner(Mapper board) {
        final int GOOD = 0;

        adjustPrioritiesCenterBad(board, 1);
        set6Corners(board, GOOD);
        set3Areas(board, GOOD);
    }

    private static void set3Areas(Mapper board, int priority) {
        int edge = board.edge;
        // Fill Area 1, 2, 3
        for (int hexY = 0; hexY < edge * 3 / 9 + 1 ; hexY++) {
            for (int hexX = 0 ; hexX < hexY ; hexX++) {
                int topY = edge * 4 / 9 + hexY;
                board.setPriorityHex(hexX + (edge / 4), topY, priority);
                board.setPriorityHex(hexX + (edge + edge / 4 - 1), topY, priority);

                int bottomY = edge + topY;
                board.setPriorityHex((edge / 4 - bottomY) + hexX, bottomY, priority);
            }
        }
    }

    private static void set6Corners(Mapper board, int priority) {
        int edge = board.edge;
        for (int hexY = 0; hexY < edge / 3 ; hexY++) {
            // Fill tl, tr, bl, br
            for (int hexX = 0; hexX < edge / 3 ; hexX++) {
                // tl
                board.setPriorityHex(hexX, hexY, priority);
                // tr
                board.setPriorityHex((edge + hexY) - hexX - 1, hexY, priority);
                // bl
                board.setPriorityHex(hexX, edge * 2 - 2 - hexY, priority);
                // br
                board.setPriorityHex((edge + hexY) - hexX - 1, edge * 2 - 2 - hexY, priority);
            }
            // Fill ml, mr
            for (int hexX = 0 ; hexX < hexY ; hexX++) {
                int mltopY = hexY + edge * 2 / 3;
                // ml-top
                board.setPriorityHex(hexX, mltopY, priority);
                // mr-top
                board.setPriorityHex((edge + mltopY) - hexX - 1, mltopY, priority);

                int mlbottomY = edge * 2 - 2 - (hexY + edge * 2 / 3);
                // ml-bottom
                board.setPriorityHex(hexX, mlbottomY, priority);
                // mr-bottom
                board.setPriorityHex((edge * 3 - 2 - mlbottomY) - hexX - 1, mlbottomY, priority);
            }
        }
    }

    /**
     * Adjust all priorities so that the center position is best and the edges are worst.
     */
    public static void adjustPrioritiesCenterGood(Mapper board) {
        final int centerX = board.width/2;
        final int centerY = board.height/2;
        board.visitAllXY((x, y) -> {
            int distToCenter = (int) Math.sqrt(Math.pow(Math.abs(centerX - x), 2)/2 +
                                               Math.pow(Math.abs(centerY-y), 2));
            //System.out.printf("pos(%d, %d), center(%d, %d), dist=%d\n", x, y, centerX, centerY, distToCenter);
            board.adjustPriority(x, y, distToCenter*2);
        });
    }

    /**
     * Adjust all priorities so that the priority of any cell is the number of its triples.
     */
    public static void adjustPrioritiesByTripleCount(Mapper board) {
        log.debug("Adjusting priorities by triple count" + (board.edge < 100 ? "" : ". This might take some seconds"));
        board.visitAllXY((x, y) -> {
            AtomicInteger counter = new AtomicInteger(0);
            board.visitTriples(x, y, (pos, pos2) -> {
                counter.addAndGet(2);
            });
            //System.out.printf("pos(%d, %d), center(%d, %d), dist=%d\n", x, y, centerX, centerY, distToCenter);
            board.adjustPriority(x, y, counter.get());
        });
    }

    
    
}
