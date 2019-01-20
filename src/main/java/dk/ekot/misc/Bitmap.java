/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.
 */
package dk.ekot.misc;

import dk.ekot.ibm.Jan2019;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Arrays;

public class Bitmap {
    private final long[] backing;
    private final int size;

    public Bitmap(int size) {
        this.size = size;
        backing = new long[(size >>> 6) + ((size & 63) == 0 ? 0 : 1)];
    }
    public void set(int index) {
        backing[index >>> 6] |= 1L << (63-(index & 63));
    }
    public boolean get(int index) {
        return (backing[index >>> 6] & (1L << (63-(index & 63)))) != 0;
    }
    public long[] getBacking() {
        return backing;
    }

    public int size() {
        return size;
    }

    public static Bitmap or(Bitmap map1, Bitmap map2, Bitmap reuse) {
        throw new UnsupportedOperationException("Not imp+lemented yet");
    }

    public static Bitmap and(Bitmap map1, Bitmap map2, Bitmap reuse) {
        throw new UnsupportedOperationException("Not imp+lemented yet");
    }

    /**
     * Shift the full bitmap by the specified offset.
     * @param offset negative offsets shifts left, positive shifts right.
     */
    public void shift(int offset) {
        if (offset == 0) {
            return;
        }
        if (offset < 0) {
            offset = -offset;
            final int lOffset = offset >>> 6;
            final int bOffset = offset & 63;
            if ((bOffset) == 0) { // Whole block shift left
                System.arraycopy(backing, lOffset, backing, 0, backing.length-lOffset);
                Arrays.fill(backing, backing.length-lOffset, backing.length, 0L);
            } else {
                for (int i = 0; i < backing.length; i++) {
                    final long source1 = i+lOffset >= backing.length ? 0L : backing[i+lOffset];
                    final long source2 = i+lOffset+1 >= backing.length ? 0L : backing[i+lOffset+1];
                    backing[i] = source1 << bOffset | source2 >>> (64-bOffset);
                }
            }
        } else {
            final int lOffset = offset >>> 6;
            final int bOffset = offset & 63;
            if ((bOffset) == 0) { // Whole block shift right
                System.arraycopy(backing, 0, backing, lOffset, backing.length-lOffset);
                Arrays.fill(backing, 0, lOffset, 0L);
            } else {
                for (int i = backing.length-1; i >= 0; i--) {
                    final long source1 = i-lOffset < 0 ? 0L : backing[i-lOffset];
                    final long source2 = i-lOffset-1 < 0 ? 0L : backing[i-lOffset-1];
                    backing[i] = source1 >> bOffset | source2 << (64 - bOffset);
                }
            }
        }
    }

}
