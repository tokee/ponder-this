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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 *
 */
public class StrategyConservative extends StrategyBase {
    private static final Logger log = LoggerFactory.getLogger(StrategyConservative.class);

    public static final int MAX_TIME_MS = 1000;

    private int best = 0;
    private long nextReset = MAX_TIME_MS;

    public StrategyConservative(Walker walker, EListener listener) {
        super(walker, listener, true, false, true);
    }

    @Override
    public Action getAction(StrategySolverState state) {
        if (state.getLevel() > best) {
            best = state.getLevel();
            listener.localBest(Thread.currentThread().getName(), this, state.getBoard(), state);
        }
        if (state.getLevel() == 0) {
            nextReset = state.getMSTotal() + MAX_TIME_MS;
        }
        if (state.getMSTotal() > nextReset) {
            nextReset = state.getMSTotal() + MAX_TIME_MS;
            return Action.restartLevel(0);
        }
        return Action.continueLocal();
    }
}
