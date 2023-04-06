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
import java.util.function.Consumer;
import java.util.function.Function;

public class QuadMapFactory {
    private static final Logger log = LoggerFactory.getLogger(QuadMapFactory.class);

    public static final int MAX_PCOL = 27;
    public static final int MAX_QCOL_EDGE1 = MAX_PCOL * MAX_PCOL;

    public static QuadEdgeMap generateMap(
            QuadBag quadBag, long maxHash, Function<Long, Long> hasher) {
//        dumpStats(quadBag, maxHash, hasher);
        // TODO: Introduce optimized maps, consider replacing 0b1111 with 0b111 and an extra check
        return new QuadMapHash(quadBag, hasher);
    }

    /**
     * Calculate and output stats for maps.
     */
    private static void dumpStats(QuadBag quadBag, long maxHash, Function<Long, Long> hasher) {
        UniqueCounter counter;
        if (maxHash <= MAX_QCOL_EDGE1) { // Single + double side
            counter = new UniqueCounterArray(Math.toIntExact(maxHash + 1));
        } else {
            counter = new UniqueCounterHash();
        }
        long[] qedges = quadBag.getQedgesRaw();
        Arrays.stream(qedges).
                boxed().
                map(hasher).
                forEach(counter);
        log.info("QB {} set got stated maxHash={}, #quads={}, found {}",
                 quadBag.getType(), maxHash, qedges.length, counter.stats());
    }

    private interface UniqueCounter extends Consumer<Long> {
        /**
         * @return the number of times the hash has been counted.
         */
        int getCount(long hash);

        /**
         * @return the count for the hash with the highest count.
         */
        int getMaxCount();

        /**
         * @return the hash with the highest numeric value.
         */
        long getMaxHash();

        long getCountSum();

        int getUniqueHashCount();

        default String stats() {
            return "maxSameHash=" + getMaxCount() + ", maxHash=" + getMaxHash() + ", #uniqueHash=" + getUniqueHashCount() +
                   ", countSum=" + getCountSum(); // countSum is sanity check
        }
    }

    private static class UniqueCounterArray implements UniqueCounter {
        private final int[] counters;

        public UniqueCounterArray(int hashes) {
            counters = new int[hashes];
        }

        @Override
        public void accept(Long hash) {
            ++counters[Math.toIntExact(hash)];
        }

        @Override
        public int getCount(long hash) {
            return counters[Math.toIntExact(hash)];
        }

        @SuppressWarnings("ForLoopReplaceableByForEach")
        @Override
        public int getMaxCount() {
            int max = 0;
            for (int i = 0; i < counters.length; i++) {
                if (counters[i] > max) {
                    max = counters[i];
                }
            }
            return max;
        }

        @Override
        public long getMaxHash() {
            int maxIndex = 0;
            for (int i = 0; i < counters.length; i++) {
                if (counters[i] != 0) {
                    maxIndex = i;
                }
            }
            return maxIndex;
        }

        @Override
        public long getCountSum() {
            int sum = 0;
            for (int i = 0; i < counters.length; i++) {
                sum += counters[i];
            }
            return sum;
        }

        @Override
        public int getUniqueHashCount() {
            int unique = 0;
            for (int i = 0; i < counters.length; i++) {
                if (counters[i] != 0) {
                    unique++;
                }
            }
            return unique;
        }
    }

    private static class UniqueCounterHash implements UniqueCounter {
        // (hash, count)
        private final Map<Long, Integer> countMap = new HashMap<>();

        @Override
        public int getCount(long hash) {
            // Fails on unknown key on purpose (should only be called with seen keys)
            return countMap.get(hash);
        }

        @Override
        public int getMaxCount() {
            return countMap.values().stream().
                    mapToInt(Integer::intValue).
                    max().orElse(0);
        }

        @Override
        public long getMaxHash() {
            return countMap.keySet().stream().
                    mapToLong(Long::longValue).
                    max().orElse(0);
        }

        @Override
        public long getCountSum() {
            return countMap.values().stream().
                    mapToInt(Integer::intValue).
                    sum();
        }
        @Override
        public void accept(Long hash) {
            if (!countMap.containsKey(hash)) {
                countMap.put(hash, 1);
            } else {
                countMap.put(hash, countMap.get(hash)+1);
            }
        }

        @Override
        public int getUniqueHashCount() {
            return countMap.size();
        }
    }
}