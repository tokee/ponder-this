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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transforms from and to flat and quadratic representations of a hexagonal board. Supports visualization.
 *
 * Note: This implementation is not optimized for speed or low memory footprint.
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

    static final int INVALID = -1;
    static final int NEUTRAL = 0;
    static final int MARKER = 1;
    static final int ILLEGAL = 2;

    final int edge; // Hexagonal edge
    final int width;
    final int height;
    final int valids;
    final int[] quadratic; // top-down, left-right. (0, 0) is top left

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
    }

    private void setQuadratic(int x, int y, int element) {
        quadratic[y*width+x] = element;
    }
    private int getQuadratic(int x, int y) {
        return quadratic[y*width+x];
    }

    private Mapper(Mapper other) {
        this.edge = other.edge;
        this.height = other.height;
        this.width = other.width;
        this.valids = other.valids;
        this.quadratic = new int[other.quadratic.length];
        System.arraycopy(other.quadratic, 0, quadratic, 0, quadratic.length);
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
