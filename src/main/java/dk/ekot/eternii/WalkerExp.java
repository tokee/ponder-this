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

import java.util.Collection;
import java.util.Comparator;
import java.util.function.ToIntFunction;

/**
 * For experimentation
 *
 */
public class WalkerExp extends WalkerImpl {
    private static final Logger log = LoggerFactory.getLogger(WalkerExp.class);

    public WalkerExp(EBoard board) {
        super(board);
    }

    @Override
    protected Comparator<Move> getMoveComparator() {
        return Comparator.
                //comparingInt(this::onBoardEdges)
                comparingInt(Move::clueCornersOrdered)
                //.thenComparingInt(Move::clueCornersOrdered)
                //.thenComparingInt(this::onBoardEdges)
                .thenComparingInt(onTopLeftBottomRight());
    }

    @Override
    protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.
                comparingInt(this::boardEdges)
                //comparingInt(this::clueCornersOrdered)
                //.thenComparingInt(this::clueCornersOrdered)
                //.thenComparingInt(this::boardEdges)
                .thenComparingInt(topLeftBottomRight());
    }


}
