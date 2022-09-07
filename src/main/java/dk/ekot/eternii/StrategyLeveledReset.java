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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * If a given level takes too long to progress, backtracking at most X levels is initiated.
 * "Too long" is x*level^y + base.
 * "X levels" is a maxBacktrack.
 */
public class StrategyLeveledReset extends StrategyBase {
    private static final Logger log = LoggerFactory.getLogger(StrategyLeveledReset.class);

    private final double base;
    private final double x;
    private final double y;
    private final long[] maxTopTimesMS = new long[256];


    private final int maxBacktrack;
    private final long maxTotalTimeMS;

    private final long quitTime;
    private int totalBest = 0;

    private int localBest = 0;
    private long localStartTime = System.currentTimeMillis();

    public StrategyLeveledReset(Walker walker, EListener listener, double x, double y, double base, int maxBacktrack) {
        this(walker, listener, x, y, base, maxBacktrack, Integer.MAX_VALUE);
    }
    public StrategyLeveledReset(Walker walker, EListener listener, double x, double y, double base,
                                int maxBacktrack, int maxTotalTimeMS) {
        super(walker, listener, true, false, true);
        this.x = x;
        this.y = y;
        this.base = base;

        for (int i = 0; i < maxTopTimesMS.length ; i++) {
            maxTopTimesMS[i] = Math.round(x*Math.pow(i, y) + base);
        }
        this.maxBacktrack = maxBacktrack;
        this.maxTotalTimeMS = maxTotalTimeMS;
        quitTime = System.currentTimeMillis()+maxTotalTimeMS;
        setOnlySingleField(false); // Does not make sense to have deterministic here
        System.out.println("Reset times: " + Arrays.stream(new int[]{0, 50, 100, 150, 200, 250}).
                boxed().
                map(i -> maxTopTimesMS[i]).
                collect(Collectors.toList()));
    }

    @Override
    public Action getAction(StrategySolverState state) {
        if (state.getLevel() > totalBest) {
            totalBest = state.getLevel();
            listener.localBest(Thread.currentThread().getName(), this, state.getBoard(), state);
        }
        if (System.currentTimeMillis() > quitTime) {
            return Action.quit();
        }
        if (state.getLevel() > localBest) {
            localBest = state.getLevel();
            localStartTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis()-localStartTime > maxTopTimesMS[state.getLevel()]) {
            int resetLevel = Math.max(0, state.getLevel()-maxBacktrack);
            localBest = -1;
            //System.out.println("Resetting from " + state.getLevel() + " to " + resetLevel);
            return Action.restartLevel(resetLevel);
        }

        return Action.continueLocal();
    }
}
