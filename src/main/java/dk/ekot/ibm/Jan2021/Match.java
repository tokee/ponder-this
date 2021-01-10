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

import java.util.List;
import java.util.Locale;

/**
 *
 */
class Match {
    public final String nature;
    public final int width;
    public final int height;
    public final int startY;
    public final int moves;
    public final long spendTimeMS;
    public final String visualGrid;
    public final List<Pos> antis;

    public Match(String nature, int width, int height, int moves, long spendTimeMS, String visualGrid, int[] antis, int startY) {
        this(nature, width, height, moves, spendTimeMS, visualGrid, Pos.toPosList(antis, width, height), startY);
    }
    public Match(String nature, int width, int height, int moves, long spendTimeMS, String visualGrid, List<Pos> antis, int startY) {
        this.nature = nature;
        this.width = width;
        this.height = height;
        this.startY = startY;
        this.moves = moves;
        this.spendTimeMS = spendTimeMS;
        this.visualGrid = visualGrid;
        this.antis = antis;
    }

    public String toString() {
        boolean matchOnStartY = false;
        for (Pos anti: antis) {
            if (anti.y == startY && anti.x == 0) {
                matchOnStartY = true;
                break;
            }
        }
        return String.format(
                Locale.ENGLISH, "%s(%3d, %3d) ms=%,7d, antis=%d: %s, startY=%d%s",
                nature, width, height, spendTimeMS, antis.size(), antis, startY, matchOnStartY ? "" : " *");

    }
}
