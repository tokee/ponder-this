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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *  Keeps track of pieces.
 */
public class PieceTracker {
    private static final Logger log = LoggerFactory.getLogger(PieceTracker.class);

    public static final int EF = 30; // Must be > max edge type id (int)

    // The sets holds pieces
    
    // Edge -> pieces
    private final PieceHolder one = new PieceHolder();
    // [edge1, edge2 (clockwise immediately after edge1)] -> pieces
    private final PieceHolder two = new PieceHolder();
    // [edge1, edge2 (clockwise immediately after edge1), edge3 (clockwise immediately after edge2)] -> pieces
    private final PieceHolder three = new PieceHolder();
    // [edge1, edge2 (at the other side of edge)] -> pieces. One 180 degree rotation
    private final PieceHolder opposing = new PieceHolder();
    // [edge1, edge2, edge3, edge4] -> pieces. All rotations
    private final PieceHolder four = new PieceHolder();

    private final Set<Integer> all = new HashSet<>();

    private final EPieces pieces;

    public PieceTracker(EPieces pieces) {
        this.pieces = pieces;
    }

    private void addPiece(int piece) {
        process(piece, set -> set.add(piece));
        if (!all.add(piece)) {
            throw new IllegalStateException("Add piece " + piece + " called, but the piece was already present");
        }
    }
    private void removePiece(int piece) {
        process(piece, set -> set.remove(piece));
        if (!all.remove(piece)) {
            throw new IllegalStateException("Remove piece " + piece + " called, but the piece was not present");
        }
    }
    private void process(int piece, Consumer<Set<Integer>> adjuster) {
        process(pieces.getTop(piece, 0), pieces.getRight(piece, 0),
                pieces.getBottom(piece, 0), pieces.getLeft(piece, 0),
                adjuster);
    }

    /**
     * Adds all valid permutations of the edges.
     * @param edge1 an edge or -1 if not present.
     * @param edge2 an edge or -1 if not present.
     * @param edge3 an edge or -1 if not present.
     * @param edge4 an edge or -1 if not present.
     * @param adjuster receives sets and .
     */
    private void process(int edge1, int edge2, int edge3, int edge4, Consumer<Set<Integer>> adjuster) {
        // All edges are always defined for pieces, so we permutate and call all possible sets
        
        processFour(edge1, edge2, edge3, edge4, adjuster);
        processThree(edge2, edge3, edge4, adjuster);
        processThree(edge3, edge4, edge1, adjuster);
        processThree(edge4, edge1, edge2, adjuster);
        processTwo(edge1, edge2, adjuster);
        processTwo(edge2, edge3, adjuster);
        processTwo(edge3, edge4, adjuster);
        processTwo(edge4, edge1, adjuster);
        processOne(edge1, adjuster);
        processOne(edge2, adjuster);
        processOne(edge3, adjuster);
        processOne(edge4, adjuster);
        processOpposing(edge1, edge3, adjuster);
        processOpposing(edge2, edge4, adjuster);
    }
    
    public void processOne(int edge1, Consumer<Set<Integer>> adjuster) {
        adjuster.accept(one.getPieces(edge1));
    }
    public Set<Integer> getOnes(int edge1) {
        return one.getPieces(edge1);
    }

    public void processTwo(int edge1, int edge2, Consumer<Set<Integer>> adjuster) {
        adjuster.accept(two.getPieces(EF * edge1 + edge2));
    }
    public Set<Integer> getTwos(int edge1, int edge2) {
        return two.getPieces(EF * edge1 + edge2);
    }

    public void processOpposing(int edge1, int edge2, Consumer<Set<Integer>> adjuster) {
        adjuster.accept(opposing.getPieces(EF * edge1 + edge2));
        if (edge1 != edge2) {
            adjuster.accept(opposing.getPieces(EF * edge2 + edge1));
        }
    }
    public Set<Integer> getOpposings(int edge1, int edge2) {
        if (edge1 != edge2) {
            Set<Integer> joined = new HashSet<>();
            joined.addAll(opposing.getPieces(EF * edge1 + edge2));
            joined.addAll(opposing.getPieces(EF * edge2 + edge1));
            return joined;
        }
        return opposing.getPieces(EF * edge1 + edge2);
    }

    public void processThree(int edge1, int edge2, int edge3, Consumer<Set<Integer>> adjuster) {
        adjuster.accept(three.getPieces(EF * EF * edge1 + EF * edge2 + edge3));
    }
    public Set<Integer> getThrees(int edge1, int edge2, int edge3) {
        return three.getPieces(EF * EF * edge1 + EF * edge2 + edge3);
    }

    public void processFour(int edge1, int edge2, int edge3, int edge4, Consumer<Set<Integer>> adjuster) {
        visitFour(edge1, edge2, edge3, edge4, key -> adjuster.accept(four.get(key)));
    }
    public Set<Integer> getFours(int edge1, int edge2, int edge3, int edge4) {
        Set<Integer> joined = new HashSet<>();
        visitFour(edge1, edge2, edge3, edge4, key -> joined.addAll(four.getPieces(key)));
        return joined;
    }
    private void visitFour(int edge1, int edge2, int edge3, int edge4, Consumer<Integer> callback) {
        callback.accept(EF * EF * EF * edge1 + EF * EF * edge2 + EF * edge3 + edge4);
        callback.accept(EF * EF * EF * edge2 + EF * EF * edge3 + EF * edge4 + edge1);
        callback.accept(EF * EF * EF * edge3 + EF * EF * edge4 + EF * edge1 + edge2);
        callback.accept(EF * EF * EF * edge4 + EF * EF * edge1 + EF * edge2 + edge3);
    }
    
    private static class PieceHolder extends HashMap<Integer, Set<Integer>> {
        public Set<Integer> getPieces(Integer key) {
            if (!containsKey(key)) {
                put(key, new HashSet<>());
            }
            return get(key);
        }
        public void addPiece(Integer key, Integer piece) {
            if (!containsKey(key)) {
                put(key, new HashSet<>());
            }
            get(key).add(piece);
        }
    }
}
