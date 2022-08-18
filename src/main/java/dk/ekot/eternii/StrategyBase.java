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
public class StrategyBase implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(StrategyBase.class);
    protected Walker walker;
    protected EListener listener;
    protected boolean acceptsUnresolvable;
    protected boolean onlySingleField;

    private List<String> eLog = new ArrayList<>();

    public StrategyBase(Walker walker, EListener listener, boolean acceptsUnresolvable, boolean onlySingleField) {
        this.walker = walker;
        this.listener = listener;
        this.acceptsUnresolvable = acceptsUnresolvable;
        this.onlySingleField = onlySingleField;
    }

    @Override
    public void addToLog(String logLine) {
        eLog.add(logLine);
    }

    @Override
    public boolean acceptsUnresolvable() {
        return acceptsUnresolvable;
    }

    @Override
    public boolean onlySingleField() {
        return onlySingleField;
    }

    @Override
    public Walker getWalker() {
        return walker;
    }

    @Override
    public boolean shouldProcess(
            EBoard board, int level, long attemptsFromTop, long attemptsTotal, long msFromTop, long msTotal) {
        return true;
    }
}
