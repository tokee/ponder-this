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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Special purpose map from hash to quad sets.
 * <p>
 * The sets are abstract and interaction goes through the map.
 */
public interface QuadEdgeMap {
    AtomicInteger need = new AtomicInteger(0);

    /**
     * @return IDs for free Quads in the abstract set.
     */
    IntStream getAvailableQuadIDs(long hash);

    /**
     * Increment need.
     * @return new need.
     */
    default int incNeed() {
        return need.incrementAndGet();
    }

    /**
     * Decrement need.
     * @return new need.
     */
    default int decNeed() {
        return need.decrementAndGet();
    }

    /**
     * Getting the size might mean a recalculation of the mask.
     * @return the number of remaining Quads.
     */
    int available(long hash);

    /**
     * needsSatisfied is not a guarantee as some quads might share pieces.
     * Checking involves calling {@link #available(long)} which might mean a recalculation of the mask.
     * @return true if needs are less than size.
     */
    default boolean needsSatisfied(long hash) {
        return need.get() <= available(hash);
    }

}
