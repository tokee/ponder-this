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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides next Field wih corresponding Pieces to try.
 */
public interface Walker {

    EBoard getBoard();

    /**
     * Encapsulation of a field and pieces that can be legally placed on the field.
     * Lazy evaluation where possible.
     */
    class Move {
        final EBoard board;
        final int x;
        final int y;
        final int w;
        final int h;
        final int fieldCount;
        int piecesSize;
        Set<Integer> localPieceIDs = null;

        public Move(EBoard board, EBoard.Field field) {
            this.board = board;
            x = field.getX();
            y = field.getY();
            w = board.getWidth();
            h = board.getHeight();
            fieldCount = w*h;
            piecesSize = board.getPieceIDs(x, y).size(); // Very often called
        }

        public Move(EBoard board, int x, int y) {
            this.board = board;
            this.x = x;
            this.y = y;
            w = board.getWidth();
            h = board.getHeight();
            fieldCount = w*h;
            piecesSize = board.getPieceIDs(x, y).size(); // Very often called
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        /**
         * Not recommended due to garbage collection overhead. Use {@link #fillPieceIDs(int[])} instead.
         * @return pieceIDs.
         */
        public Set<Integer> getPieceIDs() {
            if (localPieceIDs == null) {
                localPieceIDs = new HashSet<>(board.getPieceIDs(x, y)); // We copy to avoid concurrent modificaton exception
            }
            return localPieceIDs;
        }

        /**
         * Adds pieceIDs to the given buffer. This is the recommended way to get the pieceIDs.
         * @return the number of added IDs.
         */
        public int fillPieceIDs(int[] buffer) {
            AtomicInteger index = new AtomicInteger(0);
            (localPieceIDs == null ? board.getPieceIDs(x, y) : localPieceIDs).forEach(
                    id -> buffer[index.getAndIncrement()] = id);
            return index.get();
        }

        public int[] getPieceIDsArray() {
            final Set<Integer> ids = localPieceIDs == null ? board.getPieceIDs(x, y) : localPieceIDs;
            int[] buffer = new int[ids.size()];
            AtomicInteger index = new AtomicInteger(0);
            ids.forEach(id -> buffer[index.getAndIncrement()] = id);
            return buffer;
        }

        public int validPiecesSize() {
            return piecesSize;
        }

        public void setLocalPieceIDs(Set<Integer> localPieceIDs) {
            this.localPieceIDs = localPieceIDs;
            piecesSize = localPieceIDs.size();
        }

        public int[] getValidRotations(int pieceID) {
            return board.getValidRotations(x, y, pieceID);
        }

        public boolean isOnEdge() {
            return x == 0 || x == w-1 || y == 0 || y == h-1;
        }

        /* Comparators below */

        public int leastSetOuterEdgesFirst() {
            return EBits.countDefinedEdges(board.getState(x, y));
        }
        public int mostSetOuterEdgesFirst() {
            return 4-EBits.countDefinedEdges(board.getState(x, y));
        }

        public int boardEdgeFirst() {
            return x == 0 || x == w-1 || y == 0 || y == h-1 ? 0 : 1;
        }

        /**
         * @return 0 if in one of the four 3x3 corners, else 1.
         */
        public int clueCornersFirst() {
            return ((x <= 2 || x >= w-3) && (y <= 2 || y >= h-3)) ? 0 : 1;
        }

        /**
         * Top-left, top-right. bottom-left, bottom-right corner.
         */
        public int clueCornersOrdered() {
            if (x < 3) {
                if (y < 3) {
                    return 1;
                } else if (y >= h - 3){
                    return 3;
                }
            } else if (x >= w - 3) {
                if (y < 3) {
                    return 2;
                } else if (y >= h - 3){
                    return 4;
                }
            }
            return 5;
        }

        public int identity() {
            return 0;
        }

        public int topLeftFirst() {
            return y*w + x;
        }

        public int bottomRightFirst() {
            return fieldCount-(y*w + x);
        }

        /**
         * @return 0 if a corner, 1 if in a 3x3 corners, else 2.
         */
        public int cornersToCluesFirst() {
            return  ((x == 0 || x == w-1) && (y== 0 || y == h-1)) ? 0 :
                    ((x <= 2 || x >= w-3) && (y <= 2 || y >= h-3)) ? 1 : 2;
        }

        /**
         * @return nearest to top-left or bottom-right corner, measured row by row.
         */
        public int topLeftBottomRightBestFirst() {
            final int tl = topLeftFirst();
            final int br = 255-tl;
            return Math.min(tl, br);
        }
        /**
         * @return 0 is inside rectangle (all edges inclusive) else 1.
         */
        public static ToIntFunction<Move> rectFirst(int x1, int y1, int x2, int y2) {
            return move -> {
                final int x = move.getX();
                final int y = move.getY();
                return x >= x1 && x <= x2 && y >= y1 && y <= y2 ? 0 : 1;
            };
        }
    }

