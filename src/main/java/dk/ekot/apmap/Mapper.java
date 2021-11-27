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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Transforms from and to flat and quadratic representations of a hexagonal board. Supports visualization.
 *
 * The primary coordinate system is quadratic and is optimized towards
 * - Fast get/set of content at a given quadratic coordinate
 * - Fast resolving of new quadratic coordinates given a quadratic origo and a delta
 * - Local memory access by using a single array to hold the matrix.
 *
 * Coordinate systems:
 *
 * hex: As defined in the assignment: Top-down row based hexagon with upper left as (0, 0)
 *
 * flat: The rows from the hexagon packed directly after each other.
 *
 * quadratic:
 *   0:         X   X   .   X   X
 *   1:       X   X   .   .   X   X
 *   2:     .   .   .   X   .   .   .
 *   3:   X   .   .   .   .   .   .   X
 *   4: X   X   .   .   .   .   .   X   X
 *   5:   X   .   X   .   .   X   .   X
 *   6:     .   .   .   .   .   .   .
 *   7:       X   X   .   .   X   X
 *   8:         X   X   .   X   X
 *   the hexagon represented on a quadratic board. Allows for easy delta calculations.
 */
public class Mapper {
    private static final Logger log = LoggerFactory.getLogger(Mapper.class);

    static final int INVALID = -1; // Outside of the board
    static final int NEUTRAL = 0;  // Valid but unset
    static final int MARKER = 1;   // Marked
    static final int ILLEGAL = 2;  // Cannot be set (will result in AP)
    static final int VISITED = 3;  // Previously visited in this descend tree (do not mark again)

    final int edge; // Hexagonal edge
    final int width;
    final int height;
    final int valids;
    final int[] quadratic; // top-down, left-right. (0, 0) is top left
    final int[] priority;  // 0-∞. Lower numbers are better (idea #13)

    final int[] boardChanges;   // Change tracker
    final int[] boardChangeIndexes;
    // TODO: Remove this
    final short[] tripleDeltas; // [deltaX1, deltaY1, deltaX2, deltaY2]*

//    final long[][] tripleDeltasByColumn; // [deltaX1, deltaY1, deltaX2, deltaY2]*
//    final long[][] tripleDeltasByRow; // [deltaX1, deltaY1, deltaX2, deltaY2]*

    int marked = 0;
    int neutrals;
    int changeIndexPosition = 0;
    boolean completed = false;
    long walkTimeMS = 0;

    /*
    final int[] flatToQuadratic;
    final int[] flatToHex;
    final int[][] quadraticToFlat;
    final int[][] quadraticToHex;
    final int[][] hexToFlat;
    final int[][] hexToQuadratic;
      */
    /**
     * @param edge the edge of the hexagonal board.
     */
    public Mapper(int edge) {
        this.edge = edge;
        width = edge*4-3;
        height = edge*2-1;
        quadratic = new int[height*width];
        priority = new int[height*width];
        boardChanges = new int[height * width * 2]; // Times 2 as they are coordinates
        boardChangeIndexes = new int[height * width];

        // Draw the quadratic map
        Arrays.fill(quadratic, INVALID);
        final AtomicInteger v = new AtomicInteger(0);
        visitAll(pos -> {
            quadratic[pos] = NEUTRAL;
            v.incrementAndGet();
        });
        valids = v.get();
        neutrals = valids;
        tripleDeltas = getTripleDeltas();
//        tripleDeltasByColumn = getDeltaColumns();
//        tripleDeltasByRow = getDeltaRows();
    }

    private Mapper(Mapper other, boolean viewOnly) {
        this.edge = other.edge;
        this.height = other.height;
        this.width = other.width;
        this.valids = other.valids;

        this.quadratic = Arrays.copyOf(other.quadratic, other.quadratic.length);
        if (viewOnly) {
            this.priority = null;
            this.boardChanges = null;
            this.boardChangeIndexes = null;
        } else {
            this.priority = Arrays.copyOf(other.priority, other.priority.length);
            this.boardChanges = Arrays.copyOf(other.boardChanges, other.boardChanges.length);
            this.boardChangeIndexes = Arrays.copyOf(other.boardChangeIndexes, other.boardChangeIndexes.length);
        }

        this.marked = other.marked;
        this.neutrals = other.neutrals;
        this.changeIndexPosition = other.changeIndexPosition;
        this.completed = other.completed;

        this.tripleDeltas = Arrays.copyOf(other.tripleDeltas, other.tripleDeltas.length);
//        this.tripleDeltasByRow = Arrays.copyOf(other.tripleDeltasByRow, other.tripleDeltasByRow.length);
//        this.tripleDeltasByColumn = Arrays.copyOf(other.tripleDeltasByColumn, other.tripleDeltasByColumn.length);
    }

