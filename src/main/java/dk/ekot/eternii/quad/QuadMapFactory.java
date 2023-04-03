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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public class QuadMapFactory {
    private static final Logger log = LoggerFactory.getLogger(QuadMapFactory.class);
    private final int maxHash;
    private final UniqueCounter counter;

    public QuadMapFactory(int maxHash, Stream<Integer> hashStream) {
        this.maxHash = maxHash;
        if (maxHash <= 1024 * 1024) { // Single + double side
            counter = new UniqueCounterArray(maxHash + 1);
        } else {
            counter = new UniqueCounterHash();
        }
        hashStream.forEach(counter);
        log.info("QuadFactory got stated maxHash={}, found {}",
                 maxHash, counter.stats());
        // TODO: Finish implementation
    }

    private interface UniqueCounter extends Consumer<Integer> {
        /**
         * @return the number of times the hash has been counted.
         */
        int getCount(int hash);

        /**
         * @return the count for the hash with the highest count.
         */
        int getMaxCount();

        /**
         * @return the hash with the highest numeric value.
         */
        int getMaxHash();

        long getCountSum();

        int getUniqueHashCount();

        default String stats() {
            return "maxCount=" + getMaxCount() + ", maxHash=" + getMaxHash() + ", uniqueHash=" + getUniqueHashCount() +
                   ", countSum=" + getCountSum();
        }
    }

    private static class UniqueCounterArray implements UniqueCounter {
        private final int[] counters;

        public UniqueCounterArray(int hashes) {
            counters = new int[hashes];
        }

        @Override
        public void accept(Integer hash) {
            ++counters[hash];
        }

        @Override
        public int getCount(int hash) {
            return counters[hash];
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
        public int getMaxHash() {
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
        private final Map<Integer, Integer> countMap = new HashMap<>();

        @Override
        public int getCount(int hash) {
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
        public int getMaxHash() {
            return countMap.keySet().stream().
                    mapToInt(Integer::intValue).
                    max().orElse(0);
        }

        @Override
        public long getCountSum() {
            return countMap.values().stream().
                    mapToInt(Integer::intValue).
                    sum();
        }
        @Override
        public void accept(Integer hash) {
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