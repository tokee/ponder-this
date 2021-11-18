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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Walker based on a flattened representation of the board.
 */
public class FlatWalker {
    final FlatBoard board;
    int bestMarkers = 0;
    int[] bestFlat;

    public FlatWalker(FlatBoard board) {
        this.board = board;
        bestFlat = board.getFlatCopy();
    }

    public void walk() {
        walk(Long.MAX_VALUE);
    }

    /**
     * Perform exhaustive walk.
     */
    public void walk(long maxFulls) {
        AtomicLong fulls = new AtomicLong(0);
        walk(-1, 0, 0, fulls, maxFulls);
    }

    private void walk(int depth, int position, int setMarkers, AtomicLong fulls, long maxFulls) {
//            System.out.println("ppp " + position + " edge " + ((board.edge>>2)+1));
        if (fulls.get() >= maxFulls ||
            (depth == 0 && position > (board.edge >> 2) + 1)) { // idea 3 + idea 5
            return;
        }
        if (depth <= 1 || fulls.get() % 1000000 == 0) {
            System.out.printf(
                    Locale.ROOT,
                    "edge=%d, depth=%4d, pos=%5d, nextNeutral=%5d, markers=%d/%d/%d, neutrals=%4d, fulls=%d\n",
                    board.edge, depth, position, board.nextNeutral(position),
                    setMarkers, bestMarkers, board.flatLength(),
                    board.neutralCount(), fulls.get());
        }
//            System.out.println(board);
//            System.out.println();
        if (setMarkers + (board.flatLength() - position) < bestMarkers) {
            return; // We can never beat bestMarkers
        }
        // TODO: Seems we don't iterate the starting point (initial depth)!?
        while ((position = board.nextNeutral(position)) != -1) {

            int illegalCount = board.updateIllegals(position);
            board.change(position, board.illegalsBuffer, illegalCount);
            if (bestMarkers < setMarkers + 1) {
                bestMarkers = setMarkers + 1;
                bestFlat = board.getFlatCopy();
                //    System.out.println(board + " fulls:" + fulls.get());
                //    System.out.println();
            }

            walk(depth + 1, position + 1, setMarkers + 1, fulls, maxFulls);
            board.rollback();
            ++position;
        }
        fulls.incrementAndGet();
    }

    public int getBestMarkers() {
        return bestMarkers;
    }

    public Mapper getBestMapper() {
        Mapper mapper = new Mapper(board.edge);
        mapper.setFlat(bestFlat);
        return mapper;
    }
}