    /**
     * @deprecated use delayed evaluation with {@link Move}.
     */
    default EBoard.Pair<EBoard.Field, List<EBoard.Piece>> toRotatedPieces(EBoard.Pair<EBoard.Field, Set<Integer>> pair) {
        if (pair == null) {
            return null;
        }
        EBoard.Field field = pair.left;
        List<EBoard.Piece> pieces = pair.right.stream()
                .map(p -> new EBoard.Piece(p, field.getValidRotation(p)))
//                .filter(piece -> isValidPieceOnPosition(field.getX(), field.getY(), piece))
                .collect(Collectors.toList());
        return new EBoard.Pair<>(field, pieces);
    }

    /**
     * @return All free fields, with corresponding valid pieces, rotated and sorted.
     * @deprecated use {@link #getAll} instead and switch to delayed rotation.
     */
    Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getAllRotated();

    /**
     * @return a single move (position on the board with candidate pieces, sorted by Walker priority). Null if no move.
     */
    default Move get() {
        return getAll().findFirst().orElse(null);
    }

    /**
     * @deprecated use {@link #get} instead.
     */
    EBoard.Pair<EBoard.Field, List<EBoard.Piece>> getLegacy();


    /**
     * @return all possible moves, prioritized by the Walker implementation.
     */
    Stream<Move> getAll();

    default Stream<Move> getMinima() {
        throw new UnsupportedOperationException("Not implemented for this walker");
    }


    /**
     * @return All free fields, with corresponding valid pieces (not rotated), not sorted.
     * @deprecated use {@link #streamRawMoves} instead
     */
    default List<EBoard.Pair<EBoard.Field, Set<Integer>>> getFreeFieldsRaw() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> fields = new ArrayList<>(getBoard().getWidth() * getBoard().getHeight());
        getBoard().visitAllFree((x, y) -> {
            EBoard.Field field = getBoard().getField(x, y);
            fields.add(new EBoard.Pair<>(field, field.getBestPiecesNonRotating()));
        });
        return fields;
    }

    /**
     * @return all possible moves that have non-0 candidates, left-to-right, top-to-bottom (cheap operation).
     */
    default Stream<Move> streamRawMoves() {
        final EBoard board = getBoard();
        return board.streamAllFields().
                filter(EBoard.Field::isFree).
                filter(field -> !board.getPieceIDs(field.getX(), field.getY()).isEmpty()).
                map(field -> new Move(board, field));
    }

    /* Comparators below ---------------------------------------------------------------------------------------- */

    /**
     * @return 0 if board edge piece, else 1.
     */
    default int boardEdges(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        return pair.left.getX() == 0 || pair.left.getY() == 0 ||
               pair.left.getX() == getBoard().getWidth() - 1 ||
               pair.left.getY() == getBoard().getHeight() - 1 ? 0 : 1;
    }

    /**
     * @return 0 if in one of the four 3x3 corners, else 1.
     */
    default int clueCorners(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        final int x = pair.left.getX();
        final int y = pair.left.getY();
        final int w = getBoard().getWidth(); // 16
        final int h = getBoard().getHeight();

        return ((x <= 2 || x >= w-3) && (y <= 2 || y >= h-3)) ? 0 : 1;
    }

    /**
     * @return 0 if a corner, 1 if in a 3x3 corners, else 2.
     */
    default int cornersToClues(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        final int x = pair.left.getX();
        final int y = pair.left.getY();
        final int w = getBoard().getWidth();
        final int h = getBoard().getHeight();

        return  ((x == 0 || x == w-1) && (y== 0 || y == h-1)) ? 0 :
                ((x <= 2 || x >= w-3) && (y <= 2 || y >= h-3)) ? 1 : 2;
    }

    /**
     * @return amount of valid pieces.
     */
    default int validPieces(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        return pair.right.size();
    }

    /**
     * @return nearest to top-left corner, measured row by row.
     */
    default ToIntFunction<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> topLeft() {
        final int boardWidth = getBoard().getWidth();
        return pair -> pair.left.getY() * boardWidth + pair.left.getX();
    }

    /**
     * @return nearest to top-left or bottom-right corner, measured row by row.
     */
    default ToIntFunction<? super EBoard.Pair<EBoard.Field, ? extends Collection<?>>> topLeftBottomRight() {
        final int boardWidth = getBoard().getWidth();
        return pair -> {
            final int tl = pair.left.getY() * boardWidth + pair.left.getX();
            final int br = 255-tl;
            return Math.min(tl, br);
        };
    }

