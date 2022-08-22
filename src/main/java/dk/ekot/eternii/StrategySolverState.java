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
public class StrategySolverState {
    private final long startTime = System.currentTimeMillis();
    public EBoard board;
    public int level = 0;
    public long attemptsFromTop = 0;
    public long attemptsTotal = 0;
    public long msFromTop = 0;
    public double possibilities = 1.0;

    public int bestLevel = 0;
    public int bestMarked = 0;
    public String bestSolution = "";

    public StrategySolverState(EBoard board) {
        this.board = board;
    }

    public long getMSTotal() {
        return System.currentTimeMillis()-startTime;
    }

    public EBoard getBoard() {
        return board;
    }

    public int getLevel() {
        return level;
    }

    public long getAttemptsFromTop() {
        return attemptsFromTop;
    }

    public long getAttemptsTotal() {
        return attemptsTotal;
    }

    public long getMsFromTop() {
        return msFromTop;
    }

    public void setLevel(int level) {
        if (bestLevel < level) {
            bestLevel = level;
            bestMarked = board.getFilledCount();
            bestSolution = board.getDisplayURL();
        }
        this.level = level;
    }

    public int getBestMarked() {
        return bestMarked;
    }

    public void setAttemptsFromTop(long attemptsFromTop) {
        this.attemptsFromTop = attemptsFromTop;
    }

    public void setAttemptsTotal(long attemptsTotal) {
        this.attemptsTotal = attemptsTotal;
    }

    public void incAttemptsTotal() {
        attemptsTotal++;
    }

    public void setMsFromTop(long msFromTop) {
        this.msFromTop = msFromTop;
    }

    public double getPossibilities() {
        return possibilities;
    }

    public void setPossibilities(double possibilities) {
        this.possibilities = possibilities;
    }

    public long getTotalAttemptsPerMS() {
        return attemptsTotal == 0 ? 0 : attemptsTotal/getMSTotal();
    }
}
