package dk.ekot.misc;


/**
 * Specialized Bitmap where the accessors works on the intersection of two long-arrays but setters
 * only affects one.
 *
 * Accessors work relative to the base Bitmap.
 */
public class IntersectionBitmap extends Bitmap {
    final Bitmap baseBitmap;
    final long[] base;

    final int startBlock; // Inclusive
    final int endBlock;   // Exclusive
    final int startIndex;   // Inclusive: startBlock << 6
    final int endIndex;     // Exclusive: endBlock << 6

    long lastChangeCounter = -1;

    public IntersectionBitmap(Bitmap bitmap) {
        this(bitmap, 0, bitmap.backing.length);
    }

    public IntersectionBitmap(Bitmap bitmap, int startBlock, int endBlock) {
        super((endBlock-startBlock) << 6, new long[endBlock-startBlock], false);
        baseBitmap = bitmap;
        this.base = bitmap.backing;
        this.startBlock = startBlock;
        this.endBlock = endBlock;
        startIndex = startBlock << 6;
        endIndex = endBlock << 6;
    }

    @Override
    public boolean get(int index) {
        if (index < startIndex || index >= endIndex) {
            return false;
        }
        return unsafeGet (index);
    }

    /**
     * Getter that assumes that {@code startIndex <= index} and {@code index < endIndex}.
     */
    public boolean unsafeGet(int index) {
        return (backing[(index - startIndex) >>> 6] & base[index >>> 6] & (1L << (63 - (index & 63)))) != 0;
    }

    @Override
    public void set(int index) {
        if (index < startIndex || index >= endIndex) {
            throw new IllegalArgumentException(
                    "Invalid set: startPos=" + startIndex + ", index=" + index + ", endPos=" + endIndex);
        }
        backing[(index - startIndex) >>> 6] |= 1L << (63 - (index & 63));
        invalidateCardinality();
    }
    @Override
    public void clear(int index) {
        if (index < startIndex || index >= endIndex) {
            throw new IllegalArgumentException(
                    "Invalid clear: startPos=" + startIndex + ", index=" + index + ", endPos=" + endIndex);
        }
        backing[(index - startIndex) >>> 6] &= ~(1L << (63 - (index & 63)));
        invalidateCardinality();
    }

    // Integer.MAX_VALUE means no more bits
    @Override
    public int thisOrNext(int index) {
        if (index < startIndex) {
            index = startIndex;
        }
        if (index >= endIndex) {
            return Integer.MAX_VALUE;
        }

        // Check current word
        if ((index & 0x63) != 0) { // Current word check
            final int nextAligned = ((index >>> 6) + 1) << 6;
            while (index < nextAligned) {
                if (unsafeGet(index++)) {
                    return index - 1;
                }
            }
        }

        // Skip empty words
        int word = index >>> 6;
        while (word < endBlock && (backing[word-startBlock] & base[word]) == 0) {
            word++;
            index += 64;
        }

        // Look in-word until hit or EOD
        while (index < endIndex && !unsafeGet(index)) {
            index++;
        }

        return index < endIndex ? index : Integer.MAX_VALUE;
    }

    public long[] getBase() {
        return base;
    }

    // TODO: Make a better check for cardinality invalidation
    @Override
    public int cardinality() {
        if (cardinality != -1  && lastChangeCounter == baseBitmap.changeCounter) {
            return cardinality;
        }

        lastChangeCounter = baseBitmap.changeCounter;

        cardinality = 0;
        for (int i = 0 ; i <backing.length ; i++) {
            cardinality += Long.bitCount(backing[i] & base[i+startBlock]);
        }
        return cardinality;
    }

    // Imprecise cardinality counter. Counts at least up to limit (if possible)
    @Override
    public int cardinalityStopAt(int limit) {
        throw new UnsupportedOperationException("Not supported by IntersectionBitmap");
    }

    @Override
    public void invert() {
        throw new UnsupportedOperationException("Not possible to implement for IntersectionBitmaps");
    }

    @Override
    public void shift(int offset) {
        throw new UnsupportedOperationException("Not possible to implement for IntersectionBitmaps");
    }
    /**
     * Shift the full bitmap by the specified offset.
     * @param offset negative offsets shifts left, positive shifts right.
     */
    public void shift(int offset, IntersectionBitmap destination) {
        throw new UnsupportedOperationException("Not possible to implement for IntersectionBitmaps");
    }

    public boolean equalBits(IntersectionBitmap other) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public IntersectionBitmap makeCopy() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    @Override
    public IntersectionBitmap makeCopy(boolean enableShiftcache) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void copy(IntersectionBitmap destination) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public long countIntersectingBits(IntersectionBitmap other) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long[] getBackingCopy() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
