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
    public final List<Integer> startYs;
    public final int moves;
    public final long spendTimeMS;
    public final String visualGrid;
    public final List<Pos> antis;

    public Match(String nature, int width, int height, int moves, long spendTimeMS, String visualGrid, int[] antis, List<Integer> startYs) {
        this(nature, width, height, moves, spendTimeMS, visualGrid, Pos.toPosList(antis, width, height), startYs);
    }
    public Match(String nature, int width, int height, int moves, long spendTimeMS, String visualGrid, List<Pos> antis, List<Integer> startYs) {
        this.nature = nature;
        this.width = width;
        this.height = height;
        this.startYs = startYs;
        this.moves = moves;
        this.spendTimeMS = spendTimeMS;
        this.visualGrid = visualGrid;
        this.antis = antis;
    }

    public String toString() {
        String startYStatus = " *";
        out:
        if (startYs.size() > 0) {
            for (int i = 0 ; i < antis.size() ; i++) {
               Pos anti = antis.get(i);
                for (int startY: startYs) {
                    if (anti.y == startY && anti.x == 0) {
                        startYStatus = i == 0 ? "" : " -";
                        break out;
                    }
                }
            }
        }
        return String.format(
                Locale.ENGLISH, "%s(%3d, %3d) ms=%,10d, antis=%d: %s, startYs=%s%s",
                nature, width, height, spendTimeMS, antis.size(), antis, startYs, startYStatus);

    }
}
