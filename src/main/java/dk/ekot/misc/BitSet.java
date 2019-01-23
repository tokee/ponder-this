package dk.ekot.misc;

import java.util.*;

/**
 * Allows for shift & and operations on a logical set of bits.
 */
public interface BitSet extends Iterable<Integer> {

    BitSet set(int index);
    boolean get(int index);
    BitSet clear();
    BitSet copy();
    BitSet shallowCopy();
    BitSet shift(int offset);
    @Override
    Iterator<Integer> iterator();

    default BitSet set(int... index) {
        for (int i: index) {
            set(i);
        }
        return this;
    }


    static BitSet and(BitSet... sets) {
        return new Multi(Single.MODE.and, sets); // TODO: Optimize if consisting of multi with and
    }

    static BitSet or(BitSet... sets) {
        return new Multi(Single.MODE.or, sets);
    }

    enum MODE {and, or}

    public class Single implements BitSet {

        private LinkedHashSet<Integer> bits = new LinkedHashSet<>();
        private int offset = 0;
        private int lastSet = -1;

        public Single() { }

        @Override
        public BitSet set(int index) {
            if (index < lastSet) {
                throw new IllegalStateException(
                        "Bits must be set in increasing order. Last bit was " + bits + ", attempted bit was " + index);
            }
            lastSet = index;
            bits.add(index+offset);
            return this;
        }

        @Override
        public boolean get(int index) {
            return index >= 0 && index-offset >= 0 && bits.contains(index-offset);
        }

        @Override
        public BitSet clear() {
            bits.clear();
            offset = 0;
            return this;
        }

        @Override
        public BitSet copy() {
            Single result = new Single();
            result.bits.addAll(bits);
            result.offset = offset;
            return result;
        }

        @Override
        public BitSet shallowCopy() {
            Single result = new Single();
            result.bits = bits;;
            result.offset = offset;
            return result;
        }

        @Override
        public BitSet shift(int offset) {
            this.offset += offset;
            return this;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new OffsetIterator(bits.iterator(), offset);
        }
    }
    static class OffsetIterator implements Iterator<Integer> {
        final Iterator<Integer> backing;
        final int offset;
        Integer value;

        public OffsetIterator(Iterator<Integer> iterator, int offset) {
            this.backing = iterator;
            this.offset = offset;
            iterateToNext();
        }

        private void iterateToNext() {
            value = null;
            while (backing.hasNext()) {
                value = backing.next()+offset;
                if (value >= 0) {
                    break;
                }
                value = null;
            }

        }

        @Override
        public boolean hasNext() {
            return value != null;
        }

        @Override
        public Integer next() {
            Integer result = value;
            iterateToNext();
            return result;
        }
    }


    public class Multi implements BitSet {
        private final Single.MODE mode;

        private List<BitSet> backing = new ArrayList<>();
        private int offset = 0;

        public Multi(Single.MODE mode, BitSet... sets) {
            this.mode = mode;
            backing.addAll(Arrays.asList(sets));
        }

        @Override
        public BitSet set(int index) {
            throw new UnsupportedOperationException("Set does not work for aggregations");
        }

        @Override
        public boolean get(int index) {
            if (index < 0 || index-offset < 0) {
                return false;
            }
            switch (mode) {
                case and: {
                    for (BitSet bs: backing) {
                        if (!bs.get(index-offset)) {
                            return false;
                        }
                    }
                    return true;
                }
                case or: {
                    for (BitSet bs: backing) {
                        if (bs.get(index-offset)) {
                            return true;
                        }
                    }
                    return false;
                }
                default: throw new UnsupportedOperationException("The MODE " + mode + " is unsupported");
            }
        }

        @Override
        public BitSet clear() {
            backing.clear();
            offset = 0;
            return this;
        }

        @Override
        public BitSet copy() {
            Multi result = new Multi(mode);
            result.backing.addAll(backing);
            result.offset = offset;
            return result;
        }

