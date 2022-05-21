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
import java.util.Map;

/**
 *  Keeps track of open edges for pieces.
 */
public class EdgeTracker {
    private static final Logger log = LoggerFactory.getLogger(EdgeTracker.class);

    public static final int EDGE_FACTOR = 30; // Must be > max edge type id (int)

    // Edge -> count
    private final Map<Integer, Integer> single = new HashMap<>();
    // [edge1, edge2 (clockwise immediately after edge1)] -> count
    private final Map<Integer, Integer> followingTwo = new HashMap<>();
    // [edge1, edge2 (clockwise immediately after edge1), edge3 (clockwise immediately after edge2)] -> count
    private final Map<Integer, Integer> followingThree = new HashMap<>();
    // [edge1, edge2 (at the other side of edge)] -> count
    private final Map<Integer, Integer> opposing = new HashMap<>();

    public void registerSingle(int edgeType1) {
        increment(single, edgeType1);
    }

    public void registerFollowing(int edgeType1, int edgeType2) {
        int compoundID = EDGE_FACTOR*edgeType1 + edgeType2;
        increment(followingTwo, compoundID);
    }

    public void registerOpposing(int edgeType1, int edgeType2) {
        int compoundID = EDGE_FACTOR*edgeType1 + edgeType2;
        increment(opposing, compoundID);
    }

    public void registerFollowing(int edgeType1, int edgeType2, int edgeType3) {
        int compoundID = EDGE_FACTOR*EDGE_FACTOR*edgeType1 + EDGE_FACTOR*edgeType2 + edgeType3;
        increment(followingThree, compoundID);
    }

    private void increment(Map<Integer, Integer> counters, int key) {
        Integer count = counters.get(key);
        counters.put(key, count == null ? 1 : count + 1);
    }
}
