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
package dk.ekot.ibm.Jan2021;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache for checking whether antibots can make viable outcomes.
 * The first antibot in a given tuple is considered the major bot
 * and is used as key for the cache.
 */
public class ViabilityChecker {
    private static final int MAX_ENTRIES_DEFAULT = 1000;
    private final int width;
    private final int height;
    private final int maxEntries;
    private final FlatGrid emptyFinder;
    private final Future<boolean[]> noAntisMap;
    private final Map<Integer, Future<boolean[]>> emptyMaps = new LinkedHashMap<Integer, Future<boolean[]>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxEntries;
        }
    };
    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        int threadID = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "nogo_" + threadID++);
            t.setDaemon(true);
            return t;
        }
    });
    private final AtomicLong viables = new AtomicLong();
    private final AtomicLong nonViables = new AtomicLong();
    private final AtomicLong emptyCreated = new AtomicLong();

    public ViabilityChecker(int width, int height) {
        this(width, height, MAX_ENTRIES_DEFAULT);
    }
    public ViabilityChecker(int width, int height, int maxEntries) {
        this.width = width;
        this.height = height;
        this.maxEntries = maxEntries;
        emptyFinder = new FlatGrid(width, height);
        noAntisMap = executor.submit(() -> {
            synchronized (emptyFinder) {
                emptyFinder.clear();
                emptyFinder.fullRun();
                emptyCreated.incrementAndGet();
                return emptyFinder.getVisitedMap();
            }
        });
    }

    public boolean isViable(int[] antiBots) {
        final int primary = antiBots[0];
        Future<boolean[]> map;
        synchronized (emptyMaps) {
            map = emptyMaps.get(primary);
            if (map == null) {
                boolean isInNoAntis;
                try {
                    isInNoAntis = noAntisMap.get()[primary];
                } catch (Exception e) {
                    throw new RuntimeException("Internal error: Exception while requesting noAntisMap", e);
                }
                map = !isInNoAntis ? noAntisMap : executor.submit(() -> {
                    // TODO: This is a bottleneck. We should have a pool of grids
                    synchronized (emptyFinder) {
                        emptyFinder.fullRun(new int[]{primary});
                        emptyCreated.incrementAndGet();
                        return emptyFinder.getVisitedMap();
                    }
                });
            }
            emptyMaps.put(primary, map);
        }
        for (int ai = 1 ; ai < antiBots.length ; ai++) {
            try {
                if (map.get()[antiBots[ai]]) {
                    viables.incrementAndGet();
                    return true;
                }
            } catch (Exception e) {
                // TODO: consider logging and returning true
                throw new RuntimeException("Exception while getting result", e);
            }
        }
        nonViables.incrementAndGet();
        return false;
    }

    // ViabilityChecker(calls=62795, viable=58824, non_viable=3971, gridwalks=258)
    // TopD(106, 106) ms=    40,068, antis=2: [( 99,  99), (  0,  54)]
    // ViabilityChecker(calls=62794, viable=58823, non_viable=3971, gridwalks=258)
    // TopD(106, 106) ms=    40,398, antis=2: [( 99,  99), (  0,  54)]

    // ViabilityChecker(calls=62799, viable=58828, non_viable=3971, checkGridBuilds=232)
    // TopD(106, 106) ms=    38,928, antis=2: [( 99,  99), (  0,  54)]
    public String toString() {
        return String.format(Locale.ROOT, "ViabilityChecker(calls=%d, viable=%d, non_viable=%d, checkGridBuilds=%d)",
                             viables.get() + nonViables.get(), viables.get(), nonViables.get(), emptyCreated.get());
    }

    public long getViableCount() {
        return viables.get();
    }

    public long getNonViableCount() {
        return nonViables.get();
    }
}
