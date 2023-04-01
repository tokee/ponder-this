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
package dk.ekot.misc;

/**
 *
 */
public class GrowableInts {
    int[] ints = new int[100];
    int pos = 0;

    public GrowableInts() {
        ints = new int[100];
    }

    public GrowableInts(int initialSize) {
        ints = new int[initialSize];
    }

    public void add(int v) {
        if (pos == ints.length) {
            int[] newInts = new int[ints.length * 2];
            System.arraycopy(ints, 0, newInts, 0, ints.length);
            ints = newInts;
        }
        ints[pos++] = v;
    }

    public int get(int index) {
        return ints[index];
    }

    public int size() {
        return pos;
    }

    /**
     * @return truncated version.
     */
    public int[] copyInts() {
        int[] result = new int[pos];
        System.arraycopy(ints, 0, result, 0, pos);
        return result;
    }
    public int[] rawInts() {
        return ints;
    }

    /**
     * @return a deep copy of this GrowableInts with the internal structure trimmed down to {@link #size()}.
     */
    public GrowableInts copy() {
        GrowableInts copy = new GrowableInts(pos);
        System.arraycopy(ints, 0, copy.ints, 0, pos);
        return copy;
    }
}
