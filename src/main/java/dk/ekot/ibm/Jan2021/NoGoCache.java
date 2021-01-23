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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Cache for checking whether antibots can make viable outcomes.
 * The first antibot in a given tuple is considered the major bot
 * and is used as key for the cache.
 */
public class NoGoCache {
    private final int width;
    private final int height;
    private final int maxEntries;
    private final FlatGrid emptyFinder;
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

    public NoGoCache(int width, int height, int maxEntries) {
        this.width = width;
        this.height = height;
        this.maxEntries = maxEntries;
        emptyFinder = new FlatGrid(width, height);
    }


    public boolean isViable(int[] antiBots) {
        final int primary = antiBots[0];
        Future<boolean[]> map;
        synchronized (emptyMaps) {
            map = emptyMaps.get(primary);
            if (map == null) {
                map = executor.submit(new Callable<boolean[]>() {
                    @Override
                    public boolean[] call() throws Exception {
                        // TODO: This is a bottleneck. We should have a pool of grids
                        synchronized (emptyFinder) {
                            emptyFinder.fullRun(new int[]{primary});
                            return emptyFinder.getVisitedMap();
                        }
                    }
                });
            }
            emptyMaps.put(primary, map);
        }
        for (int ai = 1 ; ai < antiBots.length ; ai++) {
            try {
                if (map.get()[ai]) {
                    return true;
                }
            } catch (Exception e) {
                // TODO: consider logging and returning true
                throw new RuntimeException("Exception while getting result", e);
            }
        }
        return false;
    }
}
