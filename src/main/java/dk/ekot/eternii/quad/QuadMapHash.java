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
package dk.ekot.eternii.quad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Basic HashMap-based QuadMap.
 * Qua standard Java HashMap it works best with low to medium keys and medium to large array values.
 */
public class QuadMapHash implements QuadEdgeMap {
    private static final Logger log = LoggerFactory.getLogger(QuadMapHash.class);

    private final QuadBag quadBag;
    // (hash, quadIDs)
    private final Map<Long, int[]> quadMap = new HashMap<>();

    public QuadMapHash(QuadBag quadBag, Function<Long, Long> hasher) {
        this.quadBag = quadBag;
        fillMap(hasher);
    }

    private void fillMap(Function<Long, Long> hasher) {
        final long[] qedges = quadBag.getQedgesRaw();
        for (int quadID = 0 ; quadID < qedges.length ; quadID++) {
            final long hash = hasher.apply(qedges[quadID]);
            int[] mapQuadIDs = quadMap.get(hash);
            if (mapQuadIDs == null) {
                mapQuadIDs = new int[1];
                mapQuadIDs[0] = quadID;
            } else {
                int[] newMapQuadIDs = new int[mapQuadIDs.length+1];
                System.arraycopy(mapQuadIDs, 0, newMapQuadIDs, 0, mapQuadIDs.length);
                newMapQuadIDs[mapQuadIDs.length-1] = quadID;
                mapQuadIDs = newMapQuadIDs;
            }
            quadMap.put(hash, mapQuadIDs);
        }
    }

    @Override
    public IntStream getQuadIDs(long hash) {
        // TODO: Can the array from the quadMap be null?
        return Arrays.stream(quadMap.get(hash));
    }

    @Override
    public int availableQuads() {
        return 0;
    }
}
