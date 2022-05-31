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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Eternity II board (flexible size)
 *
 * Principle:
 *
 * The board is made up of fields, holding pieces. Every field on the board has 4 edges; the board itself
 * also has an edge, which is called the board-edge.
 *
 * Edge requirements are handled by the {@link EdgeTracker}. At the start only the board-edges are registered as
 * requirements, each counting negative in the tracker. All free pieces are counted positive. When the edges of an
 * empty field or a free piece is tracked, all combinations of colored edges are registered.
 *
 * An empty field with 3 surrounding pieces with edges ABC are tracked as ABC, AV, BC, A-C, C-A, A, B, C where
 * A-C is opposing sides.
 * Duplicates are allowed for single-sides, so the field with edges ABA is tracked as ABA, AB, BA, A-A, A, B, A.
 *
 * A free piece is always tracked with all combinations:
 * ABCD -> ABCD, ABC, BCD, CDA, DAB, AB, BC, CD, DA, A-C, B-D, C-A, D-B, A, B, C, D
 *
 * When a piece is positioned at (x, y), the algorithm is as follows:
 * 1) For all free fields in the 9 fields from (x-1, y-1) to (x+1, y+1), update the edge tracker positively
 * 2) Position the piece on (x, y)
 * 3) For all free surrounding fields in the 8 fields from (x-1, y-1) to (x+1, y+1), excluding (x, y),
 *    update the edge tracker negatively
 * 4) Update the tracking of the edges for the (formerly) free piece negatively
 * 5) Remove the piece from the free collection
 * If at any point during #3 and #4 any tracker becomes negative, mark the attempt as failed
 * 6) If the attempt was failed, rollback the positioning and the tracking
 *
 * The field for the next piece is selected by priority:
 * 0) Free fields
 * 1) Fields with the least matching pieces
 * 2) Fields with the most surrounding edges
 * 3) Top left field
 *
 * Known weakness:
 * Tracking all combinations for a piece leads to the same piece counting multiple times:
 * If the board has has fields with edges AB, CD and there is a piece ABCD, it will count both for AB and CD.
 */
public class EBoard {
    private static final Logger log = LoggerFactory.getLogger(EBoard.class);

    private final EPieces pieces;
    private final PieceTracker freeBag;

    private final int width;
    private final int height;

    private final long[][] board; // states, as defined in EBits
    private final EdgeTracker edgeTracker = new EdgeTracker();

    private final Set<Observer> observers = new HashSet<>();

    public EBoard(EPieces pieces, int width, int height) {
        this.pieces = pieces;
        this.width = width;
        this.height = height;
        this.board = new long[width][height];
        for (int x = 0 ; x < width ; x++) {
            Arrays.fill(board[x], EBits.BLANK_STATE);
        }
        freeBag = new PieceTracker(pieces);
/*        pieces.allPieces()
                .boxed()
                .peek(piece -> updatePieceTracking(piece, 1))
                .forEach(this.freeBag::add);*/
        log.debug("EdgeTracker blank: " + getEdgeTracker());
        updateEdgeTrackerAll(-1);
        log.debug("EdgeTracker after EBoard construction: " + getEdgeTracker());
    }

    private boolean isCorner(int x, int y) {
        return (x == 0 && y == 0) || (x == width-1 && y == 0) ||
               (x == 0 && y == height-1) || (x == width-1 && y == height-1);
    }

    /**
     * Remove the piece at the given position, updating the tracker and adding the piece to the free bag.
     */
    public void removePiece(int x, int y) {
        int piece = getPiece(x, y);
        if (piece == -1) {
            throw new IllegalStateException("removePiece(" + x + ", " + y + ") called but the field has no piece");
        }
        // Remove piece from board
        updateTracker9(x, y, +1);
        board[x][y] = EBits.BLANK_STATE;
        updateTracker9(x, y, -1);

        // Register piece as free
        updatePieceTracking(piece, +1);
        freeBag.add(piece);
        notifyObservers(x, y, "");
    }

    /**
     * Positions the piece without updating or checking any of the tracking structures.
     * Use only for visualisation!
     */
    public void placeUntrackedPiece(int x, int y, int piece, int rotation) {
        board[x][y] = EBits.setPieceFull (board[x][y], piece, rotation, pieces.getEdges(piece, rotation));
        notifyObservers(x, y, "");
    }

