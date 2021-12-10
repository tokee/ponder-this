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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    static final int VISITED = 3;  // Previously visited in this descend tree (do not mark again)

    // ILLEGAL is special: First ILLEGAL for an element is just the value.
    //                     Secondary ILLEGALS stacks by adding the ILLEGAL constant (5) each time.
    static final int ILLEGAL = 5;  // Cannot be set (will result in AP)

    final int edge; // Hexagonal edge
    final int width;
    final int height;
    final int valids;
    final int[] allValidPositions; // As indices into quadratic & priority

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
        boardChanges = new int[height * width * 2 * 10]; // Times 2 as they are coordinates, times 10 for ILLEGALS (hack)
        boardChangeIndexes = new int[height * width];

        // Draw the quadratic map
        Arrays.fill(quadratic, INVALID);
        final AtomicInteger v = new AtomicInteger(0);
        visitAllSlow(pos -> {
            quadratic[pos] = NEUTRAL;
            v.incrementAndGet();
        });
        valids = v.get();
        allValidPositions = new int[valids];
        AtomicInteger i = new AtomicInteger(0);
        visitAllSlow(pos -> {
            allValidPositions[i.getAndIncrement()] = pos;
        });
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
        this.allValidPositions = Arrays.copyOf(other.allValidPositions, other.allValidPositions.length);
