package dk.ekot.misc;


/**
 * Extension of Bitmap that automatically expands if {@link #set(int)} if called on a bit that
 * is outside of the bitmap.
 */
public class GrowableBitmap extends Bitmap {
    public static final boolean DEFAULT_ENABLE_SHIFT_CACHE = false;

    public GrowableBitmap(int size) {
        this(size, DEFAULT_ENABLE_SHIFT_CACHE);
    }
    public GrowableBitmap(int size, boolean enableShiftCache) {
        super(size, enableShiftCache);
    }
    public GrowableBitmap(long[] backing, boolean enableShiftCache) {
        this (backing.length*Long.SIZE, backing, enableShiftCache);
    }
    public GrowableBitmap(int size, long[] backing, boolean enableShiftCache) {
        super(size, backing, enableShiftCache);
    }

    @Override
    public void set(int index) {
        expandIfNeeded(index);
        super.set(index);
    }

    @Override
    public boolean get(int index) {
        expandIfNeeded(index);
        return super.get(index);
    }

    @Override
    public void clear(int index) {
        expandIfNeeded(index);
        super.clear(index);
    }

    private void expandIfNeeded(int index) {
        if (index >= size()) {
            int newSize = Math.max(index+1, size() * 2);
            long[] newBacking = new long[(newSize >>> 6) +1];
            System.arraycopy(backing, 0, newBacking, 0, backing.length);
            backing = newBacking;
            size = newSize;
            invalidateCardinality();
        }
    }
}
