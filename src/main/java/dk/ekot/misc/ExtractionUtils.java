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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extraction of subsets of streams or collections.
 *
 * Taken from kb-utils (also Apache 2)
 */
public class ExtractionUtils {

    /**
     * Returns a random element from a stream. This involves collecting all elements temporarily.
     *
     * Usage sample
     * <pre>
     *   int sample = List.of(1, 2, 3, 4, 5).
     *                  collect(ExtractionUtils.sample()).
     *                  orElseThrow();
     * </pre>
     *
     * Note: If sample is called extensively, consider creating an instance of the collector and reusing it.
     * @return a random value.
     */
    public static <V> Collector<V, List<V>, Optional<V>> sample() {
        return sample(new Random());
    }

    /**
     * Pick a random element from a stream. This involves collecting all elements temporarily.
     *
     * Usage sample
     * <pre>
     *   Random r = new Random();
     *   int sample = List.of(1, 2, 3, 4, 5).
     *                  collect(ExtractionUtils.sample(r)).
     *                  orElseThrow();
     * </pre>
     *
     * Note: If sample is called extensively, consider creating an instance of the collector and reusing it.
     * @param random generator used for selecting the random value.
     * @return a random value.
     */
    public static <V> Collector<V, List<V>, Optional<V>> sample(final Random random) {
        return new Collector<V, List<V>, Optional<V>>() {
            @Override
            public Supplier<List<V>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<V>, V> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<V>> combiner() {
                return (l1, l2) -> {
                    List<V> combined = new ArrayList<>();
                    combined.addAll(l1);
                    combined.addAll(l2);
                    return combined;
                };
            }

            @Override
            public Function<List<V>, Optional<V>> finisher() {
                return l -> l.isEmpty() ? Optional.empty() : Optional.of(l.get(random.nextInt(l.size())));
            }

            @Override
            public Set<Characteristics> characteristics() {
                return new HashSet<>(Collections.singletonList(Characteristics.UNORDERED));
            }
        };
    }

    /**
     * Pick a list of random element from a stream. This involves collecting all elements temporarily.
     * The original order of the elements is preserved.
     *
     * Usage sample
     * <pre>
     *   List<Integer> samples = List.of(1, 2, 3, 4, 5).
     *                  collect(ExtractionUtils.samples(2));
     * </pre>
     *
     * Note: If sample is called extensively, consider creating an instance of the collector and reusing it.
     * @param maxSampleSize the number of random elements to extract.
     * @return a random value.
     */
    public static <V> Collector<V, List<V>, List<V>> samples(final int maxSampleSize) {
        return samples(maxSampleSize, new Random());
    }
    
