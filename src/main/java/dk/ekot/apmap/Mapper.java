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

import java.util.Arrays;

/**
 * Transforms from and to a quadratic representation of a hexagonal board. Supports visualization.
 *
 * Note: This implementation is not optimized for speed or low memory footprint.
 */
public class Mapper {
    private static final Logger log = LoggerFactory.getLogger(Mapper.class);

    static final int NEUTRAL = 0;
    static final int MARKER = 1;
    static final int ILLEGAL = 2;
    static final int INVALID = 3;

    final int edge; // Hexagonal edge
    final int width;
    final int height;
    final int[][] quadratic; // y, x index

    /**
     * @param edge the edge of the hexagonal board.
     */
    public Mapper(int edge) {
        this.edge = edge;
        this.width = edge*4-2;
        this.height = edge*2-1;
        quadratic = new int[height][width];

        // Draw the quadratic map
        for (int y = 0 ; y < height ; y++) {
            Arrays.fill(quadratic[y], INVALID);
        }
        for (int y = 0 ; y < height ; y++) {
            int margin = Math.abs(y-height/2);
            //System.out.println("y=" + y + ", margin=" + margin + ", side=" + side + ", x=[" + margin + ", " + (side-margin-1) + "]");
            for (int x = margin ; x < width-margin ; x+=2) {
                quadratic[y][x] = NEUTRAL;
            }
        }
    }

    /**
     * @return the number of valid elements in the abstract hexagon.
     */
    public int elementCount() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (quadratic[y][x] != INVALID) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * @return the state of the map flattened to a single array.
     */
    public int[] getFlat() {
        int[] flat = new int[elementCount()];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (quadratic[y][x] != INVALID) {
                    flat[index++] = quadratic[y][x];
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
        if (flat.length != elementCount()) {
            throw new IllegalArgumentException(
                    "The length of the flat structure was " + flat.length +
                    " while the number of vali entries in the map was " + elementCount());
        }
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (quadratic[y][x] != INVALID) {
                    quadratic[y][x] = flat[index++];
                }
            }
        }
    }

    public String toJSON() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(quadratic.length*2);
        for (int y = 0 ; y < height ; y++) {
            sb.append((String.format("%3d: ", y)));
            for (int x = 0; x < width; x++) {
                switch (quadratic[y][x]) {
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
                        throw new UnsupportedOperationException("Unknown element: " + quadratic[x][y]);
                }
                sb.append(x < width-1 ? " " : "");
            }
            sb.append(y < height-1 ? "\n" : "");
        }
        return sb.toString();
    }

}