    /**
     * Positions the given piece on the board, updating the tracker and removing the piece from the free bag.
     * @return if the positioning resulted in a negative tracker and was rolled back.
     */
    public boolean placePiece(int x, int y, int piece, int rotation, String label) {

        if (EBits.getPiece(board[x][y]) != -1) {
            throw new IllegalStateException(
                    "placePiece(" + x + ", " + y + ", ...) called but the field already had a piece");
        }
        // Remove surrounding registers
        updateTracker9(x, y, +1);
        board[x][y] = EBits.setPieceFull(board[x][y], piece, rotation, pieces.getEdges(piece, rotation));
        updateOuterEdges(x, y);
        if (!updateTracker9(x, y, -1)) {
            // At least one tracker is negative so we rollback
            updateTracker9(x, y, +1);
            board[x][y] = -1;
            updateTracker9(x, y, -1);
            return false;
        }
        if (!updatePieceTracking(piece, -1)) {
            updatePieceTracking(piece, +1);
            // At least one tracker is negative so we rollback
            updateTracker9(x, y, +1);
            board[x][y] = -1;
            updateTracker9(x, y, -1);
            return false;
        }
        if (!freeBag.remove(piece)) {
           throw new IllegalStateException("Tried removing piece " + piece + " from the free bag but it was not there");
        }
        notifyObservers(x, y, label);
        return true;
    }

    private void updateOuterEdges(int x, int y) {
        int topEdge = lenientGetBottomEdge(x, y-1);
        int rightEdge = lenientGetLeftEdge(x+1, y);
        int bottomEdge = lenientGetTopEdge(x, y+1);
        int leftEdge = lenientGetRightEdge(x-1, y);

        board[x][y] = EBits.setAllEdges(board[x][y], topEdge, rightEdge, bottomEdge, leftEdge);
    }

    /**
     * Register edges from the given pieces and add them to the board bag.
     * @param pieces the pieces to register.
     */
    public void registerFreePieces(Collection<Integer> pieces) {
        pieces.stream()
                .peek(piece -> updatePieceTracking(piece, 1))
                .forEach(this.freeBag::add);
    }

    /**
     * Register edges from the given piece.
     * @param piece the piece to register.
     * @param delta the amount to adjust the edge counters with.
     */
    private boolean updatePieceTracking(int piece, int delta) {
        return edgeTracker.add(pieces.getTop(piece, 0), pieces.getRight(piece, 0),
                               pieces.getBottom(piece, 0), pieces.getLeft(piece, 0), delta);
    }

    /**
     * Update the tracker for the fields at (origoX, origoY) and surrounding fields.
     * @param origoX start X.
     * @param origoY start Y.
     * @param delta the amount to update with (typically -1 or 1).
     */
    private boolean updateTracker9(int origoX, int origoY, int delta) {
        AtomicBoolean allOK = new AtomicBoolean(true);
        visit9(origoX, origoY, (x, y) -> {
            allOK.set(allOK.get() && updateTracker(x, y, delta));
//            if (!allOK.get()) {
//                System.out.println("Invalidated at " + x + ", " + y);
//            }
        });
        return allOK.get();
    }

    /**
     * Update the tracker for the field (x, y).
     * @param delta the amount to update with (typically -1 or 1).
     * @return false if the updating resulted in at least 1 tracker reaching a negative state.
     */
    private boolean updateTracker(int x, int y, int delta) {
        if (board[x][y] != -1) { // No action if occupied
            return true;
        }
        int topEdge = lenientGetBottomEdge(x, y-1);
        int rightEdge = lenientGetLeftEdge(x+1, y);
        int bottomEdge = lenientGetTopEdge(x, y+1);
        int leftEdge = lenientGetRightEdge(x-1, y);
        return edgeTracker.add(topEdge, rightEdge, bottomEdge, leftEdge, delta);
    }


    /**
     * Update the tracker for all fields.
     * @param delta the amount to update with (typically -1 or 1).
     */
    private void updateEdgeTrackerAll(int delta) {
        streamAllFields()
                .filter(Field::isFree)
                .forEach(field -> {
                    int topEdge = lenientGetBottomEdge(field.getX(), field.getY()-1);
                    int rightEdge = lenientGetLeftEdge(field.getX()+1, field.getY());
                    int bottomEdge = lenientGetTopEdge(field.getX(), field.getY()+1);
                    int leftEdge = lenientGetRightEdge(field.getX()-1, field.getY());
//                if (topEdge != -1 || rightEdge != -1 || bottomEdge != -1 || leftEdge != -1) {
//                    System.out.println("(" + field.getX() + ", " + field.getY() + ") " + topEdge + " " + rightEdge + " " + bottomEdge + " " + leftEdge);
//                }
                    edgeTracker.add(topEdge, rightEdge, bottomEdge, leftEdge, delta);

                });
    }