//        this.tripleDeltasByRow = Arrays.copyOf(other.tripleDeltasByRow, other.tripleDeltasByRow.length);
//        this.tripleDeltasByColumn = Arrays.copyOf(other.tripleDeltasByColumn, other.tripleDeltasByColumn.length);
    }

    // Wven with bitmaps this is unrealistic: 22:29:01.663 [main] INFO dk.ekot.apmap.Mapper - CacheTriples: #elements is 2666895 with 1000519 valids. With a full bitmap for each valid that is 318083 MByte

    public void cacheTriples() {
//        log.info("CacheTriples: #elements is " + width*height + " with " + valids + " valids. With a full " +
//                 "bitmap for each valid that is " + ((long) valids * width * height / 8 / 1024 / 1024) + " MByte");
        // [pos][firstMarkers]
        int[][] triplesRadial = new int[width*height][];
        // [pos]firstMarker]
        int[][] triplesIntersecting = new int[width*height][];
        streamAllValid().forEach(origo -> {
            List<Integer> radial = new ArrayList<>();
            visitTriplesRadial(origo%width, origo/width, (pos1, pos2) -> {
                int closest = Math.abs(origo-pos1) < Math.abs(origo-pos2) ? pos1 : pos2;
                radial.add(closest);
            });
            triplesRadial[origo] = radial.stream().mapToInt(Integer::intValue).toArray();

            List<Integer> intersect = new ArrayList<>();
            visitTriplesIntersecting(origo%width, origo/width, (pos1, pos2) -> {
                intersect.add(pos1); // Does not matter if it is pos1 or pos2
            });
            triplesIntersecting[origo] = intersect.stream().mapToInt(Integer::intValue).toArray();
        });
        long elements = 0;
        for (int i = 0 ; i < width*height ; i++) {
            if (triplesRadial[i] != null) {
                elements += triplesRadial[i].length + triplesIntersecting[i].length;
            }
        }
        log.info("CacheTriples produced " + elements + " elements ~= " + (elements*4/1024/1024) + " MByte");
    }

    /**
     * Set the markers stated in the JSON. Does not perform any cleaning beforehand.
     * @param json APMap-compliant JSON.
     */
    public void addJSONMarkers(String json) {
        final int rows = (int) json.chars().filter(c -> c == '{').count();
        if (rows != height) {
            throw new IllegalArgumentException(
                    "Got " + rows + " rows from JSON, but the height of the current layout is " + height);
        }
        Matcher m = CURLY.matcher(json);
        int row = 0;
        while (m.find()) { // {0, 2, 89}, group(1) == 0, 2, 89
                int margin = Math.abs(row-(height>>1));
                for (String xs: m.group(1).split(" *, *")) {
                    if (xs.isEmpty()) {
                        continue;
                    }
                    int x = Integer.parseInt(xs);
                    setMarker(margin+x*2, row, true);
                }
            ++row;
        }
    }
    private static final Pattern CURLY = Pattern.compile("[{]([^}]*)[}]");

    /**
     * Creates a new structure based on the given JSON.
     * @param json APMap-compliant JSON.
     * @return a Mapper with markers as stated in the JSON.
     */
    public static Mapper fromJSON(String json) {
        Mapper board = new Mapper(getEdgeFromJSON(json));
        board.addJSONMarkers(json);
        return board;
    }

    /**
     * @param json APMap-compliant JSON.
     * @return the edge as inferred from the JSON.
     */
    public static int getEdgeFromJSON(String json) {
        return (int) ((json.chars().filter(c -> c == '{').count() + 1) / 2);
    }

    /**
     * Turn the given positions into lazy positions.
     * @param positions valid positions on the board.
     * @return the positions represented af lazy positions.
     */
    public List<LazyPos> makeLazy(List<Integer> positions) {
        return positions.stream().map(LazyPos::new).collect(Collectors.toList());
    }

    /**
     * Checks if the markers on the board are legally places (no complete triples).
     * This is a slow process.
     */
    public void validate() {
        visitAllXY((x, y) -> {
            if (getQuadratic(x, y) >= ILLEGAL) {
                int illegals = getQuadratic(x, y)/ILLEGAL;
                AtomicInteger count = new AtomicInteger(0);
                StringBuilder sb = new StringBuilder();
                visitTriples(x, y, (pos1, pos2) -> {
                    if (quadratic[pos1] == MARKER && quadratic[pos2] == MARKER) {
                        count.incrementAndGet();
                        sb.append(String.format(Locale.ROOT, "\n(%d, %d) (%d, %d)",
                                                pos1%width, pos1/width, pos2%width, pos2/width));
                    }
                });
                if (count.getAndIncrement() != illegals) {
                    throw new IllegalStateException(String.format(
                            Locale.ROOT, "Board illegals for (%d, %d) was %d, but should be %d. Relevant triples are%s",
                            x, y, illegals, count.get(), sb));
                }
            } else if (getQuadratic(x, y) == MARKER) {
                visitTriples(x, y, (pos1, pos2) -> {
                    if (quadratic[pos1] == MARKER && quadratic[pos2] == MARKER) {
                        throw new IllegalStateException(String.format(
                                Locale.ROOT, "Triple detected at (%d, %d), (%d, %d), (%d, %d)",
                                x, y, pos1%width, pos1/width, pos2%width, pos2/width));
                    };
                    if (quadratic[pos1] == MARKER) {
                        if (quadratic[pos2] < ILLEGAL) {
                            throw new IllegalStateException(String.format(
                                    Locale.ROOT, "Triple detected with two markers (%d, %d), (%d, %d) but (%d, %d) was %d where it should be ILLEGAL",
                                    x, y, pos1%width, pos1/width, pos2%width, pos2/width, quadratic[pos2]));
                        }
                    }
                    if (quadratic[pos2] == MARKER) {
                        if (quadratic[pos1] < ILLEGAL) {
                            throw new IllegalStateException(String.format(
                                    Locale.ROOT, "Triple detected with two markers (%d, %d), (%d, %d) but (%d, %d) was %d where it should be ILLEGAL",
                                    x, y, pos2%width, pos2/width, pos1%width, pos1/width, quadratic[pos1]));
                        }
                    }
                });
            }
        });
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
     * Extremely fast positions ordering by only comparing positions. Sort order:
     * 1) Position (lower is better)
     * @return a comparator with the described characteristica.
     */
    public static Comparator<LazyPos> getPositionComparator() {
        return Comparator.comparing(LazyPos::getPos);
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
     * Slow priorityChanges ordering by only comparing positions and priority. Sort order:
     * 1) Priority (lower is better)
     * 2) Priority changes (lower is better)
     * 2) Position (lower is better)
     * @return a comparator with the described characteristica.
     */
    public static Comparator<LazyPos> getPriorityChangesComparator() {
        return Comparator.
                comparing(LazyPos::getPosPriority).
                thenComparing(LazyPos::getPriorityChanges).
                thenComparing(LazyPos::getPos);
    }
    /**
     * Get all positions that are neutral, sorted by the given comparator.
     * @param comparator determines the order of the result.
     * @return all neutral (aka free) positions, sorted by comparator.
     */
    public List<LazyPos> getLazyPositions(Comparator<LazyPos> comparator) {
        List<LazyPos> positions = new ArrayList<>(valids);
        visitAllValid(pos -> {
            if (quadratic[pos] == NEUTRAL) {
                positions.add(new LazyPos(pos));
            }
        });
        positions.sort(comparator);
        return positions;
    }

    /**
     * Fill the given positions with all neutrals, in order of priority then left-right, sop-down.
     *
     * This is not a thread safe operation!
     */
    public void fillPositionsWithNeutralsByPriority(Positions positions) {
        if (longCache.length != positions.getMaxCapacity()) {
            longCache = new long[positions.getMaxCapacity()];
        }

        // Collect all neutrals as long by concatenating priority with position.
        AtomicInteger index = new AtomicInteger(0);
        visitAllValid(pos -> {
            if (quadratic[pos] == NEUTRAL) {
                longCache[index.getAndIncrement()] = (long) priority[pos] << 32 | (long) pos;
            }
        });

        // Sort by natural order and extract positions
        Arrays.sort(longCache, 0, index.get());
        for (int i = 0 ; i < index.get() ; i++) {
            positions.add((int) longCache[i]);
        }
    }
    long[] longCache = new long[0];

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

        public int getPos() {
            return pos;
        }

        public int getPosPriority() {
            return posPriority;
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
     * Simple x-y-based representation in the quadratic system.
     */
    public class XYPos {
        public final int x;
        public final int y;

        public XYPos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public XYPos(int pos) {
            this.x = pos%width;
            this.y = pos/width;
        }

        /**
         * @return the position as index instead of {@code (x, y)}.
         */
        public int getPos() {
            return y*width+x;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XYPos xyPos = (XYPos) o;
            return x == xyPos.x && y == xyPos.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
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
    public final PriorityPosXY nextPriority(PriorityPosXY origo) {
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
                    return new PriorityPosXY(qx, qy, origo.priority);
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
                                return new PriorityPosXY(bestX, bestY, bestP);
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
        return new PriorityPosXY(bestX, bestY, bestP);
    }

    final static class PriorityPosXY {
        public final int x;
        public final int y;
        public final int priority;

        public PriorityPosXY() {
            this(-1, 0, 0);
        }

        public PriorityPosXY(int x, int y, int priority) {
            this.x = x;
            this.y = y;
            this.priority = priority;
        }

        public PriorityPosXY copy() {
            return new PriorityPosXY(x, y, priority);
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + priority + ")";
        }
    }

    private class PriorityPos implements Comparable<PriorityPos> {
        public final int pos;
        public final int priority;

        public PriorityPos(int pos) {
            this.pos = pos;
            this.priority = Mapper.this.priority[pos];
        }

        public PriorityPos(int pos, int priority) {
            this.pos = pos;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityPos o) {
            return priority == o.priority ? Integer.compare(pos, o.pos) : Integer.compare(priority, o.priority);
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
     * Getter that allows requests outside of the board. In that case {@link #INVALID} is returned.
     * Does not update {@link #neutrals} and {@link #marked}.
     * @param pos quadratic coordinates.
     * @return the element at the given coordinates or {@link #INVALID} if outside the board.
     */
    private int getQuadratic(XYPos pos) {
        return getQuadratic(pos.x, pos.y);
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
     * The setter does not check if the coordinates are legal and does not update {@link #neutrals} and {@link #marked}.
     * @param pos quadratic coordinates.
     * @param element the element to set at the coordinates.
     */
    public final void setQuadratic(XYPos pos, int element) {
        quadratic[pos.y*width+pos.x] = element;
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

    public final void markAndDeltaExpand(PriorityPosXY pos, boolean updatePriorities) {
        markAndDeltaExpand(pos.x, pos.y, updatePriorities); // TODO: Make this fast. Maye with x & y as first class?
    }
    public final void markAndDeltaExpand(LazyPos pos, boolean updatePriorities) {
        markAndDeltaExpand(pos.getX(), pos.getY(), updatePriorities); // TODO: Make this fast. Maye with x & y as first class?
    }

    /**
     * Set the given pos to VISITED and add the position to the current rollback-list.
     * Performing a rollback after this will rollback whatever was on the rollback list plus the newly added pos.
     * @param pos an index into {@link #quadratic} which will be set to visited.
     */
    public void addVisited(final int pos) {
        quadratic[pos] = VISITED;
        --neutrals;
        boardChanges[boardChangeIndexes[changeIndexPosition]++] = pos;
    }

    /**
     * Adds the 5 60° rotations of (x, y) to positions, starting at 0.
     * @param x in quadratic coordinates.
     * @param y in quadratic coordinates.
     * @param positions offsets in {@link #quadratic}.
     */
    public void fillRotated(int x, int y, int[] positions) {
        fillRotated(x, y, positions, 0);
    }
    /**
     * Adds the 5 60° rotations of (x, y) to positions, starting at start.
     * @param x in quadratic coordinates.
     * @param y in quadratic coordinates.
     * @param positions offsets in {@link #quadratic}.
     * @param start where to begin storing the 5 rotations of (x, y).
     */
    // TODO: Introduce visitRotated instead
    public void fillRotated(int x, int y, int[] positions, int start) {
        // Translate quadratical coordinates so that they are relative to center
        int centerX = width/2;
        int centerY = height/2;

        int relX = x-centerX;
        int relY = y-centerY;

        // Adjust horizontal coordinates to be without gaps
        relX = relX>>1; // TODO: Should probably do some trickery every other line here

//        System.out.printf("relX=%d, relY=%d\n", relX, relY);

        // Translate to hex coordinates
        int xx = relX - (relY - (relY&1)) / 2;
        int zz = relY;
        int yy = -xx - zz;
//        System.out.printf("xx=%d, yy=%d, zz=%d\n", xx, yy, zz);

        for (int i = 0 ; i < 5 ; i++) {
            // Rotate 60°

            int xxO = xx;
            int yyO = yy;
            int zzO = zz;

            xx = -zzO;
            yy = -xxO;
            zz = -yyO;

            // Translate back to center-relative quadratic coordinates
            relX = xx + (zz - (zz&1)) / 2;
            relY = zz;

            // Expand horizontal coordinates to the space oriented format
            relX = (relX<<1) + (relY&1); // relY&1 to compensate for eneven offset

            // Translate center-relative to plain rectangular coordinates.
            x = relX+centerX;
            y = relY+centerY;

            //System.out.printf("x=%d, y=%d\n", x, y);

            positions[start+i] = y*width+height;
        }
    }

    // TODO: Reduce this to call fillRotated
    public void addVisitedRotated(int x, int y) {
        // https://gamedev.stackexchange.com/questions/15237/how-do-i-rotate-a-structure-of-hexagonal-tiles-on-a-hexagonal-grid
        // https://www.redblobgames.com/grids/hexagons/#rotation
//        System.out.printf("x=%d, y=%d\n", x, y);

        setQuadratic(x, y, VISITED);

        // Translate quadratical coordinates so that they are relative to center
        int centerX = width/2;
        int centerY = height/2;

        int relX = x-centerX;
        int relY = y-centerY;

        // Adjust horizontal coordinates to be without gaps
        relX = relX>>1; // TODO: Should probably do some trickery every other line here

//        System.out.printf("relX=%d, relY=%d\n", relX, relY);

        // Translate to hex coordinates
        int xx = relX - (relY - (relY&1)) / 2;
        int zz = relY;
        int yy = -xx - zz;
//        System.out.printf("xx=%d, yy=%d, zz=%d\n", xx, yy, zz);

        for (int i = 0 ; i < 5 ; i++) {
            // Rotate 60°

            int xxO = xx;
            int yyO = yy;
            int zzO = zz;

            xx = -zzO;
            yy = -xxO;
            zz = -yyO;

            // Translate back to center-relative quadratic coordinates
            relX = xx + (zz - (zz&1)) / 2;
            relY = zz;

            // Expand horizontal coordinates to the space oriented format
            relX = (relX<<1) + (relY&1); // relY&1 to compensate for eneven offset
            
            // Translate center-relative to plain rectangular coordinates.
            x = relX+centerX;
            y = relY+centerY;

            //System.out.printf("x=%d, y=%d\n", x, y);

            setQuadratic(x, y, VISITED);
        }
    }

    public void markAndDeltaExpand(final int pos, boolean updatePriorities) {
        markAndDeltaExpand(pos%width, pos/width, updatePriorities); // TODO: Avoid this as it is very slow
    }

    /**
     * Marks the given quadratic (x, y) and adds the coordinated to changed at changedIndex.
     * Uses the {@link #tripleDeltas} to resolve all fields that are neutral and where setting a mark would cause
     * a triple (arithmetic progression). The fields are set to {@link #ILLEGAL} and their coordinate pairs are added
     * to changed, with changedIndex being incremented accordingly.
     * This updates {@link #marked} and {@link #neutrals}.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @param updatePriorities
     * @return the new changedIndex. Will always be at least 2 more than previously.
     */
    public void markAndDeltaExpand(final int x, final int y, boolean updatePriorities) {
        ++changeIndexPosition;
        boardChangeIndexes[changeIndexPosition] = boardChangeIndexes[changeIndexPosition - 1];
        final int origoPos = y*width+x;
        // TODO: Simplify so that boardChanges is only indexes, not a mix of (x, y) and indexes
        boardChanges[boardChangeIndexes[changeIndexPosition]++] = x;
        boardChanges[boardChangeIndexes[changeIndexPosition]++] = y;
        if (quadratic[origoPos] != NEUTRAL) {
            throw new IllegalStateException(
                    "Attempted to mark (" + x + ", " + y + ") bit but it already had state " + quadratic[origoPos]);
        }

        quadratic[origoPos] = MARKER;
        ++marked;
        --neutrals;
        visitTriples(x, y, (pos1, pos2) -> {
            final int pos1Element = quadratic[pos1];
            final int pos2Element = quadratic[pos2];

            if (pos1Element == MARKER) { // pos2element is either NEUTRAL (0) or ILLEGAL (5+) // && pos2Element == NEUTRAL) {
                quadratic[pos2] += ILLEGAL;
                try {
                    boardChanges[boardChangeIndexes[changeIndexPosition]++] = pos2;
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalStateException(String.format(
                            Locale.ROOT, "IOBException on markAndDeltaExpand(x=%d, y=%d, updatePriorities=%b) while " +
                                         "logging board changes for position (%d, %d)." +
                                         "changeIndexPosition=%d, boardChangeIndexes.length=%d, boardChanges.length=%d," +
                                         " width=%d, height=%d, valids=%d",
                            x, y, updatePriorities, pos2%width, pos2/width,
                            changeIndexPosition, boardChangeIndexes.length, boardChanges.length,
                            width, height, valids), e);
                }
                if (pos2Element == NEUTRAL) {
                    --neutrals;
                }
            } else if (pos2Element == MARKER) { // && pos1Element == NEUTRAL) {
                quadratic[pos1] += ILLEGAL;
                boardChanges[boardChangeIndexes[changeIndexPosition]++] = pos1;
                if (pos1Element == NEUTRAL) {
                    --neutrals;
                }
            }
        });
        // TODO: Make this part of the visitTriples above
        if (updatePriorities) {
            adjustPriorities(x, y, 1);
        }

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
     * Adjust all priorities so that the center position is worst and the edges are best.
     */
    public void adjustPrioritiesCenterBad() {
        final int centerX = width/2;
        final int centerY = height/2;
        visitAllXY((x, y) -> {
            int distToCenter = (int) Math.sqrt(Math.pow(Math.abs(centerX - x), 2)/2 +
                                               Math.pow(Math.abs(centerY-y), 2));
            //System.out.printf("pos(%d, %d), center(%d, %d), dist=%d\n", x, y, centerX, centerY, distToCenter);
            adjustPriority(x, y, (edge-distToCenter)*2);
        });
    }

    /**
     * Adjust all priorities so that the center position is best and the edges are worst.
     */
    public void adjustPrioritiesCenterGood() {
        final int centerX = width/2;
        final int centerY = height/2;
        visitAllXY((x, y) -> {
            int distToCenter = (int) Math.sqrt(Math.pow(Math.abs(centerX - x), 2)/2 +
                                               Math.pow(Math.abs(centerY-y), 2));
            //System.out.printf("pos(%d, %d), center(%d, %d), dist=%d\n", x, y, centerX, centerY, distToCenter);
            adjustPriority(x, y, distToCenter*2);
        });
    }

    /**
     * Adjust all priorities so that the priority of any cell is the number of its triples.
     */
    public void adjustPrioritiesByTripleCount() {
        log.debug("Adjusting priorities by triple count" + (edge < 100 ? "" : ". This might take some seconds"));
        visitAllXY((x, y) -> {
            AtomicInteger counter = new AtomicInteger(0);
            visitTriples(x, y, (pos, pos2) -> {
                counter.addAndGet(2);
            });
            //System.out.printf("pos(%d, %d), center(%d, %d), dist=%d\n", x, y, centerX, centerY, distToCenter);
            adjustPriority(x, y, counter.get());
        });
    }

    /**
     * Mark all quadratic coordinate pairs in {@code changed[from..to]} (to is exclusive) as {@link #NEUTRAL}.
     * This performs {@code --marked} and {@code neutrals += (to-from)/2}.
     */
    public void rollback(boolean updatePriorities) {
        //System.out.println(this);
        int start = boardChangeIndexes[changeIndexPosition - 1];

        final int x =  boardChanges[start++];
        final int y =  boardChanges[start++];
//        System.out.printf("popping(%d, %d) from %d\n", x, y, boardChangeIndexes[changeIndexPosition-1]);
        if (updatePriorities) {
            adjustPriorities(x, y, -1); // Must be before the clearing of the marker below
        }
        setQuadratic(x, y, NEUTRAL);
        --marked;
        ++neutrals;

        for (int i = start; i < boardChangeIndexes[changeIndexPosition] ; i++) {
            if (quadratic[boardChanges[i]] == MARKER) {
                quadratic[boardChanges[i]] = NEUTRAL;
                ++neutrals;
            } else if (quadratic[boardChanges[i]] >= ILLEGAL) {
                quadratic[boardChanges[i]] -= ILLEGAL;
            } else if (quadratic[boardChanges[i]] == VISITED) {
                quadratic[boardChanges[i]] = NEUTRAL;
                ++neutrals;
            }

        }
        //neutrals += (boardChangeIndexes[changeIndexPosition] - boardChangeIndexes[changeIndexPosition - 1])-1;
        --changeIndexPosition;
        //System.out.printf("\n%s\n---- rollbacked(%d, %d)\n", this, x, y);
    }

    /**
     * Marks the given quadratic (x, y).
     * Uses the {@link #tripleDeltas} to resolve all fields that are neutral and where setting a mark would cause
     * a triple (arithmetic progression). The fields are updated with {}@link #ILLEGAL}.
     * This updates {@link #marked} and {@link #neutrals}.
     * Note: Slow due to division and modulo.
     * @param pos quadratic coordinates.
     * @param updatePriorities
     */
    public void setMarker(int pos, boolean updatePriorities) {
        setMarker(pos%width, pos/width, updatePriorities);
    }
    /**
     * Marks the given quadratic (x, y).
     * Uses the {@link #tripleDeltas} to resolve all fields that are neutral and where setting a mark would cause
     * a triple (arithmetic progression). The fields are updated with {}@link #ILLEGAL}.
     * This updates {@link #marked} and {@link #neutrals}.
     * @param pos quadratic coordinates.
     * @param updatePriorities
     */
    public void setMarker(final XYPos pos, boolean updatePriorities) {
        setMarker(pos.x, pos.y, updatePriorities);
    }
    /**
     * Marks the given quadratic (x, y).
     * Uses the {@link #tripleDeltas} to resolve all fields that are neutral and where setting a mark would cause
     * a triple (arithmetic progression). The fields are updated with {}@link #ILLEGAL}.
     * This updates {@link #marked} and {@link #neutrals}.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @param updatePriorities
     */
    public void setMarker(final int x, final int y, boolean updatePriorities) {
        final int origoPos = y*width+x;
        if (quadratic[origoPos] != NEUTRAL) {
            throw new IllegalStateException(
                    "Attempted to mark (" + x + ", " + y + ") bit but it already had state " + quadratic[origoPos]);
        }

        quadratic[origoPos] = MARKER;
        ++marked;
        --neutrals;

        visitTriples(x, y, (pos1, pos2) -> {
            final int pos1Element = quadratic[pos1];
            final int pos2Element = quadratic[pos2];

            if (pos1Element == MARKER) { // pos2element is either NEUTRAL (0) or ILLEGAL (5+) // && pos2Element == NEUTRAL) {
                quadratic[pos2] += ILLEGAL;
                if (pos2Element == NEUTRAL) {
                    --neutrals;
                }
            } else if (pos2Element == MARKER) { // && pos1Element == NEUTRAL) {
                quadratic[pos1] += ILLEGAL;
                if (pos1Element == NEUTRAL) {
                    --neutrals;
                }
            }
        });
        // TODO: Make this part of the visitTriples above
        if (updatePriorities) {
            adjustPriorities(x, y, 1);
        }
    }

    /**
     * Removes a MARKER from the given position and updates all relevant ILLEGAL elements by checking triples.
     * Warning: Slow as it uses division and modulo.
     * @param pos quadratic coordinates.
     * @param updatePriorities if true, priorities are also adjusted.
     */
    public void removeMarker(int pos, boolean updatePriorities) {
        removeMarker(pos%width, pos/width, updatePriorities);
    }
    /**
     * Removes a MARKER from the given position and updates all relevant ILLEGAL elements by checking triples.
     * @param pos quadratic coordinates.
     * @param updatePriorities if true, priorities are also adjusted.
     */
    public void removeMarker(XYPos pos, boolean updatePriorities) {
        removeMarker(pos.x, pos.y, updatePriorities);
    }
    /**
     * Removes a MARKER from the given position and updates all relevant ILLEGAL elements by checking triples.
     * @param x quadratic X.
     * @param y quadratic Y.
     * @param updatePriorities if true, priorities are also adjusted.
     */
    public void removeMarker(int x, int y, boolean updatePriorities) {
        if (getQuadratic(x, y) != MARKER) {
            throw new IllegalStateException("Tried removing MARKER from (" + x + ", " + y + ") but it was " +
                                            getQuadratic(x, y) + " instead of the expected " + MARKER);
        }
        if (updatePriorities) {
            adjustPriorities(x, y, -1); // Must be before the clearing of the marker below
        }
        setQuadratic(x, y, NEUTRAL);
        --marked;
        ++neutrals;

        visitTriples(x, y, (pos1, pos2) -> {
            if (quadratic[pos1] >= ILLEGAL && quadratic[pos2] == MARKER) {
                quadratic[pos1] -= ILLEGAL;
                if (quadratic[pos1] == NEUTRAL) {
                    ++neutrals;
                }
            }
            if (quadratic[pos2] >= ILLEGAL && quadratic[pos1] == MARKER) {
                quadratic[pos2] -= ILLEGAL;
                if (quadratic[pos2] == NEUTRAL) {
                    ++neutrals;
                }
            }
        });
    }

    /**
     * Shuffling by finding the markers that locks the lowest amount of positions, then removing those until minFree
     * positions has been freed, not counting the removed markers themselves.
     * After that new markers are set, prioritizing the positions freed by removing markers.
     *
     * Observation: Performs worse than shuffle2
     * @param minFree the minimum amount of markers to remove.
     * @return number of marks gained (can be negative).
     */
    public int shuffle4(int minFree) {
        final int initialNeutrals = getNeutralCount(); // Should be 0
        if (initialNeutrals != 0) {
            System.out.println("shuffle4: Warning: Expected 0 neutrals to start with but found " + initialNeutrals);
        }
        final int initialMarkedCount = getMarkedCount();

        // Prioritize markers with the least locking first
        List<Integer> markers = streamAllValid()
                .filter(pos -> quadratic[pos] == MARKER)
                .boxed()
                .map(PriorityPos::new)
                .sorted()
                .map(pp -> pp.pos)
                .collect(Collectors.toList());


        // Remove markers
        List<Integer> explicitFreed = new ArrayList<>();
        for (Integer marker: markers) {
            removeMarker(marker, true);
            explicitFreed.add(marker);
            if (getNeutralCount() > explicitFreed.size() + minFree) {
                break;
            }
        }

        // Locate implicitly freed markers
        List<Integer> implicitFreed = streamAllValid()
                .filter(pos -> quadratic[pos] == NEUTRAL)
                .boxed()
                .filter(pos -> !explicitFreed.contains(pos))
                .map(PriorityPos::new)
                .sorted()
                .map(pp -> pp.pos)
                .collect(Collectors.toList());

        // Mark again, starting with implicit
        Stream.concat(implicitFreed.stream(), explicitFreed.stream())
                .filter(pos -> quadratic[pos] == NEUTRAL)
                .forEach(pos -> setMarker(pos, true));

        return getMarkedCount() - initialMarkedCount;
    }

    /**
     * Shuffling by finding maxCandidates ILLEGALS randomly, removing the MARKERs locking the ILLEGALs and filling
     * up again, starting with the freed ILLEGALs.
     * This piggybacks on shuffle2.
     * @param seed used for the Random.
     * @param maxCandidates the number of ILLEGAL candidates to extract randomly.
     * @param maxTrials the number of trials to run before selecting the best candidate.
     * @param maxPermutations used for calls to {@link #getBestPermutation(List, List, Random, int)}.
     * @param minGained at least this amount of marks must be gained in order to apply the best result.
     * @return number of marks gained (can be negative).
     */
    // edge 27: 216 -> 229
    public int shuffle5(int seed, int maxCandidates, int maxTrials, int maxPermutations, int minGained) {
        final Random random = new Random(seed);

        //System.out.printf("edge=%d, shuffle2 seed=%d, maxPermutations=%d\n", edge, seed, maxPermutations);
        List<XYPos> locked = streamAllValid()
                .filter(pos -> quadratic[pos] >= ILLEGAL)
                .boxed()
                .map(XYPos::new)
                .collect(Collectors.toList());
        maxCandidates = Math.min(maxCandidates, locked.size());


        List<XYPos> bestLocked = null;
        List<XYPair> bestMarkerPairs = null;
        Pair<Integer, boolean[]> bestPermutation = new Pair<>(Integer.MIN_VALUE, null);

        for (int i = 0 ; i < maxTrials ; i++) {
            Set<XYPos> selectedLocked = new HashSet<>();
            while (selectedLocked.size() < maxCandidates) {
                selectedLocked.add(locked.get(random.nextInt(locked.size())));
            }
            List<XYPos> selectedLockedList = new ArrayList<>(selectedLocked);
            List<XYPair> removePairs = getMarkedTriples(selectedLockedList);
            Pair<Integer, boolean[]> result = getBestPermutation(selectedLockedList, removePairs, random, maxPermutations);
            if (result.first > bestPermutation.first) {
                bestLocked = selectedLockedList;
                bestMarkerPairs = removePairs;
                bestPermutation = result;
            }

        }

        // Apply the best permutation if it is good enough
        return bestPermutation.first < minGained ? 0 : applyPermutation(bestLocked, bestMarkerPairs, bestPermutation.second);
    }


    /**
     * Shuffling by finding the ILLEGALs with the lowest count, removing the MARKERs locking the ILLEGALs and filling
     * up again, starting with the freed ILLEGALs.
     * Addition: Permutate the MARKERs to move, selecting the best combination.
     * @return number of marks gained (can be negative).
     */
    // edge 27: 216 -> 229
    public int shuffle2(int seed, int maxPermutations) {
        //System.out.printf("edge=%d, shuffle2 seed=%d, maxPermutations=%d\n", edge, seed, maxPermutations);

        final Random random = new Random(seed);
        // Free some elements
        List<XYPos> lightestLocked = findLightestLocked();
        List<XYPair> removePairs = getMarkedTriples(lightestLocked);

        Pair<Integer, boolean[]> best = getBestPermutation(lightestLocked, removePairs, random, maxPermutations);

        // Apply the best permutation, even if negative TODO: Switch to other strategy on negative?
        return applyPermutation(lightestLocked, removePairs, best.second);
    }

    /**
     * Find the best markers to remove, one from each removePairs.
     * @param locked the locked elements that are to be freed.
     * @param markerPairs the markers that locks the elements.
     * @param random the random used for permutations.
     * @param maxPermutations the maximum number of permutations to try before returning the result.
     * @return the best delta found together with the permutation used.
     *         The permutation can be used with {@link #applyPermutation}.
     */
    private Pair<Integer, boolean[]> getBestPermutation(List<XYPos> locked, List<XYPair> markerPairs, Random random, int maxPermutations) {
        boolean[] useFirst = new boolean[markerPairs.size()]; // Set bit = use pos1 , else use pos2
        boolean[] bestPermutation = new boolean[markerPairs.size()];
        int bestDelta = Integer.MIN_VALUE;
        int permutations = (int) Math.min(maxPermutations, Math.pow(2, markerPairs.size()));
        for (int permutation = 0 ; permutation < permutations ; permutation++) {
            // Create a new removal list based on permutations
            List<XYPos> removeMarkers = new ArrayList<>(markerPairs.size());
            for (int i = 0; i < markerPairs.size() ; i++) {
                removeMarkers.add(useFirst[i] ? markerPairs.get(i).getPos1() : markerPairs.get(i).getPos2());
            }

            // Measure the effectiveness of the permutation
            int markDelta = applyShuffle(locked, removeMarkers, true);
            if (markDelta > bestDelta) {
                bestDelta = markDelta;
                System.arraycopy(useFirst, 0, bestPermutation, 0, useFirst.length);
            }

            // Update the permutation bitmap
            for (int i = 0 ; i < useFirst.length ; i++) {
                useFirst[i] = random.nextBoolean();
            }
/*            for (int i = 0 ; i < useFirst.length ; i++) {
                if (useFirst[i]) {
                    useFirst[i] = false;
                } else {
                    useFirst[i] = true;
                    break;
                }
            }*/
        }
        return new Pair<>(bestDelta, bestPermutation);
    }

    /**
     * Apply a permutation normally generated from {@link #getBestPermutation(List, List, Random, int)}.
     * @param locked the locked elements that are to be freed.
     * @param markerPairs the markers that locks the elements.
     * @param permutation the markers to remove from markerPairs.
     * @return the
     */
    private int applyPermutation(List<XYPos> locked, List<XYPair> markerPairs, boolean[] permutation) {
        List<XYPos> removeMarkers = new ArrayList<>(markerPairs.size());
        for (int i = 0; i < markerPairs.size() ; i++) {
            removeMarkers.add(permutation[i] ? markerPairs.get(i).getPos1() : markerPairs.get(i).getPos2());
        }
        return applyShuffle(locked, removeMarkers, false);
    }


    /**
     * Shuffling by finding the ILLEGALs with the lowest count, removing the MARKERs locking the ILLEGALs and filling
     * up again, starting with the freed ILLEGALs.
     * Special feature: Used priorities to select the mark to remove: The mark that has the lowest priority in the
     * triple wins.
     * @return number of marks gained (can be negative).
     */
    // Seems to do markedly worse than shuffle2
    public int shuffle3(int seed) {
        //System.out.printf("edge=%d, shuffle2 seed=%d, maxPermutations=%d\n", edge, seed, maxPermutations);

        final Random random = new Random(seed);
        // Free some elements
        List<XYPos> lightestLocked = findLightestLocked(); // TODO: Check for repetitions here?
        List<XYPair> removePairs = getMarkedTriples(lightestLocked);
        List<XYPos> removeMarkers = removePairs.stream()
                //.peek(pair -> System.out.println("Priorities(" + pair.pos1 + ", " + pair.pos2 + "): " + priority[pair.pos1.getPos()] + " " + priority[pair.pos2.getPos()]))
                .map(pair -> priority[pair.pos1.getPos()] < priority[pair.pos2.getPos()] ? pair.pos1 : pair.pos2)
                .collect(Collectors.toList());
        return applyShuffle(lightestLocked, removeMarkers, false);
    }

    private int applyShuffle(List<XYPos> locked, List<XYPos> removedMarkers, boolean rollback) {
        final int initialMarks = getMarkedCount();
        // Remove selected markers
        removedMarkers.stream()
                .filter(pos -> getQuadratic(pos) == MARKER)
                .forEach(pos -> removeMarker(pos, true));

        // Find all indirectly freed elements
        List<XYPos> indirectFreed = streamAllValid()
                .boxed()
                .map(XYPos::new)
                .filter(pos -> !(locked.contains(pos) || removedMarkers.contains(pos)))
                .collect(Collectors.toList());

        // Set markers prioritized by indirect, lightest and removed
        List<XYPos> reMarked =
                Stream.concat(indirectFreed.stream(), Stream.concat(locked.stream(), removedMarkers.stream()))
                .filter(pos -> getQuadratic(pos) == NEUTRAL)
                .peek(pos -> setMarker(pos, true))
                .collect(Collectors.toList());

        int freed = getMarkedCount() - initialMarks;
        if (!rollback) {
            return freed;
        }

        reMarked.stream()
                .filter(pos -> getQuadratic(pos) == MARKER)
                .forEach(pos -> removeMarker(pos, true));
        removedMarkers.stream()
                .filter(pos -> getQuadratic(pos) == NEUTRAL)
                .forEach(pos -> setMarker(pos, true));
        return freed;
    }


    private List<XYPair> getMarkedTriples(List<XYPos> lightestLocked) {
        return lightestLocked.stream().
                map(this::findDoubleMarkedTuples).
                flatMap(Collection::stream).
                collect(Collectors.toList());
    }

    private List<XYPair> findDoubleMarkedTuples(XYPos pos) {
        List<XYPair> locking = new ArrayList<>();
        visitTriples(pos.x, pos.y, ((pos1, pos2) -> {
            if (quadratic[pos1] == MARKER && quadratic[pos2] == MARKER) {
                locking.add(new XYPair(new XYPos(pos1), new XYPos(pos2)));
            }
        }));
        return locking;
    }

    /**
     * Shuffling by finding the ILLEGALs with the lowest count, removing the MARKERs locking the ILLEGALs and filling
     * up again, starting with the freed ILLEGALs.
     * @return number of marks gained (can be negative).
     */
    // edge 27: 216 -> 229
    public int shuffle1() {
        final int initialMarks = getMarkedCount();
        // Free some elements
        List<XYPos> explicitFreed = new ArrayList<>();
        List<XYPos> previouslyMarked = new ArrayList<>();
        String freeDebug = freeSpots(explicitFreed, previouslyMarked);
        List<XYPos> indirectFreed = new ArrayList<>();
        visitAllValid(pos -> {
            if (quadratic[pos] == NEUTRAL) {
                boolean add = true;
                for (XYPos explicit: explicitFreed) {
                    if (pos == explicit.getPos()) {
                        add = false;
                        break;
                    }
                }
                for (XYPos previous: previouslyMarked) {
                    if (pos == previous.getPos()) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    indirectFreed.add(new XYPos(pos));
                }
            }
        });

        // Set markers on previously invalid positions , if possible
        AtomicInteger reMarked = new AtomicInteger(0);
        Stream.concat(indirectFreed.stream(), Stream.concat(explicitFreed.stream(), previouslyMarked.stream())).
                forEach(xyPos -> {
                    final int pos = xyPos.getPos();
                    if (quadratic[pos] == NEUTRAL) {
                        setMarker(xyPos.x, xyPos.y, true);
                        reMarked.incrementAndGet();
                    }
                });
        if (getMarkedCount() != initialMarks) {
            System.out.printf(Locale.ROOT, "Remarking with explicit=%d, previouslyMarked=%d, indirect=%d candidates marked %d elements\n%s\n",
                              explicitFreed.size(), previouslyMarked.size(), indirectFreed.size(), reMarked.get(), freeDebug);
        }
        return getMarkedCount()-initialMarks;
    }

    private String freeSpots(List<XYPos> freedSpots, List<XYPos> previouslyMarked) {
        List<XYPos> lightestLocked = findLightestLocked();
        lightestLocked.forEach(locked -> {
            freedSpots.add(locked);
            List<XYPos> lockers = findLockingMarks(locked);
            lockers.forEach(locker -> {
                if (getQuadratic(locker.x, locker.y) == MARKER) { // Might have been removed before
                    previouslyMarked.add(locker);
                    removeMarker(locker.x, locker.y, true);
                }
            });
        });

        return "";//String.format(Locale.ROOT, "lightest=%s\nfreed=%s\nremoved=%s\n" +
                    //                      lightestLocked, freedSpots.toString(), previouslyMarked.toString());
    }

    private List<XYPos> findLockingMarks(XYPos xyPos) {
        List<Integer> locking = new ArrayList<>();
        visitTriples(xyPos.x, xyPos.y, ((pos1, pos2) -> {
            if (quadratic[pos1] == MARKER && quadratic[pos2] == MARKER) {
                // TODO: Maybe randomize here? Or select by how far each of the pos' reaches?
                locking.add(pos1);
            }
        }));
        return toXY(locking);
    }

    private List<XYPos> findLightestLocked() {
        List<Integer> lightest = new ArrayList<>();
        AtomicInteger bestIllegality = new AtomicInteger(Integer.MAX_VALUE);
        visitAllValid(pos -> {
            if (quadratic[pos] >= ILLEGAL) {
                if (quadratic[pos] < bestIllegality.get()) {
                    bestIllegality.set(quadratic[pos]);
                    lightest.clear();
                }
                if (quadratic[pos] == bestIllegality.get()) {
                    lightest.add(pos);
                }
            }
        });
        return toXY(lightest);
    }

    private List<XYPos> toXY(List<Integer> positions) {
        return positions.stream().map(pos -> new XYPos(pos%width, pos/width)).collect(Collectors.toList());
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
    public void visitAllValid(Consumer<Integer> callback) {
        Arrays.stream(allValidPositions)
                .forEach(callback::accept);
    }

    /**
     * Visit all valid positions on the hexagon represented as quadratic.
     * Important: This is a ine-time call used during constructions. Use {@link #visitAllValid} subsequently.
     * @param callback delivers the indices in {@link #quadratic} for each valid element, left->right, top->down
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void visitAllSlow(Consumer<Integer> callback) {
        int yMul = 0;
        for (int y = 0 ; y < height ; y++) {
            int margin = Math.abs(y-(height>>1));
            for (int x = margin ; x < width-margin ; x+=2) {
                callback.accept(yMul+x);
            }
            yMul += width;
        }
    }

    public IntStream streamAllValid() {
        return Arrays.stream(allValidPositions);
    }

    /**
     * Locates the positions for the entries in the top row that are {@code <= edge/2}, rounded up.
     * @return the topleft positions.
     */
    public List<Integer> getTopLeftPositions() {
        List<Integer> topleft = new ArrayList<>((edge+1)/2);
        int margin = Math.abs(height>>1);
        for (int x = margin ; x < width-margin ; x+=2) {
            topleft.add(x);
            if (topleft.size() == (edge+1)/2) {
                break;
            }
        }
        return topleft;
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

    public String getStatus() {
        return String.format(Locale.ROOT,
                             "edge=%d, marks=%d/%d, walkTime=%ds, completed=%b: %s",
                             edge, getMarkedCount(), valids, getWalkTimeMS()/1000, isCompleted(), toJSON());
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
                int element = getQuadratic(x, y);
                switch (element) {
                    case NEUTRAL:

//                        sb.append("O");
                        sb.append(getPriority(x, y) > 9 ? "∞" : getPriority(x, y));
                        break;
                    case MARKER:
                        sb.append("X");
                        break;
                    case INVALID:
                        sb.append(" ");
                        break;
                    case VISITED:
                        sb.append(",");
                        break;
                    default:
                        sb.append(".");/*
                        if (element < ILLEGAL) {
                            throw new UnsupportedOperationException("Unknown element: " + getQuadratic(x, y));
                        }
                        int illegalValue = element/ILLEGAL;
                        sb.append(illegalValue > 20 ? "I" : (char) ('a' -1 + illegalValue));*/
                        break;
                }
                sb.append(x < width-1 ? " " : "");
            }
            sb.append(y < height-1 ? "\n" : "");
        }
        return sb.toString();
    }

    public void saveToImage() throws IOException {
        System.out.println("Generating and saving image for edge=" + edge + ", marks=" + marked);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int markCount = 0;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0; x < width; x++) {
                int element = getQuadratic(x, y);
                switch (element) {
                    case NEUTRAL:
                        bi.setRGB(x, y, 0xFFFFFF);
                        break;
                    case MARKER:
                        bi.setRGB(x, y, 0x0000FF);
                        markCount++;
                        break;
                    case INVALID:
                        bi.setRGB(x, y, 0x000000);
                        break;
                    case VISITED:
                        bi.setRGB(x, y, 0x00FFFF);
                    default:
                        int red = Math.min(255, 64 + element);
                        bi.setRGB(x, y, red << 16);
                        break;
                }
            }
        }
        if (markCount != marked) {
            System.err.println("Error: Expected the mark count for edge=" + edge + " to be " + marked +
                               " but it was " + markCount);
        }

        File out = new File(String.format(Locale.ROOT, "edge_%03d_marks_%d.png", edge, markCount));
        ImageIO.write(bi, "png", out);
        System.out.println("Saved to " + out);
    }

    private class XYPair {
        public final XYPos pos1;
        public final XYPos pos2;

        public XYPair(XYPos pos1, XYPos pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }

        public XYPos getPos1() {
            return pos1;
        }

        public XYPos getPos2() {
            return pos2;
        }
    }

    public static class Pair<T, S> {
        public final T first;
        public final S second;

        public Pair(T first, S second) {
            this.first = first;
            this.second = second;
        }
    }
}