        @Override
        public BitSet shallowCopy() {
            return copy();
        }

        @Override
        public BitSet shift(int offset) {
            this.offset += offset;
            return this;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new MergingOrIterator(this);
        }
    }

    static class MergingAndIterator implements Iterator<Integer> {
        private final List<OrderedIterator> iterators;
        private Integer current = null;

        public MergingAndIterator(Multi multi) {
            this(getIterators(multi.backing));
            if (multi.mode != MODE.and) {
                throw new IllegalArgumentException("Only mode AND is accepted, but got " + multi.mode);
            }
        }

        public MergingAndIterator(List<Iterator<Integer>> iterators) {
            this.iterators = new ArrayList<>(iterators.size());
            for (Iterator<Integer> iterator: iterators) {
                OrderedIterator oi = new OrderedIterator(iterator);
                if (oi.hasNext()) {
                    this.iterators.add(oi);
                }
            }
            next();
        }

        @Override
        public boolean hasNext() {
            return !iterators.isEmpty();
        }

        @Override
        public Integer next() {
            Integer result = current;
            ensureCurrent();
            if (!iterators.isEmpty()) {
                current = iterators.get(0).peekValue();
            }
            return result;
        }

        public void ensureCurrent() {
            // Iterate ahead to at least current+1
            int previousHighest = -1;
            int highest = current == null ? 0 : current+1;
            while (!iterators.isEmpty() && previousHighest != highest) {
                previousHighest = highest;
                for (int i = iterators.size() - 1; i >= 0; i--) {
                    OrderedIterator oi = iterators.get(i);
                    if (oi.hasNext(highest)) {
                        if (highest < oi.peekValue()) {
                            highest = oi.peekValue();
                        }
                    } else {
                        iterators.remove(i);
                    }
                }
            }
        }
    }

    static class MergingOrIterator implements Iterator<Integer> {
        private final PriorityQueue<OrderedIterator> iterators;

        public MergingOrIterator(Multi multi) {
            this(getIterators(multi.backing));
            if (multi.mode != MODE.or) {
                throw new IllegalArgumentException("Only mode OR is accepted, but got " + multi.mode);
            }
        }

        public MergingOrIterator(List<Iterator<Integer>> iterators) {
            this.iterators = new PriorityQueue<>(iterators.size());
            for (Iterator<Integer> iterator: iterators) {
                OrderedIterator oi = new OrderedIterator(iterator);
                if (oi.hasNext()) {
                    this.iterators.add(oi);
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !iterators.isEmpty();
        }

        @Override
        public Integer next() {
            OrderedIterator oi = iterators.remove();
            Integer value = oi.popValue();
            if (oi.hasNext()) {
                iterators.add(oi);
            }
            return value;
        }
    }

    static List<Iterator<Integer>> getIterators(List<BitSet> bitSets) {
        List<Iterator<Integer>> iterators = new ArrayList<>(bitSets.size());
        for (BitSet bits: bitSets) {
            iterators.add(bits.iterator());
        }
        return iterators;
    }

    static class OrderedIterator implements Comparable<OrderedIterator> {
        private final Iterator<Integer> backing;
        private Integer value;
        public OrderedIterator(Iterator<Integer> iterator) {
            this.backing = iterator;
            value = iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public int compareTo(OrderedIterator o) {
            if (value == null) {
                return o.value != null ? -1 : 0;
            }
            if (o.value == null) {
                return 1;
            }
            return value.compareTo(o.value);
        }

        public Integer popValue() {
            Integer result = value;
            if (value != null) {
                value = backing.hasNext() ? backing.next() : null;
            }
            return result;
        }

        public Integer peekValue() {
            return value;
        }

        public boolean hasNext() {
            return value != null;
        }

        public boolean hasNext(int atLeast) {
            while (hasNext() && value < atLeast) {
                popValue();
            }
            return hasNext();
        }
    }
}