    public EPieces getPieces() {
        return pieces;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Stream<Field> streamAllFields() {
        if (allFields == null) {
            allFields = new ArrayList<>(width*height);
            for (int y = 0 ; y < height ; y++) {
                for (int x = 0; x < width; x++) {
                    allFields.add(new Field(x, y));
                }
            }
        }
        return allFields.stream();
    }
    private List<Field> allFields;

    /**
     * All fields from (origoX-1, origoY-1) to (origoX+1, origoY+1) that are on the board.
     */
    private Stream<Field> streamValidFields9(int origoX, int origoY) {
        List<Field> fields = new ArrayList<>(9);
        for (int y = origoY-1 ; y < origoY+1 ; y++) {
            if (y < 0 || y >= height) {
                continue;
            }
            for (int x = origoX-1 ; x < origoY+1 ; x++) {
                if (x < 0 || x >= width) {
                    continue;                                                       
                }
                fields.add(new Field(x, y));
            }
        }
        return fields.stream();
    }

    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetTopEdge(int x, int y) {
        return y >= height ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : EBits.getPieceNorthEdge(board[x][y]);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetRightEdge(int x, int y) {
        return x < 0 ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : EBits.getPieceEastEdge(board[x][y]);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetBottomEdge(int x, int y) {
        return y < 0 ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : EBits.getPieceSouthEdge(board[x][y]);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetLeftEdge(int x, int y) {
        return x >= width ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : EBits.getPieceWestEdge(board[x][y]);
    }

    private boolean hasPiece(int x, int y) {
        return x < 0 || x >= width || y < 0 || y >= height || EBits.getPiece(board[x][y]) == -1;
    }

    private void visit9(int origoX, int origoY, BiConsumer<Integer, Integer> visitor) {
        for (int y = origoY-1 ; y <= origoY+1 ; y++) {
            if (y < 0 || y >= height) {
                continue;
            }
            for (int x = origoX-1 ; x <= origoX+1 ; x++) {
                if (x < 0 || x >= width) {
                    continue;
                }
                visitor.accept(x, y);
            }
        }
    }

    // -1 = no piece
    public int getPiece(int x, int y) {
        return EBits.getPiece(board[x][y]);
    }

    // -1 = no piece
    public int getRotation(int x, int y) {
        return EBits.getRotation(board[x][y]);
    }

    // Format from https://e2.bucas.name/
    // acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab
    public void placeUntrackedAll(String full) {
        if (full.length()/4 != width*height) {
            throw new IllegalArgumentException(
                    "Got full of length " + full.length() + " but expected " + width*height*4);
        }

        for (int y = 0 ; y < height ; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                int piece = pieces.getPieceFromString(full.substring(i, i + 4));
                int rotation = pieces.getRotationFromString(full.substring(i, i + 4));
                //System.out.println("G " + piece + " " + rotation + ": " + pieces.toDisplayString(piece, rotation));
                placeUntrackedPiece(x, y, piece, rotation);
                notifyObservers(x, y, "");
            }
        }
    }

    public String getDisplayURL() {
        // https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=
        // acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab
        StringBuilder sb = new StringBuilder();
        sb.append("https://e2.bucas.name/#puzzle=displayTest&board_w=").append(width)
                .append("&board_h=").append(height).append("&board_edges=");
        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(EBits.getPiece(board[x][y]) == -1 ?
                                  "aaaa" : // Empty
                                  pieces.toDisplayString(EBits.getPiece(board[x][y]), EBits.getPiece(board[x][y])));
            }
        }
        return sb.toString();
    }

    /**
     * @return true if the piece fits on the board (edges matches).
     */
    private boolean fits(int x, int y, int piece, int rotation) {
        long state = board[x][y];
        int outerEdge;
        return ((outerEdge = EBits.getNorthEdge(state)) == -1 || outerEdge == EBits.getPieceNorthEdge(state)) &&
               ((outerEdge = EBits.getEastEdge(state)) == -1 || outerEdge == EBits.getPieceEastEdge(state)) &&
               ((outerEdge = EBits.getSouthEdge(state)) == -1 || outerEdge == EBits.getPieceSouthEdge(state)) &&
               ((outerEdge = EBits.getWestEdge(state)) == -1 || outerEdge == EBits.getPieceWestEdge(state));
    }

    public EdgeTracker getEdgeTracker() {
        return edgeTracker;
    }

    public Field getField(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException(
                    "There is no field at (" + x + ", " + y + ") on a board of size " + width + "x" + height);
        }
        return new Field(x, y);
    }

