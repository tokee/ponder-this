package dk.ekot.misc;


import java.util.Arrays;

/**
 * Specialized Bitmap (the specialized part is the whole-bitmap shift).
 */
public class Bitmap {
    private final long[] backing;
    private final int size;
    private int cardinality = -1;

    public Bitmap(int size) {
        this.size = size;
        backing = new long[(size >>> 6) + ((size & 63) == 0 ? 0 : 1)];
    }
    public void set(int index) {
        backing[index >>> 6] |= 1L << (63-(index & 63));
        cardinality = -1;
    }
    public boolean get(int index) {
        return (backing[index >>> 6] & (1L << (63-(index & 63)))) != 0;
    }

    // Integer.MAX_VALUE means no more bits
    public int thisOrNext(int index) {
        while (index < size) {
            // Check
            if (get(index)) {
                return index;
            }

            // Skip empty words
            int word = index >>> 6;
            while (backing[word] == 0 && ++word < backing.length) {
                index += 64;
            }
            // Skip until hit or EOD
            while (!get(index) && ++index < size);
        }
        return Integer.MAX_VALUE;
    }

    public long[] getBacking() {
        return backing;
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public int cardinality() {
        if (cardinality != -1) {
            return cardinality;
        }
        cardinality = 0;
        for (int i = 0 ; i <backing.length ; i++) {
            cardinality += Long.bitCount(backing[i]);
        }
        return cardinality;
    }

    public static Bitmap or(Bitmap map1, Bitmap map2, Bitmap reuse) {
        if (reuse == null) {
            reuse = new Bitmap(map1.size);
        }
        for (int i = 0 ; i < map1.backing.length ; i++) {
            reuse.backing[i] = map1.backing[i] | map2.backing[i];
        }
        reuse.cardinality = -1;
        return reuse;
    }

    public static Bitmap and(Bitmap map1, Bitmap map2, Bitmap reuse) {
        if (reuse == null) {
            reuse = new Bitmap(map1.size);
        }
        for (int i = 0 ; i < map1.backing.length ; i++) {
            reuse.backing[i] = map1.backing[i] & map2.backing[i];
        }
        reuse.cardinality = -1;
        return reuse;
    }

    public static Bitmap xor(Bitmap map1, Bitmap map2, Bitmap reuse) {
        if (reuse == null) {
            reuse = new Bitmap(map1.size);
        }
        for (int i = 0 ; i < map1.backing.length ; i++) {
            reuse.backing[i] = map1.backing[i] ^ map2.backing[i];
        }
        reuse.cardinality = -1;
        return reuse;
    }

    public void invert() {
        for (int i = 0 ; i < backing.length ; i++) {
            backing[i] = ~backing[i];
        }
        cardinality = -1;
    }

    public void shift(int offset) {
        shift(offset, this);
    }
    /**
     * Shift the full bitmap by the specified offset.
     * @param offset negative offsets shifts left, positive shifts right.
     */
    public void shift(int offset, Bitmap destination) {
        if (offset == 0) {
            return;
        }
        cardinality = -1;
        if (offset < 0) {
            offset = -offset;
            final int lOffset = offset >>> 6;
            final int bOffset = offset & 63;
            if ((bOffset) == 0) { // Whole block shift left
                System.arraycopy(backing, lOffset, destination.backing, 0, backing.length-lOffset);
                Arrays.fill(destination.backing, backing.length - lOffset, backing.length, 0L);
            } else {
                for (int i = 0; i < backing.length; i++) {
                    final long source1 = i+lOffset >= backing.length ? 0L : backing[i+lOffset];
                    final long source2 = i+lOffset+1 >= backing.length ? 0L : backing[i+lOffset+1];
                    destination.backing[i] = source1 << bOffset | source2 >>> (64-bOffset);
                }
            }
        } else {
            final int lOffset = offset >>> 6;
            final int bOffset = offset & 63;
            if ((bOffset) == 0) { // Whole block shift right
                System.arraycopy(backing, 0, destination.backing, lOffset, backing.length-lOffset);
                Arrays.fill(destination.backing, 0, lOffset, 0L);
            } else {
                for (int i = backing.length-1; i >= 0; i--) {
                    final long source1 = i-lOffset < 0 ? 0L : backing[i-lOffset];
                    final long source2 = i-lOffset-1 < 0 ? 0L : backing[i-lOffset-1];
                    destination.backing[i] = source1 >> bOffset | source2 << (64 - bOffset);
                }
            }
        }
    }

    public int[] getIntegers() {
        final int[] result = new int[cardinality()];
        int pos = 0;
        for (int i = 0 ; i < size ; i++) {
            if (get(i)) {
                result[pos++] = i;
            }
        }
        return result;
    }

    public Bitmap makeCopy() {
        Bitmap b = new Bitmap(size);
        copy(b);
        return b;
    }
    public void copy(Bitmap destination) {
        System.arraycopy(backing, 0, destination.backing, 0, backing.length);
        destination.cardinality = cardinality;
    }
}
