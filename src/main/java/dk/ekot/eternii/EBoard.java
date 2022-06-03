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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.ekot.eternii.EPieces.NULL_E;

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
    private final Set[][] possible; // TODO: Figure out how to hack around the "no generic array creation"-limitation
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
        this.possible = new Set[width][height];
        updateOuterEdgesAll();
        freeBag = new PieceTracker(pieces);
/*        pieces.allPieces()
                .boxed()
                .peek(piece -> updatePieceTracking(piece, 1))
                .forEach(this.freeBag::add);*/
//        log.debug("EdgeTracker blank: " + getEdgeTracker());
        updateEdgeTrackerAll(-1);
        updatePossibleAll(); // Must be after freeBag construction
//        log.debug("EdgeTracker after EBoard construction: " + getEdgeTracker());
    }

    /**
     * Creates a blank 16x16 board where the free pieces are from the Eternii set.
     * @param clues if true, 5 clue pieces are placed.
     * @return a board ready for filling.
     */
    public static EBoard createEterniiBoard(boolean clues) {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        if (clues) {
            pieces.processEterniiClues(board::placePiece);
        }
        return board;
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
        if (piece == EPieces.NULL_P) {
            throw new IllegalStateException("removePiece(" + x + ", " + y + ") called but the field has no piece");
        }
        // Remove piece from board
        updateTracker9(x, y, +1);
        board[x][y] = EBits.BLANK_STATE;
        //updateOuterEdges9(x, y);
        updateSurroundingEdgesAndSelf(x, y);
        updateTracker9(x, y, -1);

        // Register piece as free
        updatePieceTracking(piece, +1);
        freeBag.add(piece);
        if (!updatePossible9(x, y)) {
            log.warn("removePiece(" + x + ", " + y + "): Registered at least one field with no possible pieces");
        }
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

    public boolean placePiece(int x, int y, int piece) {
        return placePiece(x, y, piece, getValidRotationFailing(x, y, piece), "");
    }
    public boolean placePiece(int x, int y, int piece, int rotation) {
        return placePiece(x, y, piece, rotation, "");
    }
    /**
     * Positions the given piece on the board, updating the tracker and removing the piece from the free bag.
     * @return if the positioning resulted in a negative tracker and was rolled back.
     */
    public boolean placePiece(int x, int y, int piece, int rotation, String label) {
//        System.out.printf("placePiece(x=%d, y=%d, piece=%d, rotation=%d, label='%s')\n",x, y, piece, rotation, label);

        if (EBits.hasPiece(board[x][y])) {
            throw new IllegalStateException(
                    "placePiece(" + x + ", " + y + ", ...) called but the field already had a piece");
        }
        // Remove surrounding registers
        updateTracker9(x, y, +1);
        board[x][y] = EBits.setPieceFull(board[x][y], piece, rotation, pieces.getEdges(piece, rotation));
        //System.out.printf("\nBefore(%d, %d): %s\n", x, y, EBits.toString(board[x][y]));
        //updateOuterEdges9(x, y);
        updateSurroundingEdgesAndSelf(x, y);
        if (!updatePossible9(x, y)) { // Rollback
            board[x][y] = EBits.BLANK_STATE;
            //updateOuterEdges9(x, y);
            updateSurroundingEdgesAndSelf(x, y);
            updatePossible9(x, y); // TODO: React on false
            updateTracker9(x, y, -1);
            return false;
//            log.warn("placePiece(" + x + ", " + y + ", piece=" + piece + ", rotation=" + rotation +
//                     "): Registered at least one field with no possible pieces");
        }
        //System.out.printf("After (%d, %d): %s\n", x, y, EBits.toString(board[x][y]));
        //System.out.printf("Origo (%d, %d): %s\n", 0, 0, EBits.toString(board[0][0]));
/*        if (y > 0) {
            if (EBits.getPieceSouthEdge(board[x][y-1]) != EBits.getNorthEdge(board[x][y])) {
                System.out.printf("piece(%d, %d) has outer n=%s but (%d, %d) has inner s=%s\n",
                                  x, y, EBits.getNorthEdge(board[x][y-1]), x, y-1, EBits.getPieceSouthEdge(board[x][y-1]));
                throw new IllegalStateException();
            }
        }
        if (x > 0) {
            System.out.printf("Piece(x-1) e=%d, Outer(x) w=%d\n", EBits.getPieceEastEdge(board[x-1][y]), EBits.getWestEdge(board[x][y]));
            if (EBits.getPieceEastEdge(board[x-1][y]) != EBits.getWestEdge(board[x][y])) {
                System.out.printf("piece(%d, %d) has outer w=%s but (%d, %d) has inner s=%s",
                                  x, y, EBits.getWestEdge(board[x][y]), x, y-1, EBits.getPieceEastEdge(board[x-1][y]));
                throw new IllegalStateException();
            }
        }*/
        if (!updateTracker9(x, y, -1)) {
            // At least one tracker is negative so we rollback
            updateTracker9(x, y, +1);
            board[x][y] = EBits.BLANK_STATE;
            //updateOuterEdges9(x, y);
            updateSurroundingEdgesAndSelf(x, y);
            updatePossible9(x, y); // TODO: React on false
            updateTracker9(x, y, -1);
            return false;
        }
        if (!updatePieceTracking(piece, -1)) {
            // At least one tracker is negative so we rollback
            updatePieceTracking(piece, +1);
            updateTracker9(x, y, +1);
            board[x][y] = EBits.BLANK_STATE;
            //updateOuterEdges9(x, y);
            updateSurroundingEdgesAndSelf(x, y);
            updatePossible9(x, y); // TODO: React on false
            updateTracker9(x, y, -1);
            return false;
        }
        if (!freeBag.remove(piece)) {
           throw new IllegalStateException("Tried removing piece " + piece + " from the free bag but it was not there");
        }
        notifyObservers(x, y, label);
        //sanityCheckAll();
        //checkEdgeTracker();
        //checkPossible(); // TODO: Remove this sanity check
        return true;
    }

    /**
     * Updates possible pieces for all fields.
     * @return false if at least 1 field does not have any possible pieces.
     */
    private boolean updatePossibleAll() {
        AtomicBoolean ok = new AtomicBoolean(true);
        visitAll((x, y) -> ok.set(ok.get() & updatePossible(x, y)));
        return ok.get();
    }

    /**
     * Updates possible pieces for the 9 nearest fields.
     * @return false if at least 1 field does not have any possible pieces.
     */
    private boolean updatePossible9(int origoX, int origoY) {
        AtomicBoolean ok = new AtomicBoolean(true);
        visit9(origoX, origoY, (x, y) -> ok.set(ok.get() & updatePossible(x, y)));
        return ok.get();
    }

    /**
     * Updates possible pieces for the field.
     * @return false if there are no possible pieces.
     */
    private boolean updatePossible(int x, int y) {
        if (EBits.hasPiece(board[x][y])) {
            return true;
        }
        possible[x][y] = freeBag.getBestMatching(board[x][y]);
        return !possible[x][y].isEmpty();
    }

    private void updateOuterEdgesAll() {
        visitAll(this::updateOuterEdges);
    }

    private void updateOuterEdges9(int origoX, int origoY) {
        visit9(origoX, origoY, this::updateOuterEdges);
    }

    private void updateOuterEdges(Field field) {
        updateOuterEdges(field.getX(), field.getY());
    }
    private void updateOuterEdges(int x, int y) {
        int topEdge = lenientGetBottomEdge(x, y-1);
        int rightEdge = lenientGetLeftEdge(x+1, y);
        int bottomEdge = lenientGetTopEdge(x, y+1);
        int leftEdge = lenientGetRightEdge(x-1, y);
        //    System.out.printf("OE(%d, %d): n=%d, e=%d, s=%d, w=%d\n", x, y, topEdge, rightEdge, bottomEdge, leftEdge);
        board[x][y] = EBits.setAllEdges(board[x][y], topEdge, rightEdge, bottomEdge, leftEdge);
/*        if (x == 1 && y == 0) {
            if (EBits.getPieceEastEdge(board[x-1][y]) != EBits.getWestEdge(board[x][y])) {
                System.out.printf("piece(%d, %d) has outer w=%s but (%d, %d) has inner s=%s",
                                  x, y, EBits.getWestEdge(board[x][y]), x, y-1, EBits.getPieceEastEdge(board[x-1][y]));
                throw new IllegalStateException();
            }
        }
        */
    }
    private void updateSurroundingEdgesAndSelf(int x, int y) {
        final long state = board[x][y];
        long t = 0;
        if (x > 0) { // To the West
            t = EBits.setEastEdge(board[x - 1][y], EBits.getPieceWestEdge(state));
            board[x - 1][y] = EBits.updateDefinedEdges(t);
        }
        if (x < width-1) { // To the East
            t = EBits.setWestEdge(board[x+1][y], EBits.getPieceEastEdge(state));
            board[x+1][y] = EBits.updateDefinedEdges(t);
        }
        if (y > 0) { // To the North
            t = EBits.setSouthEdge(board[x][y-1], EBits.getPieceNorthEdge(state));
            board[x][y-1] = EBits.updateDefinedEdges(t);
        }
        if (y < height-1) { // To the South
            t = EBits.setNorthEdge(board[x][y+1], EBits.getPieceSouthEdge(state));
            board[x][y+1] = EBits.updateDefinedEdges(t);
        }
        updateOuterEdges(x, y);
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
            allOK.set(allOK.get() & updateTracker(x, y, delta));
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
        if (EBits.hasPiece(board[x][y])) { // No action if occupied
            return true;
        }
/*        int n = lenientGetBottomEdge(x, y-1);
        int e = lenientGetLeftEdge(x+1, y);
        int s = lenientGetTopEdge(x, y+1);
        int w = lenientGetRightEdge(x-1, y);
        return edgeTracker.add(n, e, s, w, delta);

        */
        return edgeTracker.add(EBits.getAllEdges(board[x][y]), delta);
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


    public void sanityCheckAll() {
        checkFree();
        checkEdges();
        checkInner();
        checkEdgeTracker();
        checkPossible();
    }

    private void checkEdgeTracker() {
        edgeTracker.checkForNegative();
    }

    // Checks that all free field has at least 1 possible piece
    private void checkPossible() {
        streamAllFields().filter(Field::isFree).forEach(field -> {
            if (field.getBestPiecesNonRotating().isEmpty()) {
                throw new IllegalStateException(field + " has no possible pieces");
            }
        });
    }

    private void checkEdges() {
        streamAllFields().filter(Field::hasPiece).forEach(field -> {
            int x = field.getX();
            int y = field.getY();
            long state = getState(x, y);
            int topEdge = lenientGetBottomEdge(x, y-1);
            int rightEdge = lenientGetLeftEdge(x+1, y);
            int bottomEdge = lenientGetTopEdge(x, y+1);
            int leftEdge = lenientGetRightEdge(x-1, y);
            int markedTopEdge = EBits.getNorthEdge(state);
            int markedRightEdge = EBits.getEastEdge(state);
            int markedBottomEdge = EBits.getSouthEdge(state);
            int markedLeftEdge = EBits.getWestEdge(state);
            if ((topEdge != markedTopEdge) || (rightEdge != markedRightEdge) ||
                (bottomEdge != markedBottomEdge) || (leftEdge != markedLeftEdge)) {
                throw new IllegalStateException(String.format(
                        Locale.ROOT, "Edges mismatch at (%d, %d): Observed vs. marked: n=%d,%d, e=%d,%d, s=%d,%d, w=%d,%d. board=" + getDisplayURL(),
                        x, y, topEdge, markedTopEdge, rightEdge, markedRightEdge,
                        bottomEdge, markedBottomEdge, leftEdge, markedLeftEdge));
            }
        });
    }
    private void checkInner() {
        streamAllFields().filter(Field::isFree).forEach(field -> {
            long state = getState(field.getX(), field.getY());
            int n = EBits.getPieceNorthEdge(state);
            int e = EBits.getPieceEastEdge(state);
            int s = EBits.getPieceSouthEdge(state);
            int w = EBits.getPieceWestEdge(state);
            if (n != NULL_E || e != NULL_E || s != NULL_E || w != NULL_E) {
                throw new IllegalStateException(String.format(
                        Locale.ROOT, "Field (%d, %d) marked as free but had edges n=%d, e=%d, s=%d, w=%d. board=%s",
                        field.getX(), field.getY(), n, e, s, w, getDisplayURL()));
            }
        });
    }

    private void checkFree() {
        long free = streamAllFields().filter(Field::isFree).count();
        if (free != freeBag.size()) {
            throw new IllegalStateException("Piece count mismatch: free " + free + " != freeBag " + freeBag.size());
        }
    }

    /**
     * All fields from (origoX-1, origoY-1) to (origoX+1, origoY+1) that are on the board.
     */
    private Stream<Field> streamValidFields9(int origoX, int origoY) {
        List<Field> fields = new ArrayList<>(9);
        visit9(origoX, origoY, (x, y) -> fields.add(new Field(x, y)));
        return fields.stream();
    }
    private void visit9(int origoX, int origoY, BiConsumer<Integer, Integer> visitor) {
        for (int y = Math.max(0, origoY-1) ; y <= Math.min(height-1, origoY+1) ; y++) {
            for (int x = Math.max(0, origoX-1) ; x <= Math.min(width-1, origoX+1) ; x++) {
                visitor.accept(x, y);
            }
        }
    }
    public void visitAll(BiConsumer<Integer, Integer> visitor) {
        for (int y = 0 ; y < height ;y++) {
            for (int x = 0 ; x < width ; x++) {
                visitor.accept(x, y);
            }
        }
    }


    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and NULL_E if no piece.
     */
    private int lenientGetTopEdge(int x, int y) {
        return y >= height ? EPieces.EDGE_EDGE :
                !hasPiece(x, y) ? (int)NULL_E : EBits.getPieceNorthEdge(board[x][y]);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetRightEdge(int x, int y) {
        return x < 0 ? EPieces.EDGE_EDGE :
                !hasPiece(x, y) ? (int)NULL_E : EBits.getPieceEastEdge(board[x][y]);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and NULL_E if no piece.
     */
    private int lenientGetBottomEdge(int x, int y) {
        return y < 0 ? EPieces.EDGE_EDGE :
                !hasPiece(x, y) ? (int)NULL_E : EBits.getPieceSouthEdge(board[x][y]);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetLeftEdge(int x, int y) {
        return x >= width ? EPieces.EDGE_EDGE :
                !hasPiece(x, y) ? (int)NULL_E : EBits.getPieceWestEdge(board[x][y]);
    }

    private boolean hasPiece(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && EBits.hasPiece(board[x][y]);
    }

    // NULL_E = no piece
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
                // Empty
                sb.append(EBits.hasPiece(board[x][y]) ?
                                  pieces.toDisplayString(EBits.getPiece(board[x][y]), EBits.getRotation(board[x][y])) :
                                  "aaaa");
            }
        }
        return sb.toString();
    }

    public int getValidRotationFailing(int x, int y, int piece) {
        for (int rotation = 0 ; rotation < 4 ; rotation++) {
            if (fits(x, y, piece, rotation)) {
                return rotation;
            }
        }
        long state = board[x][y];
        String message = "The piece " + piece + "(" + pieces.toDisplayString(piece, 0) + ") at (" + x + ", " + y +
                         ") could not be rotated to fit." +
                         "\nabove=" + lenientPieceDisplay(x, y-1) +
                         "\nright=" + lenientPieceDisplay(x+1, y) +
                         "\nbelow=" + lenientPieceDisplay(x, y+1) +
                         "\nleft =" + lenientPieceDisplay(x-1, y) +
                         "\nexpected outer n=" + pieces.edgeToString(EBits.getNorthEdge(state)) +
                         " e=" + pieces.edgeToString(EBits.getEastEdge(state)) +
                         " s=" + pieces.edgeToString(EBits.getSouthEdge(state)) +
                         " w=" + pieces.edgeToString(EBits.getWestEdge(state)) +
                         "\nboard = " + getDisplayURL();

        throw new IllegalStateException(message);
    }
    private String lenientPieceDisplay(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            int piece = EBits.getPiece(board[x][y]);
            if (piece != EPieces.NULL_P) {
                return pieces.toDisplayString(piece, EBits.getRotation(board[x][y]));
            }
        }
        return "N/A";
    }

    /**
     * @return true if the piece fits on the board (edges matches).
     */
    private boolean fits(int x, int y, int piece, int rotation) {
        // TODO: Optimize this by maskind and making direct comparison of the bits for inner & outer
        // if (((innerEdges >> 32) & getOnlyDefinedMask(state)) == outerEdges)
        long state = board[x][y];
        long innerEdges = pieces.getEdges(piece, rotation);
        int outerEdge;
        return ((outerEdge = EBits.getNorthEdge(state)) == NULL_E || outerEdge == EBits.getPieceNorthEdge(innerEdges)) &&
               ((outerEdge = EBits.getEastEdge(state)) ==  NULL_E || outerEdge == EBits.getPieceEastEdge(innerEdges)) &&
               ((outerEdge = EBits.getSouthEdge(state)) == NULL_E || outerEdge == EBits.getPieceSouthEdge(innerEdges)) &&
               ((outerEdge = EBits.getWestEdge(state)) ==  NULL_E || outerEdge == EBits.getPieceWestEdge(innerEdges));
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

    public long getState(int x, int y) {
        return board[x][y];
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
            return EBoard.this.hasPiece(x, y);
        }
        
        public boolean isFree() {
            return !EBoard.this.hasPiece(x, y);
        }

/*        public int getTopEdge() {
            return isFree() ? (int)NULL_E : pieces.getTop(getPiece(), getRotation());
        }
        public int getRightEdge() {
            return isFree() ? (int)NULL_E : pieces.getRight(getPiece(), getRotation());
        }
        public int getBottomEdge() {
            return isFree() ? (int)NULL_E : pieces.getBottom(getPiece(), getRotation());
        }
        public int getLeftEdge() {
            return isFree() ? (int)NULL_E : pieces.getLeft(getPiece(), getRotation());
        }
  */
        public int getType() {
            return pieces.getType(getPiece());
        }

        public int getOuterEdgeCount() {
            return EBits.countDefinedEdges(board[x][y]);
/*            return (lenientGetBottomEdge(x, y-1) == NULL_E ? 0 : 1) +
                   (lenientGetLeftEdge(x+1, y) == NULL_E ? 0 : 1) +
                   (lenientGetTopEdge(x, y+1) == NULL_E ? 0 : 1) +
                   (lenientGetRightEdge(x-1, y) == NULL_E ? 0 : 1);*/
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
            return EBoard.this.getValidRotationFailing(x, y, piece);
        }
        // TODO: Switch to delayed rotation check and just return getBestMatching
        @Deprecated
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

        @SuppressWarnings("unchecked")
        public Set<Integer> getBestPiecesNonRotating() {
            return (Set<Integer>)possible[x][y];
        }
        // TODO: Verify the above method works properly
        public Set<Integer> getBestPiecesNonRotatingOld() {
            return freeBag.getBestMatching(board[x][y]);
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