    /**
     * Find the next neutral entry, starting at the quadratic position and looking forward on the board.
     * The coordinates can be outside of the board, and will be moved left->right, top->down until hitting the
     * board of falling out of the bottom.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @return the next neutral position (x as the highest 32 bits, y as the lowest) or -1 if there are none.
     */
    public final long nextNeutral(int x, int y) {
        x = Math.max(0, x);
        y = Math.max(0, y);
        if (x >= width) {
            x = 0 ;
            ++y;
        }
        if (y >= height) {
            return INVALID;
        }

        for (int qy = y ; qy < height ; qy++) {
            for (int qx = qy == y ? x : 0 ; qx < width ; qx++) { // TODO: If we have the right start, use x+=2
                int element = getQuadratic(qx, qy);
                if (element == NEUTRAL) {
                    return (((long)qx)<<32) | ((long)qy);
                }
            }
        }
        return -1;
    }

    /**
     * Fast positions ordering by only comparing positions and priority. Sort order:
     * 1) Priority (lower is better)
     * 2) Position (lower is better)
     * @return a comparator with the described characteristica.
     */
    public static Comparator<LazyPos> getPriorityComparator() {
        return (lazy1, lazy2) ->
                lazy1.posPriority < lazy2.posPriority ? -1 :
                        lazy1.posPriority > lazy2.posPriority ? 1 :
                                Integer.compare(lazy1.pos, lazy2.pos);
    }
    /**
     * Get all positions that are neutral, sorted by the given comparator.
     * @param comparator determines the order of the result.
     * @return all neutral (aka free) positions, sorted by comparator.
     */
    public List<LazyPos> getPositions(Comparator<LazyPos> comparator) {
        List<LazyPos> positions = new ArrayList<>(valids);
        visitAll(pos -> {
            if (quadratic[pos] == NEUTRAL) {
                positions.add(new LazyPos(pos));
            }
        });
        positions.sort(comparator);
        return positions;
    }

    /**
     * Position with the special property {@link LazyPos#priorityChanges} which is only evaluated if requested.
     */
    public class LazyPos {
        public final int pos;
        public final int posPriority;

        /**
         * Initialize with index into {@link #quadratic}.
         * @param pos index into {@link #quadratic}.
         */
        public LazyPos(int pos) {
            this.pos = pos;
            posPriority = priority[pos];
        }

        /**
         * The number of priority changes that will be performed of the position is marked.
         * This number is only valid as long as the overall state of the board is the same as when it was calculated.
         */
        AtomicInteger priorityChanges = new AtomicInteger(-1); // -1 = unresolved

        /**
         * This method performs a modulo operation. Do not call it many times in a tight inner loop!
         * @return x position in the quadratic coordinate system.
         */
        public int getX() {
            return pos%width;
        }
        /**
         * This method performs a division operation. Do not call it many times in a tight inner loop!
         * @return y position in the quadratic coordinate system.
         */
        public int getY() {
            return pos/width;
        }

        /**
         * @return the umber of priority changes setting a mark at this position would cause.
         */
        public int getPriorityChanges() {
            if (priorityChanges.get() == -1) {
                priorityChanges.set(0);
                visitTriples(getX(), getY(), (pos1, pos2) -> {
                    if (quadratic[pos1] == NEUTRAL) {
                        priorityChanges.incrementAndGet();
                    }
                    if (quadratic[pos2] == NEUTRAL) {
                        priorityChanges.incrementAndGet();
                    }
                });
            }
            return priorityChanges.get();
        }

    }

