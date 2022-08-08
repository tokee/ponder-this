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
package dk.ekot.eternii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 *  Keeps track of pieces.
 */
public class PieceTracker {
    private static final Logger log = LoggerFactory.getLogger(PieceTracker.class);

    public static final int EF = 30; // Must be > max edge type id (int)

    // The sets holds pieces
    
    // Edge -> pieces
    private final PieceHolderRadix one = new PieceHolderRadix(32);
    // [edge1, edge2 (clockwise immediately after edge1)] -> pieces
    private final PieceHolderRadix two = new PieceHolderRadix(32*32);
    // [edge1, edge2 (clockwise immediately after edge1), edge3 (clockwise immediately after edge2)] -> pieces
    private final PieceHolderRadix three = new PieceHolderRadix(32*32*32); // TODO: 32K entries. Might be too much?
    // [edge1, edge2 (at the other side of edge)] -> pieces. One 180 degree rotation
    private final PieceHolderRadix opposing = new PieceHolderRadix(32*32);
    // [edge1, edge2, edge3, edge4] -> pieces. All rotations
    private final PieceHolder four = new PieceHolder(); // 1M entries is definitely too much for radix

    private final Set<Integer> all = new HashSet<>();

    private final EPieces pieces;

    /**
     * If true, pieces with 2 or 3 edges og the same color are only registered once in {@link #one}.
     * If false, those pieces are registered 2 or 3 times.
     */
    private final boolean REMOVE_DUPLICATES = true;

    public PieceTracker(EPieces pieces) {
        this.pieces = pieces;
    }

    public void add(int piece) {
//        System.out.println("PieceTracker.remove(" + piece + ")");
        process(piece, set -> set.add(piece));
        if (!all.add(piece)) {
            throw new IllegalStateException(
                    "Add piece " + piece + " called, but the piece was already present in " + all);
        }
    }
    public boolean remove(int piece) {
//        System.out.println("PieceTracker.remove(" + piece + ")");
        process(piece, set -> set.remove(piece));
        if (!all.remove(piece)) {
            throw new IllegalStateException("Remove piece " + piece + " called, but the piece was not present");
        }
        return true;
    }

    public int size() {
        return all.size();
    }

    public boolean isEmpty() {
        return all.isEmpty();
    }

    private void process(int piece, Consumer<Set<Integer>> adjuster) {
        process(pieces.getEdgesAsBase(piece, 0), adjuster);
    }

    /**
     * Adds all valid permutations of the edges.
     * @param adjuster receives sets and .
     */
    private void process(long edges, Consumer<Set<Integer>> adjuster) {
//        System.out.println("PieceTracker.process(edges=" + Long.toBinaryString(edges) + ")");
        // All edges are always defined for pieces, so we permutate and call all possible sets
        if (REMOVE_DUPLICATES) {
            Arrays.fill(encounteredOnes, false);
        }
        for (int rotation = 0 ; rotation < 4 ; rotation++) {
            adjuster.accept(four.get(EBits.getHash(0b1111, edges)));
            adjuster.accept(three.get(EBits.getHash(0b1110, edges)));
            adjuster.accept(two.get(EBits.getHash(0b1100, edges)));
            if (REMOVE_DUPLICATES) {
                final int oneHash = EBits.getHash(0b1000, edges);
                if (!encounteredOnes[oneHash]) {
                    encounteredOnes[oneHash] = true;
                    adjuster.accept(one.get(oneHash));
                }
            } else {
                adjuster.accept(one.get(EBits.getHash(0b1000, edges)));
            }
            adjuster.accept(opposing.get(EBits.getHash(0b1010, edges)));
            edges = EBits.shiftEdgesRight(edges);
        }
    }
    final boolean[] encounteredOnes = new boolean[24];

    /**
     * @return the number of sets that contains the given piece;
     */
    public long countSets(int piece) {
        return one.pieces.stream().filter(set -> set.contains(piece)).count() +
               two.pieces.stream().filter(set -> set.contains(piece)).count() +
               three.pieces.stream().filter(set -> set.contains(piece)).count() +
               opposing.pieces.stream().filter(set -> set.contains(piece)).count() +
               four.values().stream().filter(set -> set.contains(piece)).count();

    }

    /**
     * Given constraints from the surrounding fields, return the best matching pieces, i.e. if all 4 edges has a color,
     * only return results from {@link #four}.
     * @return best matching pieces or empty list if there are no matching pieces.
     */
    public Set<Integer> getBestMatching(long state) {
        final int hash = EBits.getOuterHash(state);
        int defined = EBits.getDefinedEdges(state);

        switch (defined) {
            case 0b0000: return all;

            case 0b1000:
            case 0b0100:
            case 0b0010:
            case 0b0001: return one.get(hash);

            case 0b1100:
            case 0b0110:
            case 0b0011:
            case 0b1001: return two.get(hash);

            case 0b1010:
            case 0b0101: return opposing.get(hash);

            case 0b1110:
            case 0b0111:
            case 0b1011:
            case 0b1101: return three.get(hash);

            case 0b1111: return four.get(hash);
            default: throw new IllegalArgumentException("The edges should never be above 0b1111 (15) but was " + defined);
        }
    }

    @Override
    public String toString() {
        return "PieceTracker{" +
               "#one=" + one.size() +
               ", #two=" + two.size() +
               ", #three=" + three.size() +
               ", #opposing=" + opposing.size() +
               ", #four=" + four.size() +
               ", #all=" + all.size() +
               '}';
    }

    private static class PieceHolder extends HashMap<Integer, Set<Integer>> {
        @Override
        public Set<Integer> get(Object key) {
            if (!(key instanceof Integer)) {
                throw new IllegalArgumentException("Key must be a valid Integer but was " + key);
            }
            if (!containsKey(key)) {
                put((Integer)key, new HashSet<>());
            }
            return super.get(key);
        }

        public void addPiece(Integer key, Integer piece) {
            if (!containsKey(key)) {
                super.put(key, new HashSet<>());
            }
            super.get(key).add(piece);
        }
    }

    // TODO: Consider using bitmaps instead of sets as these can be OR'ed fast
    private static class PieceHolderRadix {
        private final List<Set<Integer>> pieces;

        public PieceHolderRadix(int max) {
            pieces = new ArrayList<>();
            for (int i = 0 ; i <= max ; i++) {
                pieces.add(new HashSet<>());
            }
        }

        public Set<Integer> get(Integer key) {
            return pieces.get(key);
        }

        public void addPiece(Integer key, Integer piece) {
            pieces.get(key).add(piece);
        }

        public int size() {
           return (int) pieces.stream().filter(s -> !s.isEmpty()).count();
        }
    }
}
