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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Pos {
    public int x = 0;
    public int y = 0;

    public Pos() {
    }

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static List<Pos> toPosList(int[] antis, int width, int height) {
        List<Pos> poss = new ArrayList<>(antis.length);
        for (int anti : antis) {
            poss.add(new Pos(anti % width, anti / width));
        }
        return poss;
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(int x, int y) {
        return this.x == x && this.y == y;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