    /**
     * Find the next available {@link #NEUTRAL} element after the current. Seeking is done left-right, top-down from
     * {@code (pos.x+1, pos.y)} for {@code priority == pos.priority}. If nothing is found, a seek new left->down,
     * top-> down seek for {@code priority lower than pos.priority, starting at {@code 0, 0} is performed. If that does
     * not match anything, the search is considered exhausted.
     * @param position the starting position.
     * @return the new position if possible, else null.
     */
    public final PriorityPos nextPriority(PriorityPos origo) {
        // TODO: Switch to visitAll here
        int x = Math.max(0, origo.x+1);
        int y = Math.max(0, origo.y);
        int p = origo.priority;
        if (x >= width) {
            x = 0 ;
            ++y;
        }
        if (y >= height) {
            return null;
        }

        // Search for same priority
        for (int qy = y ; qy < height ; qy++) {
            for (int qx = qy == y ? x : 0 ; qx < width ; qx++) { // TODO: If we have the right start, use x+=2
                int element = getQuadratic(qx, qy);
                if (element == NEUTRAL && p == getPriority(qx, qy)) {
                    return new PriorityPos(qx, qy, origo.priority);
                }
            }
        }

        //  0:   X   X
        //  1: X   X   1
        //  2:   1   1
        //neutralB(4, 1)
        //  0:   X   X
        //  1: X   X   X
        //  2:   1   1

        // Search for worse priority
        int bestX = -1;
        int bestY = -1;
        int bestP = Integer.MAX_VALUE;
        for (int qy = 0 ; qy < height ; qy++) {
            for (int qx = 0 ; qx < width ; qx++) { // TODO: If we have the right start, use x+=2
                int element = getQuadratic(qx, qy);
                if (element == NEUTRAL) {
                    int currentP = getPriority(qx, qy);
                    if (p < currentP) { // Worse that original priority
                        if (currentP < bestP) { // But better than what we have see on the seconds pass
                            bestX = qx;
                            bestY = qy;
                            bestP = currentP;
                            if (bestP == p + 1) { // Exactly 1 better than original, cannot get better
                                return new PriorityPos(bestX, bestY, bestP);
                            }
                            // Room for improvement, keep searching
                        }
                    }
                }
            }
        }
        if (bestX == -1) {
            return null;
        }
        return new PriorityPos(bestX, bestY, bestP);
    }

    final static class PriorityPos {
        public final int x;
        public final int y;
        public final int priority;

        public PriorityPos() {
            this(-1, 0, 0);
        }

        public PriorityPos(int x, int y, int priority) {
            this.x = x;
            this.y = y;
            this.priority = priority;
        }

        public PriorityPos copy() {
            return new PriorityPos(x, y, priority);
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + priority + ")";
        }
    }


