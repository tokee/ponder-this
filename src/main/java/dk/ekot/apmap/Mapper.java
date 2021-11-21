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

    final int edge; // Hexagonal edge
    final int width;
    final int height;
    final int valids;
    final int[] quadratic; // top-down, left-right. (0, 0) is top left
    final int[] priority;  // 0-∞. Lower numbers are better (idea #13)
    final short[] tripleDeltas; // [deltaX1, deltaY1, deltaX2, deltaY2]*
    
//    final long[][] tripleDeltasByColumn; // [deltaX1, deltaY1, deltaX2, deltaY2]*
//    final long[][] tripleDeltasByRow; // [deltaX1, deltaY1, deltaX2, deltaY2]*

    int marked = 0;
    int neutrals;

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
        width = edge*4-2;
        height = edge*2-1;
        quadratic = new int[height*width];
        priority = new int[height*width];

        // Draw the quadratic map
        Arrays.fill(quadratic, INVALID);
        int v = 0;
        for (int y = 0 ; y < height ; y++) {
            int margin = Math.abs(y-height/2);
            //System.out.println("y=" + y + ", margin=" + margin + ", side=" + side + ", x=[" + margin + ", " + (side-margin-1) + "]");
            for (int x = margin ; x < width-margin ; x+=2) {
                setQuadratic(x, y, NEUTRAL); //quadratic[y][x] = NEUTRAL;
                ++v;
            }
        }
        valids = v;
        neutrals = valids;
        tripleDeltas = getTripleDeltas();
