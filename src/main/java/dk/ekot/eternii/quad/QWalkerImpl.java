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

/**
 * Base implementation.
 */
public class QWalkerImpl implements QWalker {
    private static final Logger log = LoggerFactory.getLogger(QWalkerImpl.class);

    final QBoard board;
    final Comparator<QWalker.Move> moveComparator;

    public QWalkerImpl(QBoard board, Comparator<QWalker.Move> moveComparator) {
        this.board = board;
        this.moveComparator = moveComparator;
    }

    @Override
    public QBoard getBoard() {
        return board;
    }

    @Override
    public Comparator<Move> getMoveComparator() {
        return moveComparator;
    }
}