    /**
     * Pick a list of random elements from a stream. This involves collecting all elements temporarily.
     * The original order of the elements is preserved.
     *
     * Usage sample
     * <pre>
     *   Random r = new Random();
     *   List<Integer> samples = List.of(1, 2, 3, 4, 5).
     *                  collect(ExtractionUtils.samples(2, r));
     * </pre>
     *
     * Note: If sample is called extensively, consider creating an instance of the collector and reusing it.
     * @param maxSampleSize the number of random elements to extract.
     * @param random generator used for selecting random values.
     * @return a random value.
     */
    public static <V> Collector<V, List<V>, List<V>> samples(final int maxSampleSize, final Random random) {
        return new Collector<V, List<V>, List<V>>() {
            @Override
            public Supplier<List<V>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<V>, V> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<V>> combiner() {
                return (values1, values2) -> {
                    List<V> combined = new ArrayList<>();
                    combined.addAll(values1);
                    combined.addAll(values2);
                    return combined;
                };
            }

            @Override
            public Function<List<V>, List<V>> finisher() {
                return allValues -> {
                    if (allValues.isEmpty()) {
                        return allValues;
                    }
                    return samples(allValues, maxSampleSize, random);
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return new HashSet<>(Collections.singletonList(Characteristics.UNORDERED));
            }
        };
    }

    /**
     * Pick a list of random elements from the given list.
     *
     * The sampler does not change the given collection. It preserves the order of elements:
     * {@code sample({5, 1, 2, 3}, 2)} might give {@code {1, 3}} but never {@code {3, 1}}.
     * @param values the source to sample.
     * @param maxSampleSize the number of random elements to extract.
     *                      If the number exceeds the size of the input, it will be rounded down.
     * @return a sample of elements from the given values.
     */
    public static <V> List<V> samples(Collection<V> values, int maxSampleSize) {
        return samples(values, maxSampleSize, new Random());
    }

    /**
     * Pick a list of random elements from the given input.
     *
     * The sampler does not change the given collection. It preserves the order of elements:
     * {@code sample({5, 1, 2, 3}, 2, new Random())} might give {1, 3} but never {3, 1}.
     * @param values the source to sample.
     * @param maxSampleSize the number of random elements to extract.
     *                      If the number exceeds the size of the input, it will be rounded down.
     * @param random generator used for selecting random values.
     * @return a sample of elements from the given values.
     */
    public static <V> List<V> samples(Collection<V> values, int maxSampleSize, Random random) {

        //  A bit convoluted in order to uphold the contract of preserving original order
        // 1) Create an array of integers 0, 1, 2, ... n
        // 2) Shuffle the array
        // 3) Sort the first maxSampleSize array elements in integer order (low->high)
        // 4) Extract the samples from the values using the integers from the array as indices

        final int[] sampleIndices = new int[values.size()];
        for (int i = 0 ; i < sampleIndices.length ; i++) {
            sampleIndices[i] = i;
        }
        // Array shuffling algorithm taken from the Collections.shuffle method
        for (int i = sampleIndices.length; i > 1; i--) {
            int p = random.nextInt(i);
            int tmp = sampleIndices[i-1];
            sampleIndices[i-1] = sampleIndices[p];
            sampleIndices[p] = tmp;
        }

        final int realSampleSize = Math.min(maxSampleSize, values.size());
        Arrays.sort(sampleIndices, 0, realSampleSize); // Preserve original order of values

        // Iterate all input values, keeping those whose index is in sampleIndices
        AtomicInteger siIndex = new AtomicInteger(0);
        AtomicInteger valIndex = new AtomicInteger(0);
        // Input is Collection that does not have random access, so we need to iterate
        return values.stream().
                filter(val -> {
                    if (valIndex.getAndIncrement() == sampleIndices[siIndex.get()]) {
                        siIndex.incrementAndGet(); // Move to next sample index
                        return true;
                    }
                    return false;
                }).
                limit(realSampleSize).
                collect(Collectors.toList());
    }

    /**
     * Extract all minimum values from the input, where the minima are determined by natural order:
     * {@code getMin([4, 5, 1, 3, 2, 5, 5, 1]) -> [1, 1]}.
     *
     * Similar to {@link java.util.Collections#min(Collection)} with the change that all values with equal compare
     * results are returned.
     * @param values a finite stream of comparable values.
     * @return all minimum values from the input, by value order.
     * @param <T> any Comparable.
     */
    public static <T extends Comparable<T>> List<T> minima(Stream<T> values) {
        return minima(values, Comparable::compareTo);
    }

    /**
     * Extract all minimum values from the input, where the minima are determined by natural order:
     * {@code getMin([4, 5, 1, 3, 2, 5, 5, 1]) -> [1, 1]}.
     *
     * Similar to {@link java.util.Collections#min(Collection)} with the change that all values with equal compare
     * results are returned.
     * @param values a collection of comparable values.
     * @return all minimum values from the input, by value order.
     * @param <T> any Comparable.
     */
    public static <T extends Comparable<T>> List<T> minima(Collection<T> values) {
        return minima(values.stream(), Comparable::compareTo);
    }

    /**
     * Extract all maximum values from the input, where the maxima are determined by natural order:
     * {@code getMax([4, 5, 1, 3, 2, 5, 5, 1]) -> [5, 5, 5]}.
     *
     * Similar to {@link java.util.Collections#max(Collection)} with the change that all values with equal compare
     * results are returned.
     * @param values a finite stream of comparable values.
     * @return all maximum values from the input, by value order.
     * @param <T> any Comparable.
     */
    public static <T extends Comparable<T>> List<T> maxima(Stream<T> values) {
        return minima(values, (v1, v2) -> (-1) * v1.compareTo(v2));
    }

    /**
     * Extract all maximum values from the input, where the maxima are determined by natural order:
     * {@code getMax([4, 5, 1, 3, 2, 5, 5, 1]) -> [5, 5, 5]}.
     *
     * Similar to {@link java.util.Collections#max(Collection)} with the change that all values with equal compare
     * results are returned.
     * @param values a collection of comparable values.
     * @return all maximum values from the input, by value order.
     * @param <T> any Comparable.
     */
    public static <T extends Comparable<T>> List<T> maxima(Collection<T> values) {
        return minima(values.stream(), (v1, v2) -> (-1) * v1.compareTo(v2));
    }

    /**
     * Extract all minimum values from the input, similar to {@link #minima(Stream)} but with an explicit comparator.
     *
     * Similar to {@link java.util.Collections#min(Collection, Comparator)} with the change that all values with equal
     * compare results are returned.
     * @param values any finite stream of values.
     * @param comparator determines order of values.
     * @return all minimum values from the input, by comparator order.
     * @param <T> no explicit constraints.
     */
    public static <T> List<T> minima(Stream<T> values, Comparator<T> comparator) {
        List<T> result = new ArrayList<>();
        values.forEach(v -> {
            int compareValue = result.isEmpty() ? 0 : comparator.compare(v, result.get(0));
            if (compareValue == 0) {
                result.add(v);
            } else if (compareValue < 0) {
                result.clear();
                result.add(v);
            }
        });
        return result;
    }

    /**
     * Extract all minimum values from the input, similar to {@link #minima(Collection)} but with an explicit comparator.
     *
     * Similar to {@link java.util.Collections#min(Collection, Comparator)} with the change that all values with equal
     * compare results are returned.
     * @param values a collection of values.
     * @param comparator determines order of values.
     * @return all minimum values from the input, by comparator order.
     * @param <T> no explicit constraints.
     */
    public static <T> List<T> minima(Collection<T> values, Comparator<T> comparator) {
        return minima(values.stream(), comparator);
    }

    /**
     * Extract all maximum values from the input, similar to {@link #maxima(Stream)} but with an explicit comparator.
     *
     * Similar to {@link java.util.Collections#max(Collection, Comparator)} with the change that all values with equal
     * compare results are returned.
     * @param values any finite stream of values.
     * @param comparator determines order of values.
     * @return all maximum values from the input, by comparator order.
     * @param <T> no explicit constraints.
     */
    public static <T> List<T> maxima(Stream<T> values, Comparator<T> comparator) {
        return minima(values, comparator.reversed());
    }

    /**
     * Extract all maximum values from the input, similar to {@link #maxima(Collection)} but with an explicit comparator.
     *
     * Similar to {@link java.util.Collections#max(Collection, Comparator)} with the change that all values with equal
     * compare results are returned.
     * @param values a collection of values.
     * @param comparator determines order of values.
     * @return all maximum values from the input, by comparator order.
     * @param <T> no explicit constraints.
     */
    public static <T> List<T> maxima(Collection<T> values, Comparator<T> comparator) {
        return minima(values.stream(), comparator.reversed());
    }

}
