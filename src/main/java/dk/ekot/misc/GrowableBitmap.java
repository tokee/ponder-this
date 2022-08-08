package dk.ekot.misc;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
        if (index > size()) {
            int newSize = Math.max(index, size()*2);
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
