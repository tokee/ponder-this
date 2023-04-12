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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Special purpose merger for files from the
 * <a href="https://github.com/ymaurer/cdx-summarize-warc-indexer">CDX-summarise</a>} project.
 */
public class DomainStatMergerOverthinking {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: DomainStatsMerger csv+");
            System.exit(2);
        }

        List<File> csvs = Arrays.stream(args).
                map(File::new).
                peek(f -> {
                    if (!f.exists()) {
                        throw new RuntimeException(new FileNotFoundException("Unable to locate '" + f + "'"));
                    }
                }).
                collect(Collectors.toList());

    }

    public DomainStatMergerOverthinking(List<File> csvs) {
        System.out.println("Loading " + csvs.size() + " files into memory and sorting");
        PriorityQueue<DomainBag> sources = csvs.stream().
                map(DomainBag::new).
                collect(Collectors.toCollection(PriorityQueue::new));
                
    }

    /**
     * Holds a list of strings, providing methods for loading from file and popping string in deterministic order.
     */
    private static class DomainBag implements Comparable<DomainBag> {
        private final Deque<String> elements;

        public DomainBag(File source)  {
            List<String> lines = null;
            try {
                lines = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read strings from '" + source + "'", e);
            }
            lines.sort(Comparator.naturalOrder()); // No need for fancy sorting as long as it's deterministic order
            elements = new ArrayDeque<>(lines);
        }

        /**
         * Removes the next element from the list and returns it.
         * @return the next element in the ordered list.
         */
        public String pop() {
            if (elements.isEmpty()) {
                throw new NoSuchElementException("The bag is empty");
            }
            return elements.removeFirst();
        }

        /**
         * Reads the next element from the list and returns it without changing the list.
         * @return the next element in the ordered list.
         */
        public String peek() {
            if (elements.isEmpty()) {
                throw new NoSuchElementException("The bag is empty");
            }
            return elements.peekFirst();
        }

        /**
         * @return true if there are no more elements.
         */
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        @Override
        public int compareTo(DomainBag other) {
            if (isEmpty()) {
                return other.isEmpty() ? 0 : 1;
            }
            if (other.isEmpty()) {
                return 1;
            }
            return peek().compareTo(other.peek());
        }
    }
}
