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

import dk.ekot.eternii.BoardVisualiser;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 *
 */
public class QSolverTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(QSolverTest.class);

    public void testBasic() {
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live);
        QWalker walker = new QWalkerImpl(board,
                Comparator.comparingInt(QWalker.cornersOrdered()).
                        thenComparingInt(QWalker.borders()).
                        thenComparingInt(QWalker.borderBorders()).
                        thenComparingInt(QWalker.topLeft()));
        QSolverBacktrack solver = new QSolverBacktrack(board, walker);

        try {
            solver.run();
        } catch (Exception e) {
            log.warn("Got exception while running", e);
        }

        try {
            Thread.sleep(1000000000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