    /**
     * Top-left, top-right. bottom-left, bottom-right corner.
     */
    default int clueCornersOrdered(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        final int x = pair.left.getX();
        final int y = pair.left.getY();
        final int w = getBoard().getWidth();
        final int h = getBoard().getHeight();

        if (x < 3) {
            if (y < 3) {
                return 1;
            } else if (y >= h - 3){
                return 3;
            }
        } else if (x >= w - 3) {
            if (y < 3) {
                return 2;
            } else if (y >= h - 3){
                return 4;
            }
        }
        return 5;
    }

    static ToIntFunction<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> spiralIn(EBoard board) {
        return priority(PatternCreator.spiralIn(board.getWidth())); // TODO: Check if width differs
    }
    static ToIntFunction<Move> onSpiralIn(EBoard board) {
        return onPriority(PatternCreator.spiralIn(board.getWidth())); // TODO: Check if width differs
    }

    static ToIntFunction<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> spiralOut(EBoard board) {
        return priority(PatternCreator.spiralOut(board.getWidth())); // TODO: Check if width differs
    }
    static ToIntFunction<Move> onSpiralOut(EBoard board) {
        return onPriority(PatternCreator.spiralOut(board.getWidth())); // TODO: Check if width differs
    }

    /**
     * @param coordinates array of coordinates, where each entry is {@code [x, y]}
     * @return position in the coordinates array or coordinates.length if outside of array.
     */
    static ToIntFunction<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> priority(int[][] coordinates) {
        return pair -> {
            int x = pair.left.getX();
            int y = pair.left.getY();
            for (int i = 0 ; i < coordinates.length ; i++) {
                if (coordinates[i][0] == x && coordinates[i][1] == y) {
                    return i;
                }
            }
            return coordinates.length;
        };
    }
    /**
     * @param coordinates array of coordinates, where each entry is {@code [x, y]}
     * @return position in the coordinates array or coordinates.length if outside of array.
     */
    static ToIntFunction<Move> onPriority(int[][] coordinates) {
        return move -> {
            int x = move.getX();
            int y = move.getY();
            for (int i = 0 ; i < coordinates.length ; i++) {
                if (coordinates[i][0] == x && coordinates[i][1] == y) {
                    return i;
                }
            }
            return coordinates.length;
        };
    }

    /**
     * @param coordinates array of array of coordinates, where each entry is {@code [x, y]}
     * @return position in the coordinates array array or 256 if outside of array.
     */
    default ToIntFunction<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> priority(int[][][] coordinates) {
        return pair -> {
            int x = pair.left.getX();
            int y = pair.left.getY();
            for (int i = 0 ; i < coordinates.length ; i++) {
                final int[][] group = coordinates[i];
                for (int j = 0; j < group.length; j++) {
                    if (group[j][0] == x && group[j][1] == y) {
                        return i;
                    }
                }
            }
            return 256;
        };
    }
    /**
     * @param coordinates array of array of coordinates, where each entry is {@code [x, y]}
     * @return position in the coordinates array array or 256 if outside of array.
     */
    default ToIntFunction<Move> onPriority(int[][][] coordinates) {
        return move -> {
            int x = move.getX();
            int y = move.getY();
            for (int i = 0 ; i < coordinates.length ; i++) {
                final int[][] group = coordinates[i];
                for (int j = 0; j < group.length; j++) {
                    if (group[j][0] == x && group[j][1] == y) {
                        return i;
                    }
                }
            }
            return 256;
        };
    }

    /**
     * @return distance to origo (closer is better).
     */
    default int origoDist(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair, int origoX, int origoY) {
        final int x = pair.left.getX();
        final int y = pair.left.getY();

        final int deltaX = x-origoX;
        final int deltaY = y-origoY;
        return (deltaX*deltaX)+(deltaY*deltaY); // Don't care about square as this is only ofr comparison
    }
    /**
     * @return distance to origo (closer is better).
     */
    default int onOrigoDist(Move move, int origoX, int origoY) {
        final int x = move.getX();
        final int y = move.getY();

        final int deltaX = x-origoX;
        final int deltaY = y-origoY;
        return (deltaX*deltaX)+(deltaY*deltaY); // Don't care about square as this is only ofr comparison
    }

    /**
     * @return 0 is inside rectangle (all edges inclusive) else 1.
     */
    default ToIntFunction<? super EBoard.Pair<EBoard.Field, ? extends Collection<?>>> rect(int x1, int y1, int x2, int y2) {
        return pair -> {
            final int x = pair.left.getX();
            final int y = pair.left.getY();
            return x >= x1 && x <= x2 && y >= y1 && y <= y2 ? 0 : 1;
        };
    }

}