    /**
     * Getter that allows requests outside of the board. In that case {@link #INVALID} is returned.
     * Does not update {@link #neutrals} and {@link #marked}.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @return the element at the given coordinates or {@link #INVALID} if outside the board.
     */
    public final int getQuadratic(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return INVALID;
        }
        return quadratic[y*width+x];
    }

    /**
     * The setter does not check if the coordinates are legal and does not update {@link #neutrals} and {@link #marked}.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @param element the element to set at the coordinates.
     */
    public final void setQuadratic(int x, int y, int element) {
        quadratic[y*width+x] = element;
    }

    /**
     * Decrease the priority of the given field. The coordinates must be on the board.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     */
    public final int decreasePriority(int x, int y) {
        return ++priority[y * width + x]; // Yes, decreasing priority means a higher number
    }

    /**
     * Increase the priority of the given field. The coordinates must be on the board.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     */
    public final int increasePriority(int x, int y) {
        return --priority[y * width + x]; // Yes, increasing priority means a lower number
    }

    /**
     * Increase the priority of the given field. The coordinates must be on the board.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @param priorityDelta the adjustment.
     */
    public final int adjustPriority(int x, int y, int priorityDelta) {
        return priority[y * width + x] += priorityDelta;
    }

    /**
     * Get the priority of the given field, lower numbers are better, highest priority is 0.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @return the priority of the given field (0-n, 0 is best/highest/preferable).
     */
    public final int getPriority(int x, int y) {
        return priority[y * width + x];
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getWalkTimeMS() {
        return walkTimeMS;
    }

    public void setWalkTimeMS(long walkTimeMS) {
        this.walkTimeMS = walkTimeMS;
    }

    public int getMarkedCount() {
        return marked;
    }

    public int getNeutralCount() {
        return neutrals;
    }

    public final void markAndDeltaExpand(PriorityPos pos) {
        markAndDeltaExpand(pos.x, pos.y);
    }
    public final void markAndDeltaExpand(LazyPos pos) {
        markAndDeltaExpand(pos.getX(), pos.getY()); // TODO: Make this fast. Maye with x & y as first class?
    }

    /**
     * Set the given pos to VISITED and add the position to the current rollback-list.
     * Performing a rollback after this will rollback whatever was on the rollback list plus the newly added pos.
     * @param pos an index into {@link #quadratic} which will be set to visited.
     */
    public void addVisitedToCurrent(final int pos) {
        quadratic[pos] = VISITED;
        boardChanges[boardChangeIndexes[changeIndexPosition]++] = pos;
    }

    /**
     * Marks the given quadratic (x, y) and adds the coordinated to changed at changedIndex.
     * Uses the {@link #tripleDeltas} to resolve all fields that are neutral and where setting a mark would cause
     * a triple (arithmetic progression). The fields are set to {@link #ILLEGAL} and their coordinate pairs are added
     * to changed, with changedIndex being incremented accordingly.
     * This updates {@link #marked} and {@link #neutrals}.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @return the new changedIndex. Will always be at least 2 more than previously.
     */
    public void markAndDeltaExpand(final int x, final int y) {
        ++changeIndexPosition;
        boardChangeIndexes[changeIndexPosition] = boardChangeIndexes[changeIndexPosition - 1];
        final int origoPos = y*width+x;
        // TODO: Simplify so that boardChanges is only indexes, not a mix of (x, y) and indexes
        boardChanges[boardChangeIndexes[changeIndexPosition]++] = x;
        boardChanges[boardChangeIndexes[changeIndexPosition]++] = y;
        if (quadratic[origoPos] != NEUTRAL) {
            throw new IllegalStateException(
                    "Attempted to mark (" + x + ", " + y + ") bit it already had state " + quadratic[origoPos]);
        }

        quadratic[origoPos] = MARKER;
        ++marked;
        --neutrals;
        visitTriples(x, y, (pos1, pos2) -> {
            final int pos1Element = quadratic[pos1];
            final int pos2Element = quadratic[pos2];

            if (pos1Element == MARKER && pos2Element == NEUTRAL) {
                quadratic[pos2] = ILLEGAL;
                boardChanges[boardChangeIndexes[changeIndexPosition]++] = pos2;
                --neutrals;
            } else if (pos2Element == MARKER && pos1Element == NEUTRAL) {
                quadratic[pos1] = ILLEGAL;
                boardChanges[boardChangeIndexes[changeIndexPosition]++] = pos1;
                --neutrals;
            }
        });
        // TODO: Make this part of the visitTriples above
        adjustPriorities(x, y, 1);

        //  0:   X   X
        //  1: X   3   1
        //  2:   1   1
        //
        //  0:   X   X
        //  1: X   X   1
        //  2:   1   1
        //---- marked(2, 1)

        //System.out.printf("\n%s\n---- marked(%d, %d)\n", this, x, y);
    }

    /**
     * Mark all quadratic coordinate pairs in {@code changed[from..to]} (to is exclusive) as {@link #NEUTRAL}.
     * This performs {@code --marked} and {@code neutrals += (to-from)/2}.
     */

    public void rollback() {
        //System.out.println(this);
        int start = boardChangeIndexes[changeIndexPosition - 1];

        final int x =  boardChanges[start++];
        final int y =  boardChanges[start++];
//        System.out.printf("popping(%d, %d) from %d\n", x, y, boardChangeIndexes[changeIndexPosition-1]);
        adjustPriorities(x, y, -1); // Must be before the clearing of the marker below
        setQuadratic(x, y, NEUTRAL);

        for (int i = start; i < boardChangeIndexes[changeIndexPosition] ; i++) {
            quadratic[boardChanges[i]] = NEUTRAL;
        }
        --marked;
        neutrals += (boardChangeIndexes[changeIndexPosition] - boardChangeIndexes[changeIndexPosition - 1])-1;
        --changeIndexPosition;
        //System.out.printf("\n%s\n---- rollbacked(%d, %d)\n", this, x, y);
    }


    private void adjustPriorities(int x, int y, int priorityDelta) {
        visitTriples(x, y, (pos1, pos2) -> {
            // TODO: Can't one just adjust even when there is a marker?
            if (quadratic[pos1] != MARKER) {
                priority[pos1] += priorityDelta;
            }
            if (quadratic[pos2] != MARKER) {
                priority[pos2] += priorityDelta;
            }
        });
    }


    public Mapper copy() {
        return new Mapper(this, false);
    }

    public Mapper copy(boolean viewOnly) {
        return new Mapper(this, viewOnly);
    }

    /**
     * @return the number of valid elements in the quadratic representation (same number as the abstract hexagon).
     */
    public int validCount() {
        return valids; // Never changes
/*        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (quadratic[y][x] != INVALID) {
                    count++;
                }
            }
        }
        return count;*/
    }

    /**
     * @return the state of the map flattened to a single array.
     */
    public int[] getFlat() {
        int[] flat = new int[validCount()];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (getQuadratic(x, y) != INVALID) {
                    flat[index++] = getQuadratic(x, y);
                }
            }
        }
        return flat;
    }

    /**
     * Set the state of the map to the given flat structure.
     * @param flat a flattened view, similar to the one obtained by {@link #getFlat()}.
     */
    public void setFlat(int[] flat) {
        if (flat.length != validCount()) {
            throw new IllegalArgumentException(
                    "The length of the flat structure was " + flat.length +
                    " while the number of vali entries in the map was " + validCount());
        }
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (getQuadratic(x, y) != INVALID) {
                    setQuadratic(x, y, flat[index++]);
                }
            }
        }
    }

    /**
     * Replace regular valid entries with indices to flat representation.
     * After this the map is no longer functioning. Use only for internal trickery!
     */
    private Mapper fillWithFlatIndices() {
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (getQuadratic(x, y) != INVALID) {
                    setQuadratic(x, y, index++);
                }
            }
        }
        return this;
    }

    /**
     * Iterates all flat positions and for each generates an array of triples of flat-indices where marking all entries
     * in any given triple would be invalid.
     * @return {@code int[flatIndex][triple(3*flatIndex)Index]}
     */
    public int[][] getFlatTriples() {
        final Mapper flatIndices = copy().fillWithFlatIndices();
        int[][] tripless = new int[flatIndices.validCount()][];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (flatIndices.getQuadratic(x, y) != INVALID) {
                    tripless[flatIndices.getQuadratic(x, y)] = flatIndices.getFlatTriples(x, y);
                }
            }
        }
        return tripless;
    }

    /**
     * Helper method for {@link #getFlatTriples()}. Assumes that {@link #fillWithFlatIndices()} has been called.
     * @param x quadratic origo X.
     * @param y  quadratic origo Y.
     * @return an array of triples of flatIndices.
     */
    // TODO: Only the 2 coordinates besides the origo needs to be stored
    private int[] getFlatTriples(int x, int y) {
        if (!isValid(x, y)) {
            throw new IllegalStateException("Called getFlatTriples(x=" + x + ", y=" + y + ") with invalid coordinates");
        }
        List<Integer> triples = new ArrayList<>();
        for (int deltaX = 0 ; deltaX < width/2+1 ; deltaX++) {
            for (int deltaY = 0 ; deltaY < height/2+1 ; deltaY++) {
                if (deltaX == 0 && deltaY == 0) {
                    continue;
                }
                if ((deltaX&1) != (deltaY&1)) { // Both must be odd or both must be even to hit only valids
                    continue;
                }
                // Top left -> bottom right
                addFlatTriplesIfValid(triples, x-deltaX*2, y-deltaY*2, x-deltaX, y-deltaY, x, y);
                addFlatTriplesIfValid(triples, x-deltaX, y-deltaY, x, y, x+deltaX, y+deltaY);
                addFlatTriplesIfValid(triples, x, y, x+deltaX, y+deltaY, x+deltaX*2, y+deltaY*2);
                if (deltaY == 0) {
                    continue; // No need to reverse y-axis if y is o
                }
                // Bottom left -> top right
                addFlatTriplesIfValid(triples, x-deltaX*2, y+deltaY*2, x-deltaX, y+deltaY, x, y);
                addFlatTriplesIfValid(triples, x-deltaX, y+deltaY, x, y, x+deltaX, y-deltaY);
                addFlatTriplesIfValid(triples, x, y, x+deltaX, y-deltaY, x+deltaX*2, y-deltaY*2);
            }

        }
        return triples.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Calculate all possibly valid deltas for finding triples.
     * Note that origo is implicit so a single triple delta is 2 delta pairs:
     * {@code [deltaX1, deltaY1, deltaX2, deltaY2]}
     * Since all delta triples takes up exactly 4 entries in the array, they are returned as a single
     * concatenated array.
     * @return all potentially valid deltas for finding triples.
     */
    public short[] getTripleDeltas() {
        List<Integer> triples = new ArrayList<>();
        // TODO: Can the width/2+1 be reduced a tiny bit? w3->1, w4->1, w5->2, w6->2, w7->3
        for (int deltaX = 0 ; deltaX < width/2+1 ; deltaX++) {
            for (int deltaY = 0; deltaY < height/2+1; deltaY++) {
                if (deltaX == 0 && deltaY == 0) {
                    continue;
                }
                if ((deltaX & 1) != (deltaY & 1)) { // Both must be odd or both must be even to hit only valids
                    continue;
                }
                // Top left -> bottom right
                triples.add(-deltaX*2); triples.add(-deltaY*2); triples.add(-deltaX); triples.add(-deltaY);
                triples.add(-deltaX); triples.add(-deltaY); triples.add(deltaX); triples.add(deltaY);
                triples.add(deltaX); triples.add(deltaY); triples.add(deltaX*2); triples.add(deltaY*2);
                if (deltaY == 0 || deltaX == 0) {
                    continue; // No need to rotate direction 180° if either x or y is 0
                }
                // Bottom left -> top right
                triples.add(-deltaX*2); triples.add(deltaY*2); triples.add(-deltaX); triples.add(deltaY);
                triples.add(-deltaX); triples.add(deltaY); triples.add(deltaX); triples.add(-deltaY);
                triples.add(deltaX); triples.add(-deltaY); triples.add(deltaX*2); triples.add(-deltaY*2);
            }
        }
//        log.info("edge=" + edge + ", extracted " + triples.size()/4 + " delta triples " +
//                 "(" + triples.size()*4/1024 + " KBytes)");
/*        for (int i = 0 ; i < 1000 ; i+=4) {
            String human = String.format(
                    Locale.ROOT,
                    "(%d, %d)(%d, %d)\n",
                    triples.get(i), triples.get(i+1), triples.get(i+2), triples.get(i+3));
            System.out.print(human);
        }*/
        //return triples.stream().mapToInt(Integer::intValue).toArray();
        short[] sa = new short[triples.size()];
        for (int i = 0 ;i < sa.length ; i++) {
            sa[i] = triples.get(i).shortValue();
        }
        return sa;
    }

    /**
     * Provides stats for how the generic triples overlaps with ideal (position specific) triples
     * edge=578, valids=1000519, uniqueDeltas=2001903, sumDeltas=375388224777, minDeltas=249700, averageDeltas=375193, maxDeltas=749955, time=18681s
     */
    public void dumpDeltaStats() {
        final long startTimeMS = System.currentTimeMillis();
        long sum = 0;
        long max = 0;
        long min = Long.MAX_VALUE;

        for (int x = 0 ; x < width ; x++) {
            for (int y = 0; y < height; y++) {
                if (getQuadratic(x, y) != INVALID) {
                    long count = 0;
                    for (int i = 0 ; i < tripleDeltas.length ; i+=4) {
                        final int x1 = x + tripleDeltas[i];
                        final int y1 = y + tripleDeltas[i + 1];
                        final int x2 = x + tripleDeltas[i + 2];
                        final int y2 = y + tripleDeltas[i + 3];
                        if (getQuadratic(x1, y1) != INVALID &&
                            getQuadratic(x2, y2) != INVALID) {
                            ++count;
                        }
                    }
                    sum += count;
                    max = Math.max(max, count);
                    min = Math.min(min, count);
                }
            }
        }
        System.out.printf(
                Locale.ROOT,
                "edge=%d, valids=%d, uniqueDeltas=%d, sumDeltas=%d, minDeltas=%d, averageDeltas=%d, maxDeltas=%d, time=%ds",
                          edge, valids, tripleDeltas.length/4, sum, min, sum/valids, max,
                (System.currentTimeMillis()-startTimeMS)/1000);
    }

    /**
     * Visit all valid positions on the hexagon represented as quadratic.
     * @param callback delivers the indices in {@link #quadratic} for each valid element, left->right, top->down
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void visitAll(Consumer<Integer> callback) {
        int yMul = 0;
        for (int y = 0 ; y < height ; y++) {
            int margin = Math.abs(y-(height>>1));
            for (int x = margin ; x < width-margin ; x+=2) {
                callback.accept(yMul+x);
            }
            yMul += width;
        }
    }

    /**
     * Visit all valid positions on the hexagon represented as quadratic.
     * @param callback delivers the coordinates  in {@link #quadratic} for each valid element, left->right, top->down
     */
    public void visitAllXY(CoordinateCallback callback) {
        for (int y = 0 ; y < height ; y++) {
            int margin = Math.abs(y-(height>>1));
            for (int x = margin ; x < width-margin ; x+=2) {
                callback.accept(x, y);
            }
        }
    }

    /**
     * Iterate all triples connected to origo. This is equivalent to calling
     * {@link #visitTriplesIntersecting(int, int, TripleCallback)} and
     * {@link #visitTriplesRadial(int, int, TripleCallback)}.
     * @param origoX start X in the quadratic coordinate system.
     * @param origoY start Y in the quadratic coordinate system.
     */
    public void visitTriples(final int origoX, final int origoY, TripleCallback callback) {
       visitTriplesIntersecting(origoX, origoY, callback);
       visitTriplesRadial(origoX, origoY, callback);
    }
    /**
     * Iterate all triples where origo is in the middle of the triple.
     * @param origoX start X in the quadratic coordinate system.
     * @param origoY start Y in the quadratic coordinate system.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void visitTriplesIntersecting(final int origoX, final int origoY, TripleCallback callback) {
        final int origo = origoY*width+origoX;
        final int maxDistY = Math.min(origoY, height-origoY-1);
        int y1ArrayIndex = (origoY - maxDistY)*width;
        //System.out.println(this);
//        System.out.printf("origo(%d, %d), maxDistY=%d\n", origoX, origoY, maxDistY);
        // Only visit upper half, including origoY, as the bottom half is mirrored
        for (int y1 = origoY - maxDistY ; y1 <= origoY ; y1++) {

            // MarginX for the topmost and the bottommost point in the triple with origo in the middle
            int marginXTop = Math.abs(y1-(height>>1));
            int marginXBottom= Math.abs(origoY+(origoY-y1)-(height>>1));

            final int maxDeltaLeft = Math.min(origoX-marginXTop, width-marginXBottom-origoX);
            final int maxDeltaRight = Math.min(width-marginXTop-origoX, origoX-marginXBottom);

            int startX = origoX - maxDeltaLeft;
            //if ((startX&1) != (marginXTop&1)) { // TODO:Replace with with some XOR + MASK magic: (startX^marginXTop)&1 ?
            //    ++startX;
            //}
            startX += (startX^marginXTop)&1;
            int endX = origoX + maxDeltaRight;
            if (y1 == origoY) {
                endX = origoX;
            }
            
  //          System.out.printf("y1=%d  marginX:[%d %d], maxDelta[%d %d], x[%d %d]\n",
    //                          y1, marginXTop, marginXBottom, maxDeltaLeft, maxDeltaRight, startX, endX);

            for (int x1 = startX ; x1 <= endX ; x1+=2) {
                final int pos1 = y1ArrayIndex+x1;
                if (pos1 == origo) {
                    continue;
                }
                final int pos2 = origo + (origo-pos1); // 2*origo-pos1 !? Seems suspicious
      //          System.out.printf("  pos1(%d, %d)=%d, origo=%s=%d, pos2=%s=%d\n", x1, y1, pos1, toXY(origo), origo, toXY(pos2), pos2);
                callback.processValid(pos1, pos2);
            }
            y1ArrayIndex += width;
        }
    }
    private String toXY(int pos) {
        return String.format(Locale.ROOT, "(%d, %d)", pos/width, pos%width);
    }
    /**
     * Iterate all triples radiating out from origo.
     * @param origoX start X in the quadratic coordinate system.
     * @param origoY start Y in the quadratic coordinate system.
     */
    public void visitTriplesRadial(final int origoX, final int origoY, TripleCallback callback) {
        // edge 5/7: offsets=(0, 0), (2, 0)

        final int origo = origoY*width+origoX;

        int offsetX = 1 + (origoX&3);
        int offsetY = origoY&1;

        if ((origoY&3) == 1) {
            offsetX += 3;
        } else if ((origoY&3) == 2) {
            offsetX += 2;
        } else if ((origoY&3) == 3) {
            offsetX += 1;
        }
        boolean shift = true;

        offsetX -= 8;

//        System.out.printf("origo(%d, %d), Offsets (%d, %d)\n", origoX, offsetY, offsetX, offsetY);
        int y1ArrayIndex = offsetY*width;
        for (int y1 = offsetY ; y1 < height ; y1 += 2) {
            shift = !shift;
            int shiftDelta = shift ? 2 : 0;
            int marginX = Math.abs(y1-(height>>1));
            int startX = offsetX + shiftDelta-((y1+1)&1);
            while (startX < marginX) { // TODO: Make a no-conditionals "round to nearest mod 4 >= marginX"
                startX += 4;
            }
            for (int x1 = startX ; x1 <= width-marginX ; x1 += 4) {
                if (x1 == origoX && y1 == origoY) {
                    continue;
                }
                final int pos1 = y1ArrayIndex+x1;
                final int pos2 = (origo+pos1)>>1;

//                System.out.printf("origo(%d, %d), pos1(%d, %d), pos2(%d, %d), marginX=%d, board(%d, %d)\n",
//                                  origoX, origoY, x1, y1, x2, y2, marginX, width, height);
                //callback.processValid(y1*width+x1, y2*width+x2);
                callback.processValid(pos1, pos2);
            }
            y1ArrayIndex += width<<1;
        }
    }

    @FunctionalInterface
    public interface TripleCallback {
        /**
         * Called for every triple where each triple entry is on the quadratic board.
         * Note: It is not guaranteed that each triple is valid!
         * @param pos1 primary triple entry.
         * @param pos2 secondary triple entry.
         */
        void processValid(int pos1, int pos2);
    }

    @FunctionalInterface
    public interface CoordinateCallback {
        void accept(int x, int y);
    }


    /**
     * Add the triple flatPositions iff all positions are valid.
     */
    private void addFlatTriplesIfValid(List<Integer> triples, int x1, int y1, int x2, int y2, int x3, int y3) {
        if (isValid(x1, y1) && isValid(x2, y2) && isValid(x3, y3)) {
            triples.add(getQuadratic(x1, y1));
            triples.add(getQuadratic(x2, y2));
            triples.add(getQuadratic(x3, y3));
        }
    }

    /**
     * Checks if the coordinate has a valid entry.
     * @param x rectangular X, can be outside of the rectangle.
     * @param y rectangular Y, can be outside of the rectangle.
     * @return true if the entry is valid, else false.
     */
    public boolean isValid(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && getQuadratic(x, y) != INVALID;
    }

    public String toJSON() {
        StringBuilder sb = new StringBuilder(quadratic.length*2);
        boolean firstY = true;
        for (int y = 0 ; y < height ; y++) {
            if (!firstY) {
                sb.append(", ");
            }
            sb.append("{");
            boolean firstX = true;
            int trueX = 0;
            for (int x = 0; x < width; x++) {
                if (getQuadratic(x, y) == MARKER) {
                    if (!firstX) {
                        sb.append(", ");
                    }
                    sb.append(trueX);
                    firstX = false;
                }
                if (getQuadratic(x, y) != INVALID) {
                    trueX++;
                }
            }
            sb.append("}");
            firstY = false;
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(quadratic.length*2);
        for (int y = 0 ; y < height ; y++) {
            sb.append((String.format("%3d: ", y)));
            for (int x = 0; x < width; x++) {
                switch (getQuadratic(x, y)) {
                    case NEUTRAL:

//                        sb.append("O");
                        sb.append(getPriority(x, y) > 9 ? "∞" : getPriority(x, y));
                        break;
                    case MARKER:
                        sb.append("X");
                        break;
                    case ILLEGAL:
                        sb.append(".");
                        break;
                    case INVALID:
                        sb.append(" ");
                        break;
                    case VISITED:
                        sb.append(",");
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown element: " + getQuadratic(x, y));
                }
                sb.append(x < width-1 ? " " : "");
            }
            sb.append(y < height-1 ? "\n" : "");
        }
        return sb.toString();
    }

}
