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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Basic HashMap-based QuadMap.
 * Qua standard Java HashMap it works best with low to medium keys and medium to large array values.
 */
public class QuadMapHash implements QuadEdgeMap {
    private static final Logger log = LoggerFactory.getLogger(QuadMapHash.class);

    private final AtomicInteger need = new AtomicInteger(0);
    private final int edges;
    private final QuadBag quadBag;
    // (hash, quadIDs)
    private final Map<Long, int[]> quadMap = new HashMap<>();

    public QuadMapHash(QuadBag quadBag, Function<Long, Long> hasher, int edges) {
        this.quadBag = quadBag;
        this.edges = edges;
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
    public IntStream getAvailableQuadIDs(long hash) {
        int[] quadIDs = quadMap.get(hash);
        //        log.debug("Unfiltered quads for hash {}: {}",
        //          hash, quadIDs != null ? quadIDs.length : "N/A");
        if (quadIDs == null) {
            // We should never reach this as isOK should fail and cause a rolleback
            throw new IllegalArgumentException(
                    "Requested quadIDs for hash " + hash + " from quad map for bag type " + quadBag.getType() +
                    " for edges " + QBits.toStringEdges(edges) + " but got null");
        }
        return Arrays.stream(quadIDs).
                filter(quadBag::isAvailable);
    }

    @Override
    public IntStream getAvailableQuadIDsNoCache(long hash) {
        int[] quadIDs = quadMap.get(hash);
        //        log.debug("Unfiltered quads for hash {}: {}",
        //          hash, quadIDs != null ? quadIDs.length : "N/A");
        if (quadIDs == null) {
            // We should never reach this as isOK should fail and cause a rolleback
            throw new IllegalArgumentException(
                    "Requested quadIDs for hash " + hash + " from quad map for bag type " + quadBag.getType() +
                    " for edges " + QBits.toStringEdges(edges) + " but got null");
        }
        return Arrays.stream(quadIDs).
                filter(quadBag::isAvailableNoCache);
    }

    @Override
    public int available(long hash) {
        int[] quadIDs = quadMap.get(hash);
        if (quadIDs == null) {
            //log.debug("available({}) for type {} got null quadIDs from the map",
            //          hash, quadBag.getType());
            return 0;
        }
        return (int) Arrays.stream(quadIDs).
                filter(quadBag::isAvailable).
                count();
    }

    @Override
    public boolean hasNeeded(long hash, int need) {
        int[] quadIDs = quadMap.get(hash);
        if (quadIDs == null || quadIDs.length < need) {
            return false;
        }
        return Arrays.stream(quadIDs).
                       filter(quadBag::isAvailableNoCache).
                       limit(need).
                       count() >= need;
    }

    @Override
    public int getEdges() {
        return edges;
    }

    @Override
    public AtomicInteger getNeed() {
        return need;
    }

    public String toString() {
        return "QuadMapHash(bag=" + quadBag.getType() + ", edges=" + QBits.toStringEdges(edges) +
                ", #hashes=" + quadMap.size() + ")";
    }
}
