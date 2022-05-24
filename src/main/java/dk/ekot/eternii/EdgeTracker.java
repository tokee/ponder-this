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
package dk.ekot.eternii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *  Keeps track of open edges for pieces.
 */
public class EdgeTracker {
    private static final Logger log = LoggerFactory.getLogger(EdgeTracker.class);

    public static final int EF = 30; // Must be > max edge type id (int)

    // Edge -> count
    private final Counter one = new Counter();
    // [edge1, edge2 (clockwise immediately after edge1)] -> count
    private final Counter two = new Counter();
    // [edge1, edge2 (clockwise immediately after edge1), edge3 (clockwise immediately after edge2)] -> count
    private final Counter three = new Counter();
    // [edge1, edge2 (at the other side of edge)] -> count. One 180 degree rotation
    private final Counter opposing = new Counter();
    // [edge1, edge2, edge3, edge4] -> count. All rotations
    private final Counter four = new Counter();

    /**
     * Adds all valid permutations of the edges.
     * @param edge1 an edge or -1 if not present.
     * @param edge2 an edge or -1 if not present.
     * @param edge3 an edge or -1 if not present.
     * @param edge4 an edge or -1 if not present.
     * @param delta the value to add to the counters.
     */
    public void add(int edge1, int edge2, int edge3, int edge4, int delta) {
        // Four
        if (edge1 != -1 && edge2 != -1 && edge3 != -1 && edge4 != -1) { // Called often so this is optimized
            addFour(edge1, edge2, edge3, edge4, delta);
            addThree(edge2, edge3, edge4, delta);
            addThree(edge3, edge4, edge1, delta);
            addThree(edge4, edge1, edge2, delta);
            addTwo(edge1, edge2, delta);
            addTwo(edge2, edge3, delta);
            addTwo(edge3, edge4, delta);
            addTwo(edge4, edge1, delta);
            addOne(edge1, delta);
            addOne(edge2, delta);
            addOne(edge3, delta);
            addOne(edge4, delta);
            addOpposing(edge1, edge3, delta);
            addOpposing(edge2, edge4, delta);
            return;
        }

        // Three
        if (edge1 != -1 && edge2 != -1 && edge3 != -1) {
            addThree(edge1, edge2, edge3, delta);
        }
        if (edge2 != -1 && edge3 != -1 && edge4 != -1) {
            addThree(edge2, edge3, edge4, delta);
        }
        if (edge3 != -1 && edge4 != -1 && edge1 != -1) {
            addThree(edge3, edge4, edge1, delta);
        }
        if (edge4 != -1 && edge1 != -1 && edge2 != -1) {
            addThree(edge4, edge1, edge2, delta);
        }

        // Two
        if (edge1 != -1 && edge2 != -1) {
            addTwo(edge1, edge2, delta);
        }
        if (edge2 != -1 && edge3 != -1) {
            addTwo(edge2, edge3, delta);
        }
        if (edge3 != -1 && edge4 != -1) {
            addTwo(edge3, edge4, delta);
        }
        if (edge4 != -1 && edge1 != -1) {
            addTwo(edge4, edge1, delta);
        }

        // One
        if (edge1 != -1) {
            addOne(edge1, delta);
        }
        if (edge2 != -1) {
            addOne(edge2, delta);
        }
        if (edge3 != -1) {
            addOne(edge3, delta);
        }
        if (edge4 != -1) {
            addOne(edge4, delta);
        }

        // Opposing
        if (edge1 != -1 && edge3 == -1) {
            addOpposing(edge1, edge3, delta);
        }
        if (edge2 != -1 && edge4 == -1) {
            addOpposing(edge2, edge4, delta);
        }
    }

    public void addOne(int edgeType1, int delta) {
        one.add(edgeType1, delta);
    }
    public int getOne(int edgeType1) {
        return one.get(edgeType1);
    }

    public void addTwo(int edgeType1, int edgeType2, int delta) {
        two.add(EF * edgeType1 + edgeType2, delta);
    }
    public int getTwo(int edgeType1, int edgeType2) {
        return two.get(EF * edgeType1 + edgeType2);
    }

    public void addOpposing(int edgeType1, int edgeType2, int delta) {
        opposing.add(EF * edgeType1 + edgeType2, delta);
        if (edgeType1 != edgeType2) {
            opposing.add(EF * edgeType2 + edgeType1, delta);
        }
    }
    public int getOpposing(int edgeType1, int edgeType2) {
        if (edgeType1 != edgeType2) {
            return opposing.get(EF * edgeType1 + edgeType2) +
                   opposing.get(EF * edgeType2 + edgeType1);
        }
        return opposing.get(EF * edgeType1 + edgeType2);
    }

    public void addThree(int edgeType1, int edgeType2, int edgeType3, int delta) {
        three.add(EF * EF * edgeType1 + EF * edgeType2 + edgeType3, delta);
    }
    public int getThree(int edgeType1, int edgeType2, int edgeType3) {
        return three.get(EF * EF * edgeType1 + EF * edgeType2 + edgeType3);
    }

    public void addFour(int edgeType1, int edgeType2, int edgeType3, int edgeType4, int delta) {
        visitFour(edgeType1, edgeType2, edgeType3, edgeType4, key -> four.add(key, delta));
    }
    public int getFour(int edgeType1, int edgeType2, int edgeType3, int edgeType4) {
        AtomicInteger sum = new AtomicInteger(0);
        visitFour(edgeType1, edgeType2, edgeType3, edgeType4, key -> sum.addAndGet(four.get(key)));
        return sum.get();
    }
    private void visitFour(int edgeType1, int edgeType2, int edgeType3, int edgeType4, Consumer<Integer> callback) {
        callback.accept(EF * EF * EF * edgeType1 + EF * EF * edgeType2 + EF * edgeType3 + edgeType4);
        callback.accept(EF * EF * EF * edgeType2 + EF * EF * edgeType3 + EF * edgeType4 + edgeType1);
        callback.accept(EF * EF * EF * edgeType3 + EF * EF * edgeType4 + EF * edgeType1 + edgeType2);
        callback.accept(EF * EF * EF * edgeType4 + EF * EF * edgeType1 + EF * edgeType2 + edgeType3);
    }


    public static class Counter extends HashMap<Integer, Integer> {
        private void add(int key, int delta) {
            put(key, get(key)+delta);
        }
        private void dec(int key) {
            put(key, get(key)-1);
        }
        private void inc(int key) {
            put(key, get(key)+1);
        }
        private int get(int key) {
            Integer count = super.get(key);
            return count == null ? 0 : count;
        }
    }
}