//        tripleDeltasByColumn = getDeltaColumns();
//        tripleDeltasByRow = getDeltaRows();
    }

    private Mapper(Mapper other) {
        this.edge = other.edge;
        this.height = other.height;
        this.width = other.width;
        this.valids = other.valids;

        this.marked = other.marked;
        this.neutrals = other.neutrals;
        this.quadratic = Arrays.copyOf(other.quadratic, other.quadratic.length);
        this.priority = Arrays.copyOf(other.priority, other.priority.length);

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

    public int getMarkedCount() {
        return marked;
    }

    public int getNeutralCount() {
        return neutrals;
    }

    /**
     * Marks the given quadratic (x, y) and adds the coordinated to changed at changedIndex.
     * Uses the {@link #tripleDeltas} to resolve all fields that are neutral and where setting a mark would cause
     * a triple (arithmetic progression). The fields are set to {@link #ILLEGAL} and their coordinate pairs are added
     * to changed, with changedIndex being incremented accordingly.
     * This updates {@link #marked} and {@link #neutrals}.
     * @param x quadratic coordinate X.
     * @param y quadratic coordinate Y.
     * @param changed change tracking array.
     * @param changedIndex index into the change tracking array.
     * @return the new changedIndex. Will always be at least 2 more than previously.
     */
    public int markAndDeltaExpand(final int x, final int y, int[] changed, int changedIndex) {
        changed[changedIndex++] = x;
        changed[changedIndex++] = y;
        if (getQuadratic(x, y) != NEUTRAL) {
            throw new IllegalStateException(
                    "Attempted to mark (" + x + ", " + y + ") bit it already had state " + getQuadratic(x, y));
        }
        setQuadratic(x, y, MARKER);
        ++marked;
        --neutrals;
        for (int i = 0 ; i < tripleDeltas.length ; i+=4) {
            final int x1 = x+tripleDeltas[i];
            final int y1 = y+tripleDeltas[i+1];
            final int x2 = x+tripleDeltas[i+2];
            final int y2 = y+tripleDeltas[i+3];
            if (getQuadratic(x1, y1) == MARKER) {
                if (getQuadratic(x2, y2) == NEUTRAL) {
                    setQuadratic(x2, y2, ILLEGAL);
                    changed[changedIndex++] = x2;
                    changed[changedIndex++] = y2;
                    --neutrals;
                }
            } else if (getQuadratic(x2, y2) == MARKER) {
                if (getQuadratic(x1, y1) == NEUTRAL) {
                    setQuadratic(x1, y1, ILLEGAL);
                    changed[changedIndex++] = x1;
                    changed[changedIndex++] = y1;
                    --neutrals;
                }
            }
        }
        return changedIndex;
    }

    /**
     * Mark all quadratic coordinate pairs in {@code changed[from..to]} (to is exclusive) as {@link #NEUTRAL}.
     * This performs {@code --marked} and {@code neutrals += (to-from)/2}.
     * @param changed array of quadratic coordinates.
     * @param from from index in changed (inclusive).
     * @param to to index in changed (exclusive).
     */
    public void rollback(int[] changed, int from, int to) {
        for (int i = from ; i < to ; i+=2) {
            setQuadratic(changed[i], changed[i+1], NEUTRAL);
        }
        --marked;
        neutrals += (to-from)>>1;
    }

    public Mapper copy() {
        return new Mapper(this);
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
        // TODO: Ca the width/2+1 be reduced a tiny bit? w3->1, w4->1, w5->2, w6->2, w7->3
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
        log.info("edge=" + edge + ", extracted " + triples.size()/4 + " delta triples " +
                 "(" + triples.size()*4/1024 + " KBytes)");
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
     * @return {@code long[column(0..height)][]} og triples packed as single longs (x1, x2, y1, y2 as 2 bytes/each)
     *         and sorted ascending.
     */
    private long[][] getDeltaColumns() {
        final long[] buffer = new long[tripleDeltas.length/4];
        final long[][] deltaColumns = new long[height][];
        long tripleCount = 0;

        for (int column = 0 ; column < width ; column++) {
            int bufPos = 0;
            for (int i = 0 ; i < tripleDeltas.length ; i+=4) {
                final int y1 = column + tripleDeltas[i+1];
                final int y2 = column + tripleDeltas[i + 3];
                if (y1 >= 0 && y1 < height && y2 >= 0 && y2 < height) {
                    long packed =
                            ((long) tripleDeltas[i]) << 48 |
                            ((long) tripleDeltas[i + 1]) << 32 |
                            ((long) tripleDeltas[i + 2]) << 16 |
                            ((long) tripleDeltas[i + 3]);
                    buffer[bufPos++] = packed;
                }
            }
            //System.out.println("deltaColumns[" + column + "].length=" + bufPos);
            deltaColumns[column] = Arrays.copyOf(buffer, bufPos);
            Arrays.sort(deltaColumns[column]);
            tripleCount += bufPos;
        }

        log.info("Extracted " + tripleCount + " column triples ~= " + tripleCount/8/1024 + "KBytes");
        return deltaColumns;
    }

    /**
     * @return {@code long[row(0..height)][]} og triples packed as single longs (x1, x2, y1, y2 as 2 bytes/each)
     *         and sorted ascending.
     */
    private long[][] getDeltaRows() {
        final long[] buffer = new long[tripleDeltas.length/4];
        final long[][] deltaRows = new long[width][];
        long tripleCount = 0;

        for (int row = 0 ; row < height ; row++) {
            int bufPos = 0;
            for (int i = 0 ; i < tripleDeltas.length ; i+=4) {
                final int x1 = row + tripleDeltas[i];
                final int x2 = row + tripleDeltas[i + 2];
                if (x1 >= 0 && x1 < width && x2 >= 0 && x2 < width) {
                    long packed =
                            ((long) tripleDeltas[i]) << 48 |
                            ((long) tripleDeltas[i + 1]) << 32 |
                            ((long) tripleDeltas[i + 2]) << 16 |
                            ((long) tripleDeltas[i + 3]);
                    buffer[bufPos++] = packed;
                }
            }
            deltaRows[row] = Arrays.copyOf(buffer, bufPos);
            Arrays.sort(deltaRows[row]);
            tripleCount += bufPos;
        }

        log.info("Extracted " + tripleCount + " row triples ~= " + tripleCount/8/1024 + "KBytes");
        return deltaRows;
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
                        sb.append("O");
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
