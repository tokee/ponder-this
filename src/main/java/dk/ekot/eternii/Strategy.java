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
     * True if processing should continue under the given conditions.
     *
     * @param board           the board being attempted.
     * @param level           the depth aka number of positioned pieces.
     * @param attemptsFromTop number of attempts at placing pieces, measured from the top to the current level.
     * @param attemptsTotal   total attempts, including attempts in other sub-trees.
     * @param msFromTop       number of milliseconds spend, measured from the top to the current level.
     * @param msTotal         number of milliseconds spend on all processing.
     * @return true if processing of the current level should continue.
     */
    boolean shouldProcess(EBoard board, int level, long attemptsFromTop, long attemptsTotal, long msFromTop, long msTotal);
}
