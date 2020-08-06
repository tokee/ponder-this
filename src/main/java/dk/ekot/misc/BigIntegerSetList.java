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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * Add-only structure for storing sets of ordered BigIntegers.
 */
public class BigIntegerSetList {

    // gt[offset] = valueSizesOffset, gt[offset+1] = valueOffset
    // valueOffset doubles as offset as well as the signals EMPTY & TRIVIAL
    final GrowableLongArray groupTracker = new GrowableLongArray();
    final GrowableByteArray valueSizes = new GrowableByteArray();   // Number of bytes making up a value
    final GrowableByteArray values = new GrowableByteArray();       // Values as byte[]-entries, stored as delta to the previous value in the set
    long size = 0;
    long valueOffset = 0;

    public enum SET_TYPE {EMPTY, TRIVIAL, DEFINED}
    static final int EMPTY = -1;
    static final int TRIVIAL = -2;

    /**
     * Add a new Set.
     * @param values one or more values.
     * @param isSorted true if the values are already sorted low-to-high (this speeds up operation).
     * @return the index of the set.
     */
    public long add(Collection<BigInteger> values, boolean isSorted) {
        // No values: Set a marker and return (very fast)
        if (values.isEmpty()) {
            size++;
            groupTracker.set(size*2, groupTracker.get(size-1)); // Same valueSizesOffset as previous
            groupTracker.set(size*2+1, EMPTY); // Special marker for empty
            return size-1;
        }

        // Sort if needed
        Collection<BigInteger> ordered;
        if (isSorted) {
            ordered = values;
        } else {
            ArrayList<BigInteger> sorter = new ArrayList<>(values);
            Collections.sort(sorter);
            ordered = sorter;
        }

        throw new UnsupportedOperationException("Not finished");
        /*
        byte valueSize = groupTracker.get(size*2);
        long valueIndex = 0;
        BigInteger previous = BigInteger.ZERO;
        for (BigInteger value: values) {
            BigInteger delta = value.subtract(previous);
            previous = value;
            byte[] bytes = delta.toByteArray();
            valueSizes[]
            valueIndex++;
        }

        size++;
        groupTracker.set(size*2, groupTracker.get(size-1)+values.size());
        groupTracker.set(size*2+1, valueOffset);



        throw new UnsupportedOperationException();*/
    }

    /**
     * @param setIndex the index for the set to query.
     * @return the number of BigIntegers in the set at the given index.
     */
    public long setSize(long setIndex) {
        throw new UnsupportedOperationException();
    }

    /**
     * Iterates the set at setIndex, calling the consumer with each BigInteger in the set.
     * @param setIndex the index for the set to query.
     * @param consumer receiver for each BigInteger in the set.
     */
    public void readSet(long setIndex, Consumer<BigInteger> consumer) {
        throw new UnsupportedOperationException();
    }
}
