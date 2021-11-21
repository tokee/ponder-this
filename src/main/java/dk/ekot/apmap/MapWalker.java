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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Walker based on a quadratic the board using {@link Mapper} directly.
 */
public class MapWalker {
    private static final Logger log = LoggerFactory.getLogger(MapWalker.class);

    final Mapper board;
    int bestMarkers = 0;
    Mapper bestBoard;

    public MapWalker(Mapper board) {
        this.board = board;
        bestBoard = board.copy(true);
    }

    public void walk() {
        walk(Integer.MAX_VALUE, true);
    }

    /**
     * Perform exhaustive walk.
     */
    public void walk(int maxMS, boolean showBest) {
        AtomicLong fulls = new AtomicLong(0);
        long position = board.nextNeutral(0, 0);
        int x = (int) (position >> 32);
        int y = (int) (position & 0xFFFFFFFFL);
        walk(-1, x, y, fulls, System.nanoTime()+(maxMS*1000000L), showBest);
    }

    public Mapper getBestBoard() {
        return bestBoard;
    }

    public void walkPriority(int maxStaleMS, boolean showBest) {
        long maxNanoTime = System.nanoTime() + maxStaleMS*1000000L;
        Mapper.PriorityPos startingPos = new Mapper.PriorityPos();
        if ((startingPos = board.nextPriority(startingPos)) == null) {
            throw new IllegalStateException("Cannot find initial starting point for edge=" + board.edge);
        }
        int depth = 0;
        Mapper.PriorityPos[] positions = new Mapper.PriorityPos[board.valids];
        positions[0] = startingPos;
        long fulls = 0;

        // Invariants: (xs[depth], ys[depth]) are always neutral for current level

        while (depth < board.valids && (depth > 0 || positions[0].y == 0)) {
//            System.out.println("--------------------------------------------------");
//            System.out.println(board);
//            System.out.println("depth=" + depth + ", xs[depth]==" + xs[depth] + ", xy[depth]==" + ys[depth]);

            if (System.nanoTime() > maxNanoTime) {
                log.debug("Stopping because of timeout with edge=" + board.edge +
                          ", marked=" + board.marked + "/" + board.valids);
                break;
            }

            // Change board
            int before = board.neutrals;
            board.markAndDeltaExpand(positions[depth]);
            if (bestMarkers < board.getMarkedCount()) {
                maxNanoTime = System.nanoTime() + maxStaleMS*1000000L; // Reset timeout
                bestMarkers = board.getMarkedCount();
                bestBoard = board.copy(true);
                if (showBest) {
//                    System.out.println(board + " fulls:" + fulls);
                    System.out.printf(Locale.ROOT, "edge=%d, markers=%5d/%6d/%d: %s\n",
                                      board.edge, board.marked, board.getNeutralCount(), board.valids, board.toJSON());
                }
            }

            // Check if descending is possible with the changed board
            Mapper.PriorityPos descendPos = board.nextPriority(positions[depth]);
            if (descendPos != null) {
                positions[depth+1] = descendPos;
                ++depth;
                continue;
            }

            // Cannot descend, rollback and either go to next position or move up
            ++fulls;
            while (true) {
                board.rollback();
                Mapper.PriorityPos nextPos = board.nextPriority(positions[depth]);
                if (nextPos == null) {
                    // No more on this level, move up
                    --depth;
                    if (depth < 0) {
                        return; // All tapped out
                    }
                    continue;
                }
                positions[depth] = nextPos;
                break;
            }
        }
    }

    public void walkStraight(int maxStaleMS, boolean showBest) {
        long maxNanoTime = System.nanoTime() + maxStaleMS*1000000L;
        long position = board.nextNeutral(0, 0);
        int startX = (int) (position >> 32);
        int startY = (int) (position & 0xFFFFFFFFL);

        int depth = 0;
        int[] xs = new int[board.valids]; xs[0] = startX;
        int[] ys = new int[board.valids]; ys[0] = startY;
        long fulls = 0;

        // Invariants: (xs[depth], ys[depth]) are always neutral for current level

        while (depth < board.valids && (depth > 0 || ys[0] == 0)) {
//            System.out.println("--------------------------------------------------");
//            System.out.println(board);
//            System.out.println("depth=" + depth + ", xs[depth]==" + xs[depth] + ", xy[depth]==" + ys[depth]);

            if (System.nanoTime() > maxNanoTime) {
                log.debug("Stopping because of timeout with edge=" + board.edge +
                          ", marked=" + board.marked + "/" + board.valids);
                break;
            }

            // Change board
            int before = board.neutrals;
            board.markAndDeltaExpand(xs[depth], ys[depth]);
            if (bestMarkers < board.getMarkedCount()) {
                maxNanoTime = System.nanoTime() + maxStaleMS*1000000L; // Reset timeout
                bestMarkers = board.getMarkedCount();
                bestBoard = board.copy(true);
                if (showBest) {
                    System.out.println(board + " fulls:" + fulls);
                    System.out.printf(Locale.ROOT, "edge=%d, markers=%5d/%6d/%d: %s\n",
                                      board.edge, board.marked, board.getNeutralCount(), board.valids, board.toJSON());
                }
            }

            // Check if descending is possible with the changed board
            long descendPosition = board.nextNeutral(xs[depth] + 1, ys[depth]);
            if (descendPosition != -1) {
                // All OK, commence descend
                xs[depth + 1] = (int) (descendPosition >> 32);
                ys[depth + 1] = (int) (descendPosition & 0xFFFFFFFFL);
                ++depth;
                continue;
            }

            // Cannot descend, rollback and either go to next position or move up
            ++fulls;
            while (true) {
                // TODO: There is a double rollback here as neutrals goes in the zeroes
                before = board.neutrals;
                board.rollback();
                ++xs[depth];
                long nextPosition = board.nextNeutral(xs[depth], ys[depth]);
                if (nextPosition == -1) {
                    // No more on this level, move up
                    --depth;
                    if (depth < 0) {
                        return; // All tapped out
                    }
                    continue;
                }
                xs[depth] = (int) (nextPosition >> 32);
                ys[depth] = (int) (nextPosition & 0xFFFFFFFFL);
                break;
            }
        }
    }

    private void walk(int depth, int x, int y, AtomicLong fulls, long maxNanotime, boolean showBest) {
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

            board.markAndDeltaExpand(x, y);

            if (bestMarkers < board.getMarkedCount()) {
                bestMarkers = board.getMarkedCount();
                bestBoard = board.copy(true);
                if (showBest) {
                    //System.out.println(board + " fulls:" + fulls.get());
                    System.out.printf(Locale.ROOT, "edge=%d, markers=%d/%d: %s\n",
                                      board.edge, board.marked, board.valids, board.toJSON());
                }
            }

            walk(depth+1, x+1, y, fulls, maxNanotime, showBest);
            board.rollback();
            ++x; // TODO: Check for width here so all the checks in nextNeutral can be skipped?
        }
        fulls.incrementAndGet();
    }

}
