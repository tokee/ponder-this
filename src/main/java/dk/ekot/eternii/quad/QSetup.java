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
package dk.ekot.eternii.quad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Misc. settings for the quads.
 */
public class QSetup {
    private static final Logger log = LoggerFactory.getLogger(QSetup.class);

    private int maxDepth = 65;
    private long maxAttempts = Long.MAX_VALUE;
    private QuadDelivery quadDelivery = QuadDelivery.IDENTITY;
    private BiFunction<Integer, QBoard, Boolean> solutionCallback = QSolverBacktrack.PRINT_ALL;
    private Function<QBoard, QWalker> walkerFactory = board ->
            new QWalkerImpl(board, Comparator.comparingInt(QWalker.topLeft()));

    /**
     * Maximum depth for the solver.
     */
    public QSetup maxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Maximum number of placed quads.
     */
    public QSetup maxAttempts(long maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public long getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(long maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Potentially changes order of quads delivered from a move.
     */
    public QSetup quadDelivery(QuadDelivery quadDelivery) {
        this.quadDelivery = quadDelivery;
        return this;
    }

    public QuadDelivery getQuadDelivery() {
        return quadDelivery;
    }

    public void setQuadDelivery(QuadDelivery quadDelivery) {
        this.quadDelivery = quadDelivery;
    }

    /**
     * Called each time maxDepth is reached.
     * If true is returned, the next quad is tried. If false is returned, processing returns to the calling level.
     */
    public QSetup solutionCallback(BiFunction<Integer, QBoard, Boolean> solutionCallback) {
        this.solutionCallback = solutionCallback;
        return this;
    }

    public BiFunction<Integer, QBoard, Boolean> getSolutionCallback() {
        return solutionCallback;
    }

    public void setSolutionCallback(BiFunction<Integer, QBoard, Boolean> solutionCallback) {
        this.solutionCallback = solutionCallback;
    }

    /**
     * Delivers Moves.
     */
    public QSetup walkerFactory(Function<QBoard, QWalker> walkerFactory) {
        this.walkerFactory = walkerFactory;
        return this;
    }

    public Function<QBoard, QWalker> getWalkerFactory() {
        return walkerFactory;
    }

    public void setWalkerFactory(Function<QBoard, QWalker> walkerFactory) {
        this.walkerFactory = walkerFactory;
    }

    public QSetup walker(Comparator<QWalker.Move> moveComparator) {
        walkerFactory = board -> new QWalkerImpl(board, moveComparator);
        return this;
    }

    public QSetup walker(ToIntFunction<QWalker.Move>... comparators) {
        Comparator<QWalker.Move> chained = Comparator.comparingInt(comparators[0]);
        for (int i = 1 ; i < comparators.length ; i++) {
            chained = chained.thenComparingInt(comparators[i]);
        }
        final Comparator<QWalker.Move> complete = chained;

        walkerFactory = board -> new QWalkerImpl(board, complete);
        return this;
    }

    public QWalker getWalker(QBoard board) {
        return getWalkerFactory().apply(board);
    }

    /**
     * Creates a factory for {@link QWalkerImpl} with the given {@code moveComparator}.
     */
    public void setWalker(Comparator<QWalker.Move> moveComparator) {
        walkerFactory = board -> new QWalkerImpl(board, moveComparator);
    }
}
