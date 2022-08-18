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

    private boolean gotoTop = false;
    private int best = 0;

    public StrategyConservative(Walker walker, EListener listener) {
        super(walker, listener, false, true);
    }

    @Override
    public boolean shouldProcess(EBoard board, int level, long attemptsFromTop, long attemptsTotal, long msFromTop, long msTotal) {
        if (level > best) {
            best = level;
            listener.localBest(Thread.currentThread().getName(), this, board);
        }
        if (level == 0) {
            gotoTop = false;
        }
        if (level == 100) {
            gotoTop = true;
        }

        return !gotoTop;
    }
}
