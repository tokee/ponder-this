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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Pass-through quad edge map that delivers all valid quads form the underlying QuadBag.
 */
public class QuadMapAll implements QuadEdgeMap {
    private static final Logger log = LoggerFactory.getLogger(QuadMapAll.class);

    private final QuadBag quadBag;
    private final int edges;
    private final AtomicInteger need = new AtomicInteger(0);


    public QuadMapAll(QuadBag quadBag, int edges) {
        this.quadBag = quadBag;
        this.edges = edges;
    }

    @Override
    public IntStream getAvailableQuadIDs(long hash) {
        return quadBag.getAvailableQuadIDs();
    }

    @Override
    public IntStream getAvailableQuadIDsNoCache(long hash) {
        return quadBag.getAvailableQuadIDsNoCache();
    }

    @Override
    public int available(long hash) {
        return quadBag.available();
    }

    @Override
    public boolean hasNeeded(long hash, int need) {
        return quadBag.hasNeeded(need);
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
        return "QuadMapAll(edges=" + QBits.toStringEdges(edges) + ")";
    }
}
