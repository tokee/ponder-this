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
 * Grows automatically. Not thread safe.
 */
public class GrowableLongArray {
    public static final int DEFAULT_BLOCK_BITS = 16; // 65K * 8 bytes = ½MB
    final int BLOCK_BITS;
    final int BLOCK_SIZE;
    final long BLOCK_MASK;
    // Max index is 2^(31+BLOCK_BITS)

    long[][] backing = new long[0][];

    public GrowableLongArray() {
        this(DEFAULT_BLOCK_BITS);
    }

    public GrowableLongArray(int blockBits) {
        BLOCK_BITS = blockBits;
        BLOCK_SIZE = 1<<BLOCK_BITS;
        BLOCK_MASK = ~((~1L)<<(BLOCK_BITS-1));
    }

    public void copyIn(long[] src, long startIndex) {
        copyIn(src, 0, startIndex, src.length);
    }
    public void copyIn(long[] src, int srcIndex, long destIndex, int length) {
        int sIndex = srcIndex;
        int dBlock = (int) (destIndex >> BLOCK_BITS);
        int dIndex = (int) (destIndex & BLOCK_MASK);
        long wordsLeft = length;

        while (wordsLeft > 0) {
            ensureBlockExistence(dBlock);
            long toCopy = BLOCK_SIZE-dIndex;
            if (toCopy > wordsLeft) {
                toCopy = wordsLeft;
            }
            System.arraycopy(src, sIndex, backing[dBlock], dIndex, (int) toCopy);
            wordsLeft -= toCopy;
            // Move to next backing block in case there are more data
            sIndex += toCopy;
            dBlock++;
            dIndex = 0;
        }
    }
    public void set(long index, long value) {
        final int block = (int) (index >> BLOCK_BITS);
        ensureBlockExistence(block);
        backing[block][(int) (index & BLOCK_MASK)] = value;
    }

    private void ensureBlockExistence(int block) {
        if (block >= backing.length) {
            long[][] newBacking = new long[block+1][];
            System.arraycopy(backing, 0, newBacking, 0, backing.length);
            backing = newBacking;
        }
        if (backing[block] == null) {
            backing[block] = new long[BLOCK_SIZE];
        }
    }

    public long get(long index) {
        final int block = (int) (index >> BLOCK_BITS);
        return block >= backing.length || backing[block] == null ? 0L :
                backing[(int) (index >> BLOCK_BITS)][(int) (index & BLOCK_MASK)];
    }

    public void copyOut(long srcIndex, long[] destination) {
        copyOut(srcIndex, destination, 0, destination.length);
    }
    public void copyOut(long srcIndex, long[] dest, int destIndex, int length) {
        int sBlock = (int) (srcIndex >> BLOCK_BITS);
        int sIndex = (int) (srcIndex & BLOCK_MASK);
        int dIndex = destIndex;
        long wordsLeft = length;

        while (wordsLeft > 0) {
            ensureBlockExistence(sBlock);
            long toCopy = BLOCK_SIZE-sIndex;
            if (toCopy > wordsLeft) {
                toCopy = wordsLeft;
            }
            System.arraycopy(backing[sBlock], sIndex, dest, dIndex, (int) toCopy);
            wordsLeft -= toCopy;
            // Move to next backing block in case there are more data
            sBlock++;
            sIndex = 0;
            dIndex += toCopy;
        }
    }

    public long size() {
        return backing.length * BLOCK_SIZE;
    }

    public void clear() {
        backing = new long[0][];
    }
}
