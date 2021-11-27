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

import java.util.List;

/**
 * Holds an array of positions and allows for specific positions to be removed at any time.
 */
public class Positions {
    private int[] positions;
    private int index = 0;
    private int previousPosition = -1;

    /**
     * Initialize with the given positions. -1 is reserved for flagging missing elements.
     * The positions are assumed to be unique.
     *
     * @param positions an array of positions.
     */
    public Positions(int[] positions) {
        this.positions = positions;
    }

    public Positions(List<Integer> positions) {
        this.positions = positions.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean hasNext() {
        return index < positions.length;
    }

    public int previous() {
        if (previousPosition == -1) {
            throw new IllegalStateException("There was no previously delivered position");
        }
        return previousPosition;
    }

    public int current() {
        if (!hasNext()) {
            return -1;
        }
        return positions[index];
    }

    /**
     * @return next position or -1 if there are no more positions.
     */
    public int next() {
        if (!hasNext()) {
            return -1;
        }
        previousPosition = positions[index++];
        while (index < positions.length && positions[index] == -1) {
            index++;
        }
        return index == positions.length ? -1 : positions[index];
    }

    /**
     * Remove the given position from the array of positions. If the position is not in the array of positions,
     * this operation does not change anything.
     * This requires a seek through the array from the given {@link #index}.
     *
     * @param position the position to remove.
     */
    public void remove(int position) {
        for (int i = index; i < positions.length; i++) {
            if (positions[i] == position) {
                positions[i] = -1;
                if (i == index) {
                    index++;
                }
                break;
            }
        }
    }

}
