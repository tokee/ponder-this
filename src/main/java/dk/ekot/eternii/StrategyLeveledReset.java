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

/**
 * If a given level takes too long to react, backtracking at most X levels is initiated.
 * "Too long" is x2*level^2 + x*level + base.
 * "X levels" is a maxBacktrack.
 */
public class StrategyLeveledReset extends StrategyBase {
    private static final Logger log = LoggerFactory.getLogger(StrategyLeveledReset.class);

    private final double base;
    private final double x;
    private final double x2;
    private final long[] maxTopTimesMS = new long[256];

    private final int maxBacktrack;
    private final long maxTotalTimeMS;

    private final long quitTime;
    private int best = 0;

    public StrategyLeveledReset(Walker walker, EListener listener, double x2, double x, double base, int maxBacktrack) {
        this(walker, listener, x2, x, base, maxBacktrack, Integer.MAX_VALUE);
    }
    public StrategyLeveledReset(Walker walker, EListener listener, double x2, double x, double base,
                                int maxBacktrack, int maxTotalTimeMS) {
        super(walker, listener, true, false, true);
        this.x2 = x2;
        this.x = x;
        this.base = base;

        for (int i = 0; i < maxTopTimesMS.length ; i++) {
            maxTopTimesMS[i] = Math.round(x2*i*i + x*i + base);
        }
        this.maxBacktrack = maxBacktrack;
        this.maxTotalTimeMS = maxTotalTimeMS;
        quitTime = System.currentTimeMillis()+maxTotalTimeMS;
    }

    @Override
    public Action getAction(StrategySolverState state) {
        if (System.currentTimeMillis() > quitTime) {
            return Action.quit();
        }
        if (state.getLevel() > best) {
            best = state.getLevel();
            listener.localBest(Thread.currentThread().getName(), this, state.getBoard(), state);
        }
        if (state.getMsFromTop() > maxTopTimesMS[state.getLevel()]) {
            return Action.restartLevel(Math.max(0, state.getLevel()-maxBacktrack));
        }
        return Action.continueLocal();
    }
}
