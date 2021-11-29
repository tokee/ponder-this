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
package dk.ekot.apmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Special pool for Positions which always returns a {@link Positions} on {@link #get(Object)} by recreating missing
 * entries.
 */
public class PositionsPool extends LinkedHashMap<Integer, Positions> {
    private static final Logger log = LoggerFactory.getLogger(PositionsPool.class);

    private final int maxCached;
    private final int boardValids;
    private final BiConsumer<Integer, Positions> filler;
    private List<Positions> reusables = new ArrayList<>();

    /**
     *
     * @param maxCached   the maximum number of {@link Positions} to hold in the pool.
     * @param boardValids the number of valid elements on the board. Used for creating new Positions.
     * @param filler      callback for filling the given {@code <depth, Positions>} if needed.
     */
    public PositionsPool(int maxCached, int boardValids, BiConsumer<Integer, Positions> filler) {
        super(maxCached);
        this.maxCached = maxCached;
        this.boardValids = boardValids;
        this.filler = filler;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Positions> eldest) {
        if (size() < maxCached) {
            return false;
        }
        reusables.add(eldest.getValue());
        return true;
    }

    @Override
    public Positions get(Object key) {
        if (!(key instanceof Integer)) {
            return null;
        }
        Integer depth = (Integer)key;
        Positions pos = super.get(depth);
        if (pos == null) {
            pos = getCleanPositions();
            filler.accept(depth, pos);
        }
        put(depth, pos);
        return pos;
    }

    @Override
    public void clear() {
        reusables.addAll(values());
        super.clear();
    }

    @Override
    public Positions remove(Object key) {
        Positions removed = super.remove(key);
        if (removed != null) {
            reusables.add(removed);
        }
        return removed;
    }

    public Positions getCleanPositions() {
        if (reusables.isEmpty()) {
            return new Positions(boardValids);
        }
        Positions repurposed = reusables.remove(reusables.size()-1);
        repurposed.clear();
        return repurposed;
    }

}
