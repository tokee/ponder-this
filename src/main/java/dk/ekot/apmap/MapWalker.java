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

    public Mapper getBestBoard() {
        return bestBoard;
    }

    public void walkFlexible(int maxStaleMS, boolean showBest, int showBoardIntervalMS, boolean updatePriorities,
                             boolean returnOnFirstBottom, PriorityAdjuster.FILLER priorityFiller) {
        final long startTime = System.currentTimeMillis();

        long maxNanoTime = System.nanoTime() + maxStaleMS*1000000L;
        Mapper.PriorityPosXY zeroPos = new Mapper.PriorityPosXY();
        Mapper.PriorityPosXY startingPos = board.nextPriority(zeroPos);
        if (startingPos == null) {
            throw new IllegalStateException("Cannot find initial starting point for edge=" + board.edge);
        }
        //board.adjustPrioritiesCenterBad(); // Best 20211218
        //board.adjustPrioritiesCenterGood();
        //board.adjustPrioritiesByTripleCount(); // Very heavy!
        PriorityAdjuster.adjustPriorities(board, priorityFiller);
        int depth = 0;
        PositionsPool pool = new PositionsPool(100, board.valids, (pDepth, positions) -> {
            if (pDepth == 0) {
                positions.addAll(board.getTopLeftPositions());
            } else {
                board.fillPositionsWithNeutralsByPriority(positions);
                //positions.addAll(board.getLazyPositions(walkPrioritizer).stream().
                //        map(lazy -> lazy.pos).collect(Collectors.toList()));
            }
        });
        //positions.set(0, board.getPositions(walkPrioritizer));

        long nextShow = System.currentTimeMillis() + showBoardIntervalMS;

        // Invariants: (xs[depth], ys[depth]) are always neutral for current level

        while (depth < board.valids) {
            //System.out.println("--------------------------------------------------");
            //System.out.println(board);
            //System.out.println("depth=" + depth + ", neutrals="+ board.getNeutralCount() + "/" + board.valids);
            //if (board.getNeutralCount() != board.deepCountNeutrals()) {
            //    throw new IllegalStateException("Too many neutrals!");
            //}

            if (System.currentTimeMillis() > nextShow) {
                System.out.println(board + " time triggered show, depth=" + depth);
                System.out.println("---------------------------");
                nextShow = System.currentTimeMillis()+showBoardIntervalMS;
            }

            if (System.nanoTime() > maxNanoTime) {
                log.debug("Stopping because of timeout with edge=" + board.edge +
                          ", marked=" + board.marked + "/" + board.valids);
                bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
                return;
            }

            // Change board
//            System.out.printf("m: posIndex[depth=%d]==%d, positions.get(depth=%d).size()==%d\n", depth, posIndex[depth], depth, positions.get(depth).size());
            board.markAndDeltaExpand(pool.get(depth).current(), updatePriorities);

            // Check is a new max has been found
            if (bestMarkers < board.getMarkedCount()) {
                maxNanoTime = System.nanoTime() + maxStaleMS*1000000L; // Reset timeout
                bestMarkers = board.getMarkedCount();
                bestBoard = board.copy(false);
                if (showBest) {
//                    System.out.println(board + " fulls:" + fulls);
                    System.out.printf(Locale.ROOT, "edge=%d, markers=%5d/%6d/%d: %s\n",
                                      board.edge, board.marked, board.getNeutralCount(), board.valids, board.toJSON());
                }
            }

            // Check if descending is possible with the changed board
            if (!pool.get(depth+1).isEmpty()) {
                // Descend
                ++depth;
//                System.out.printf("s: posIndex[depth=%d]==%d, positions.get(depth=%d).size()==%d\n", depth, posIndex[depth], depth, positions.get(depth).size());
                continue;
            }

            if (returnOnFirstBottom) {
                bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
                return;
            }

            // Cannot descend, rollback and either go to next position or move up
//            System.out.printf("R: posIndex[depth=%d]==%d, positions.get(depth=%d).size()==%d\n", depth, posIndex[depth], depth, positions.get(depth).size());
            while (true) {
                pool.remove(depth+1); // Clean up below
                board.rollback(updatePriorities);
//                System.out.printf("-: posIndex[depth=%d]==%d, positions.get(depth=%d).size()==%d\n", depth, posIndex[depth], depth, positions.get(depth).size());
                // Attempt to move to next position at the current depth

                int previousPos = pool.get(depth).current();
                if (pool.get(depth).next() == -1) {
//                    System.out.printf("r: posIndex[depth=%d]==%d, positions.get(depth=%d).size()==%d\n", depth, posIndex[depth], depth, positions.get(depth).size());
                    // No more on this level, move up
                    --depth;
                    if (depth < 0) {
                        bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
                        bestBoard.setCompleted(true);
                        return; // All tapped out
                    }
                    continue;
                }
                // There was another position, mark the previous position as visited and start over with the new position
//                System.out.println("Previous " + previousPos%board.width + ", " + previousPos/board.width);
                board.addVisited(previousPos);
/*                board.fillRotated(previousPos%board.width, previousPos/board.width, rotateBuffer);// TODO: Far too expensive with the mod and divide!
                for (int position : rotateBuffer) {
                    if (board.quadratic[position] == Mapper.NEUTRAL) {
                        board.addVisited(position);
                        pool.get(depth).remove(position);
                    }
                }
                if (pool.get(depth).isEmpty()) { // TODO: Avoid code duplication
                    // No more on this level, move up
                    --depth;
                    if (depth < 0) {
                        bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
                        bestBoard.setCompleted(true);
                        return; // All tapped out
                    }
                    continue;
                }*/

                //board.addVisitedRotated(previousPos%board.width, previousPos/board.width);

//                System.out.printf("b: posIndex[depth=%d]==%d, positions.get(depth=%d).size()==%d\n", depth, posIndex[depth], depth, positions.get(depth).size());
                break;
            }
        }
        bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
        bestBoard.setCompleted(true);
    }
    final int[] rotateBuffer = new int[5];

    public void walkPriority(int maxStaleMS, boolean showBest, int showBoardIntervalMS) {
        final long startTime = System.currentTimeMillis();

        long maxNanoTime = System.nanoTime() + maxStaleMS*1000000L;
        Mapper.PriorityPosXY zeroPos = new Mapper.PriorityPosXY();
        Mapper.PriorityPosXY startingPos = board.nextPriority(zeroPos);
        if (startingPos == null) {
            throw new IllegalStateException("Cannot find initial starting point for edge=" + board.edge);
        }
        int depth = 0;
        Mapper.PriorityPosXY[] positions = new Mapper.PriorityPosXY[board.valids + 1];
        positions[0] = startingPos;
        long fulls = 0;
        long nextShow = System.currentTimeMillis() + showBoardIntervalMS;

        // Invariants: (xs[depth], ys[depth]) are always neutral for current level

        while (depth < board.valids && (depth > 0 || positions[0].y == 0)) {
//            System.out.println("--------------------------------------------------");
//            System.out.println(board);
//            System.out.println("depth=" + depth + ", xs[depth]==" + xs[depth] + ", xy[depth]==" + ys[depth]);

            if (System.currentTimeMillis() > nextShow) {
                System.out.println(board + " time triggered show, depth=" + depth);
                System.out.println("---------------------------");
                nextShow = System.currentTimeMillis()+showBoardIntervalMS;
            }

            if (System.nanoTime() > maxNanoTime) {
                log.debug("Stopping because of timeout with edge=" + board.edge +
                          ", marked=" + board.marked + "/" + board.valids);
                bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
                return;
            }

            // Change board
            board.markAndDeltaExpand(positions[depth], true);

            // Check is a new max has been found
            if (bestMarkers < board.getMarkedCount()) {
                maxNanoTime = System.nanoTime() + maxStaleMS*1000000L; // Reset timeout
                bestMarkers = board.getMarkedCount();
                bestBoard = board.copy(false);
                if (showBest) {
//                    System.out.println(board + " fulls:" + fulls);
                    System.out.printf(Locale.ROOT, "edge=%d, markers=%5d/%6d/%d: %s\n",
                                      board.edge, board.marked, board.getNeutralCount(), board.valids, board.toJSON());
                }
            }

            // Check if descending is possible with the changed board
            Mapper.PriorityPosXY descendPos = board.nextPriority(zeroPos);
            //Mapper.PriorityPos descendPos = board.nextPriority(positions[depth]);
            if (descendPos != null) {
                positions[depth+1] = descendPos;
                ++depth;
                continue;
            }

            // Cannot descend, rollback and either go to next position or move up
            ++fulls;
            while (true) {
                board.rollback(true);
                positions[depth] = board.nextPriority(positions[depth]);
                if (positions[depth] == null) {
                    // No more on this level, move up
                    --depth;
                    if (depth < 0) {
                        bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
                        return; // All tapped out
                    }
                    continue;
                }
                break;
            }
        }
        bestBoard.setWalkTimeMS(System.currentTimeMillis()-startTime);
        bestBoard.setCompleted(true);
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

            board.markAndDeltaExpand(xs[depth], ys[depth], false);

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
                board.rollback(false);
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

            board.markAndDeltaExpand(x, y, false);

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
            board.rollback(false);
            ++x; // TODO: Check for width here so all the checks in nextNeutral can be skipped?
        }
        fulls.incrementAndGet();
    }

}
