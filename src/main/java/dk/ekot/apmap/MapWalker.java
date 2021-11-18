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
 * Walker based on a quadratic the board using {@link Mapper} directly.
 */
public class MapWalker {
    final Mapper board;
    int bestMarkers = 0;
    Mapper bestBoard;

    public MapWalker(Mapper board) {
        this.board = board;
        bestBoard = board.copy();
    }

    public void walk() {
        walk(Integer.MAX_VALUE);
    }

    /**
     * Perform exhaustive walk.
     */
    public void walk(int maxMS) {
        AtomicLong fulls = new AtomicLong(0);
        long position = board.nextNeutral(0, 0);
        int x = (int) (position >> 32);
        int y = (int) (position & 0xFFFFFFFFL);
        walk(-1, x, y, new int[board.width*board.height], 0, fulls, System.nanoTime()+(maxMS*1000000L));
    }

    public Mapper getBestBoard() {
        return bestBoard;
    }

    private void walk(int depth, int x, int y, int[] changed, int changedIndex, AtomicLong fulls, long maxNanotime) {
//            System.out.println("ppp " + position + " edge " + ((board.edge>>2)+1));
        if (System.nanoTime() > maxNanotime || (depth == 0 && y > 0)) { // TODO: Implement idea 3
            return;
        }
        if (false && (depth <= 40 || fulls.get() % 100 == 1)) {
            long nextNeutral = board.nextNeutral(x, y);
            int nextX = nextNeutral == -1 ? -1 : (int) (nextNeutral >> 32);
            int nextY = nextNeutral == -1 ? -1 : (int) (nextNeutral & 0xFFFFFFFFL);
            System.out.printf(
                    Locale.ROOT,
                    "edge=%d, depth=%4d, pos=(%3d, %3d), nextNeutral=(%3d, %3d), markers=%d/%d/%d, neutrals=%4d, fulls=%d\n",
                    board.edge, depth, x, y, nextX, nextY,
                    board.getMarkedCount(), bestMarkers, board.valids,
                    board.getNeutralCount(), fulls.get());
        }
//            System.out.println(board);
//            System.out.println();
        // TODO: Early terminate if we can never improve the bextMarkers
        //if (board.getMarkedCount() + (board.valids - position) < bestMarkers) {
//            return; // We can never beat bestMarkers
  //      }
        // TODO: Seems we don't iterate the starting point (initial depth)!?
        long position;
        while ((position = board.nextNeutral(x, y)) != -1) {
            if (System.nanoTime() > maxNanotime) {
                return;
            }
            x = (int) (position >> 32);
            y = (int) (position & 0xFFFFFFFFL);

            int afterChange = board.markAndDeltaExpand(x, y, changed, changedIndex);

            if (bestMarkers < board.getMarkedCount()) {
                bestMarkers = board.getMarkedCount();
                bestBoard = board.copy();
                //    System.out.println(board + " fulls:" + fulls.get());
                //    System.out.println();
            }

            walk(depth+1, x+1, y, changed, afterChange, fulls, maxNanotime);
            board.rollback(changed, changedIndex, afterChange);
            ++x; // TODO: Check for width here so all the checks in nextNeutral can be skipped?
        }
        fulls.incrementAndGet();
    }

}
