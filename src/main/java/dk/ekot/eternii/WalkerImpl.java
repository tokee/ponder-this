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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Super class for Walkers.
 *
 * Requests all free fields with piece-IDs and used {@link #comparator} to return {@code min} in {@link #get()}.
 *
 */
public abstract class WalkerImpl implements Walker {
    private final EBoard board;
    private final Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> comparator;

    public WalkerImpl(EBoard board) {
        this.board = board;
        comparator = getFieldComparator();
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeRaw();
        return all.isEmpty() ? null : toPieces(Collections.min(all, comparator));
    }

    @Override
    public EBoard getBoard() {
        return board;
    }

    protected abstract Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator();
}
