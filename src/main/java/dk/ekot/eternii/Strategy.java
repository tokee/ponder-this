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

/**
 *
 */
public interface Strategy {

    class Action {
        public enum COMMAND { quit, continueLocal, continueLevel, restartLevel}
        public final COMMAND command;
        public final int level;

        private Action(COMMAND command, int level) {
            this.command = command;
            this.level = level;
        }

        public static Action quit() {
            return new Action(COMMAND.quit, -1);
        }
        public static Action continueLocal() {
            return new Action(COMMAND.continueLocal, -1);
        }
        public static Action continueLevel(int level) {
            return new Action(COMMAND.continueLevel, level);
        }
        public static Action restartLevel(int level) {
            return new Action(COMMAND.restartLevel, level);
        }
    }

    Action state = Action.continueLocal();

    /**
     * If true, processing of level 0 is looped, meaning processing never ends.
     * This only makes sense with randomised processing.
     * @return true if level should be restarted when all valid fields and pieces has been tried.
     */
    boolean loopLevelZero();

    /**
     * The log typically contains a seed in the case of a random walker and information about the walker and
     * solver properties. This log is
     *
     * @param logLine log info relevant for replay or understanding.
     */
    void addToLog(String logLine);

    /**
     * If true, a piece is accepted even if the Solver is able to determine that the complete puzzle can never be
     * solved from the given position. Set this to false to search for "most pieces placed".
     *
     * @return true if the Solver should continue even if the puzzle cannot be solved completely.
     */
    boolean acceptsUnresolvable();

    /**
     * If true, the Solver should only request a single field from the Walker.
     * If false, the solver should request a list of prioritized fields from the Walker.
     *
     * @return true if the solver should only try a a single field before giving up at the current level.
     */
    boolean onlySingleField();

    /**
     * `getWalker()` is called <strong>every time</strong> the solver needs to call a walker.
     * This makes it possible to change Walker on the fly.
     *
     * @return the walker to use to resolve the next field(s) and pieces to try.
     */
    Walker getWalker();

    /**
     * Determine what to do in the current situation.
     *
     * @param state the state of processing.
     * @return the Action to perform.
     */
    // TODO: Instead of using boolean in shouldProcess, it should be possible to give commands:
    // quit: Stop processing in the current thread
    // continue: Continue with current processing
    // continueLevel(int level): Go up to the given level and continue processing there
    // restartLevel(int level):  Go up to the given level and restart processing there (only makes sense with Random)
    Action getAction(StrategySolverState state);

}
