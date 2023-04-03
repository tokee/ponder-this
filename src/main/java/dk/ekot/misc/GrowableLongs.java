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
public class GrowableLongs {
    long[] longs;
    int pos = 0;

    public GrowableLongs() {
        longs = new long[100];
    }

    public GrowableLongs(int initialSize) {
        longs = new long[initialSize];
    }

    public void add(long v) {
        if (pos == longs.length) {
            long[] newLongs = new long[longs.length * 2];
            System.arraycopy(longs, 0, newLongs, 0, longs.length);
            longs = newLongs;
        }
        longs[pos++] = v;
    }

    public long get(int index) {
        return longs[index];
    }

    public int size() {
        return pos;
    }

    /**
     * @return truncated version.
     */
    public long[] copyLongs() {
        long[] result = new long[pos];
        System.arraycopy(longs, 0, result, 0, pos);
        return result;
    }

    public long[] rawLongs() {
        return longs;
    }

    /**
     * @return a deep copy of this Growablelongs with the internal structure trimmed down to {@link #size()}.
     */
    public GrowableLongs trimCopy() {
        GrowableLongs copy = new GrowableLongs(pos);
        System.arraycopy(longs, 0, copy.longs, 0, pos);
        copy.pos = pos;
        return copy;
    }
}
