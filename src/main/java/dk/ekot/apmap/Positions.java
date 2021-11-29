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
    private int length;

    /**
     * Initialize with the given positions. -1 is reserved for flagging missing elements.
     * The positions are assumed to be unique.
     *
     * @param positions an array of positions.
     */
    public Positions(int[] positions) {
        this.positions = positions;
        length = positions.length;
    }

    /**
     * Initialize with the given positions. -1 is reserved for flagging missing elements.
     * The positions are assumed to be unique.
     *
     * @param positions a list of positions.
     */
    public Positions(List<Integer> positions) {
        this(positions.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Initialize the backing positions storage to the given size, but set {@link #length} to 0.
     * This is typically used for a caching structure where the size of Positions matter for reuse.
     * @param size the number of positions to make room for.
     */
    public Positions(int size) {
        this.positions = new int[size];
        this.length = 0;
    }

    public boolean isEmpty() {
        return index == length;
    }

    public int current() {
        return isEmpty() ? -1 : positions[index];
    }

    /**
     * @return next position or -1 if there are no more positions.
     */
    public int next() {
        if (isEmpty()) {
            return -1;
        }
        ++index;
        while (!isEmpty() && current() == -1) {
            ++index;
        }
        return current();
    }

    /**
     * Add a position after the current positions. Note that the structure does not auto-expand: The maximum number of
     * positions that can be held is fixed at creation time.
     * @param position a position in the form of an integer.
     */
    public void add(int position) {
        positions[length++] = position;
    }

    public void addAll(List<Integer> poss) {
        poss.forEach(this::add);
    }
    /**
     * Remove the given position from the array of positions.
     * If the position is not in the array of positions, this operation does not change anything.
     * This requires a seek through the array from the given {@link #index} {@code O(n)}.
     *
     * @param position the position to remove.
     * @return true is there are more positions.
     */
    public boolean remove(int position) {
        for (int i = index; i < length; i++) {
            if (positions[i] == position) {
                positions[i] = -1;
                if (i == index) { // current is invalidated, skip to next
                    next();
                }
                break;
            }
        }
        return !isEmpty();
    }

    public int getMaxCapacity() {
        return positions.length;
    }

    /**
     * Logical clear of the structure {@code O(1)}.
     */
    public void clear() {
        length = 0;
        index = 0;
    }

}