    public int getFilledCount() {
        return width*height- freeBag.size();
    }

    public int getFreeCount() {
        return freeBag.size();
    }

    /**
     * Fields are dynamic, meaning that the pieces on the fields can change between calls to fields.
     */
    public class Field {
        private final int x;
        private final int y;

        public Field(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getPiece() {
            return EBits.getPiece(board[x][y]);
        }

        public int getRotation() {
            return EBits.getRotation(board[x][y]);
        }

        public boolean hasPiece() {
            return board[x][y] != -1;
        }
        
        public boolean isFree() {
            return board[x][y] == -1;
        }
        
        public int getTopEdge() {
            return isFree() ? -1 : pieces.getTop(getPiece(), getRotation());
        }
        public int getRightEdge() {
            return isFree() ? -1 : pieces.getRight(getPiece(), getRotation());
        }
        public int getBottomEdge() {
            return isFree() ? -1 : pieces.getBottom(getPiece(), getRotation());
        }
        public int getLeftEdge() {
            return isFree() ? -1 : pieces.getLeft(getPiece(), getRotation());
        }

        public int getOuterEdgeCount() {
            return (lenientGetBottomEdge(x, y-1) == -1 ? 0 : 1) +
                   (lenientGetLeftEdge(x+1, y) == -1 ? 0 : 1) +
                   (lenientGetTopEdge(x, y+1) == -1 ? 0 : 1) +
                   (lenientGetRightEdge(x-1, y) == -1 ? 0 : 1);
        }

        /**
         * @return true if the field is empty or has a piece that matches the surrounding edges.
         */
        public boolean isValid() {
            return isFree() || fits(x, y, getPiece(), getRotation());
        }


        /**
         * Checks the 4 possible rotations of the given piece to see if it fits the field.
         * @param piece
         * @return the matching rotation or -1 if no match.
         */
        public int getValidRotation(int piece) {
            for (int rotation = 0 ; rotation < 4 ; rotation++) {
                if (fits(x, y, piece, rotation)) {
                    return rotation;
                }
            }
            return -1;
        }

        public List<Piece> getBestPieces() {
            if (freeBag.isEmpty()) {
                return Collections.emptyList();
            }
            return freeBag.getBestMatching(board[x][y]).stream()
                    .map(pid -> new Piece(pid, getValidRotation(pid)))
                    .peek(p -> {
                        if (p.rotation == -1) {
                            throw new IllegalStateException("The piece " + p.piece + " at (" + x + ", " + y + ") " +
                                                            "could not be rotated into place");
                        }
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return "field(" + x + ", " + y + ")";
        }
    }

    public static class Pair<S, T> {
        public final S left;
        public final T right;

        public Pair(S left, T right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "[" + left + ", " + right + "]";
        }
    }

    /**
     * Specific Piece representation;
     */
    public static class Piece {
        public final int piece;
        public final int rotation;
        public final int compound;

        public Piece(int piece, int rotation) {
            this.piece = piece;
            this.rotation = rotation;
            compound = (rotation << 16) | (rotation & 0xFFFF);
        }

        @Override
        public String toString() {
            return "piece(id=" + piece + ", r=" + rotation + ")";
        }

    }

    /**
     * Register an observer of board changes.
     * @param observer called when the board changes.
     */
    public synchronized void registerObserver(Observer observer) {
        observers.add(observer);
    }

    /**
     * Unregisters a previously registered board change observer.
     * @param observer an observer previously added with {@link #registerObserver(Observer)}.
     * @return true if the observer was previously registered, else false.
     */
    public synchronized boolean unregisterObserver(Observer observer) {
        boolean wasThere = observers.remove(observer);
        log.debug(wasThere ?
                          "Unregistered board update observer {}" :
                          "Attempted to unregister configuration update observer {} but is was not found",
                  observer);
        return wasThere;
    }

    /**
     * Notify all observers that the field at position {@code (x, y)} was updated.
     */
    private void notifyObservers(int x, int y, String label) {
        observers.forEach(o -> o.boardChanged(x, y, label));
    }

    /**
     * Functional equivalent of {@code BiConsumer<Integer, Integer>} with a less generic method name, to support
     * registering observers with {@code registerObserver(this)} instead of {@code registerObserver(this::boardChanged}.
     */
    @FunctionalInterface
    public interface Observer {
        void boardChanged(int x, int y, String label);
    }

}
