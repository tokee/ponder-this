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

import com.google.common.collect.Comparators;
import dk.ekot.misc.Bitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Finds all possible valid hex (4x4 pieces) corners.
 */
public class HexCorners {
    private static final Logger log = LoggerFactory.getLogger(HexCorners.class);

    private static final Rect TL_HEX = new Rect( 0,  0,  3,  3);
    private static final Rect TR_HEX = new Rect(12 , 0, 15,  3);
    private static final Rect BL_HEX = new Rect( 0, 12,  3, 15);
    private static final Rect BR_HEX = new Rect(12, 12, 15, 15);

    private static final Path TL_HEX_FILE = new File("tl_4x4_corner.dat").toPath();
    private static final Path TR_HEX_FILE = new File("tr_4x4_corner.dat").toPath();
    private static final Path BL_HEX_FILE = new File("bl_4x4_corner.dat").toPath();
    private static final Path BR_HEX_FILE = new File("br_4x4_corner.dat").toPath();

    public static void main(String[] args) {
        dumpIndividualHexCorners();
        permutatePieceIDBitmaps();
    }

    private static void dumpIndividualHexCorners() {
        new Thread(() -> dumpValids(TL_HEX, TL_HEX_FILE)).start();
        new Thread(() -> dumpValids(TR_HEX, TR_HEX_FILE)).start();
        new Thread(() -> dumpValids(BL_HEX, BL_HEX_FILE)).start();
        new Thread(() -> dumpValids(BR_HEX, BR_HEX_FILE)).start();
    }

    private static void permutatePieceIDBitmaps() {
        long[][] tlIDs = sortBitmaps(piecesToIDs(TL_HEX, loadPieces(TL_HEX_FILE)));
        long[][] trIDs = sortBitmaps(piecesToIDs(TR_HEX, loadPieces(TR_HEX_FILE)));
        log.info("Finding unique blocks");
        AtomicLong counter = new AtomicLong(0);
        validCallback2(tlIDs, trIDs, (bitmaps) -> counter.incrementAndGet());
        System.out.println("Unique tl+tr: " + counter);
    }

    /**
     * Sort the bitmaps, with bitwise comparison, ignoring the last long.
     */
    private static long[][] sortBitmaps(long[][] bitmaps) {
        Arrays.sort(bitmaps, getBitmapComparator(4)); // 4*8*8=256
        return bitmaps;
    }

    /**
     * Compares bitmaps (arrays of longs), but only length elements.
     * @param length
     * @return
     */
    private static Comparator<long[]> getBitmapComparator(final int length) {
        return null; //(o1, o2) -> Arrays.compare(o1, 0, length, o2, 0, length);
    }

    /**
     * @param uniqueHandler
     */
    private static void validCallback2(long[][] ids1, long[][] ids2, Consumer<long[][]> uniqueHandler) {
        /*
        final long total = 1L*ids1.length;
        long c1, c2, c3, c4;
        AtomicLong uniquePairs = new AtomicLong(0);
        for (; c1 < ids1.lengthforlong[] block1: ids1) {
            ++c1;
            while ()
            for (long[] block2:ids2) {
                if (!anyMatch(block1, block2)) {

                    if (uniquePairs.incrementAndGet() % 100000 == 0) {
                        System.out.println(
                                "u=" + uniquePairs.get()/1000000 + "M, progress=" + c1.get()/1000000 + "M/" + total/1000000 + "M");
                    }
                    uniqueHandler.accept(new long[][]{block1, block2});
                }
            }
        } */
    }

    private static boolean anyMatch(long[] block1, long[] block2) {
        for (int i = 0 ; i < block1.length-1 ; i++) { // Last block is the pointer
            if ((block1[i] & block2[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dump all valid solutions inside of the rect to the outputFile.
     * Format is ints with the leftmost 32 bits of state {@link EBits}.
     * @return number of valid solutions.
     */
    public static long dumpValids(Rect rect, Path outputFile) {
        if (Files.exists(outputFile)) {
            System.out.println(outputFile + " already exists. Skipping!");
            return 0;
        }
        PieceDumper dumper = new PieceDumper(rect, outputFile);

        long solutions = countSolutions(
                board -> new WalkerRectangle(board, rect), getNonclued(rect),
                dumper);
        System.out.println("Stored " + solutions + " solutions in " + outputFile);
        return solutions;
    }

    /**
     * @return all pieces from the given pieceFile.
     */
    // TODO: Load as int[][]
    public static int[] loadPieces(Path pieceFile) {
        long fileSize;
        try {
            fileSize = Files.size(pieceFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to determine size of '" + pieceFile + "'", e);
        }
        log.info("Loading state pieces from '" + pieceFile + "'");
        try (FileInputStream fis = new FileInputStream(pieceFile.toFile()) ;
             BufferedInputStream bis = new BufferedInputStream(fis) ;
             DataInputStream in = new DataInputStream(bis)) {
            int total = (int) (fileSize / Integer.BYTES);
             int[] pieces = new int[total];
             for (int i = 0 ; i < total ; i++) {
                 pieces[i] = in.readInt();
             }
             return pieces;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load '" + pieceFile + "'", e);
        }
    }

    /**
     * Extracts all IDs for pieces, grouping by the rect area, represented as a bitmap and
     * extended with an extra long stating the original offset (measured in rect area).
     *
     * Pieces are represented as the leftmost 32 bits in EBits and have IDs 0-255.
     */
    public static long[][] piecesToIDs(Rect rect, int[] pieces) {
        if (pieces.length % rect.area != 0) {
            throw new IllegalArgumentException(
                    "The rect area " + rect.area + " is not a whole divisor of the total number of pieces " +
                    pieces.length);
        }
        final int blockSize = rect.area;
        final int blockCount = pieces.length / rect.area;
        log.info("Converting " + blockCount + " blocks @ " + blockSize + " pieces to ID bitmaps");
        long[][] blocks = new long[blockCount][];
        Bitmap bitmap = new Bitmap(256+64); // All possible pieces (not NULL) + book keeping long
        for (int b = 0 ; b < blockCount ; b++) {
            bitmap.clear();
            for (int p = 0 ; p < blockSize ; p++) {
                bitmap.set(EBits.getPiece((long) pieces[b * blockSize + p] << EBits.PIECE_EDGES_SHIFT));
            }
            bitmap.getBacking()[bitmap.getBacking().length-1] = b; // Remember the block
            blocks[b] = bitmap.getBackingCopy();
        }
        return blocks;
    }

    

    /**
     * @return number of fields in the rect without a clue piece.
     */
    public static int getNonclued(Rect rect) {
        EBoard board = getCluedBoard();
        AtomicInteger nonClued = new AtomicInteger(0);
        rect.walk((x, y) -> nonClued.addAndGet(board.getPiece(x, y) == EPieces.NULL_P ? 1 : 0));
        return nonClued.get();
    }

    public static class PieceDumper implements Consumer<EBoard> {
        private final Rect rect;
        private final FileOutputStream outStream;
        private final DataOutputStream out;
        private final int[] pieces;

        public PieceDumper(Rect rect, Path outputFile) {
            this.rect = rect;
            if (Files.exists(outputFile)) {
                throw new IllegalStateException("The output file '" + outputFile + "' already exists");
            }
            try {
                outStream = new FileOutputStream(outputFile.toFile(), false);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Unable to create stream for file '" + outputFile + "'");
            }
            out = new DataOutputStream(outStream);
            pieces = new int[rect.width*rect.height];
        }

        @Override
        public void accept(EBoard board) {
            rect.walk((x, y) -> {
                try {
                    out.writeInt((int) (board.getState(x, y) >> 32));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to write piece data", e);
                }
            });
        }

        public void close() {
            try {
                out.flush();
                out.close();
                outStream.flush();
                outStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Exception while closing", e);
            }
        }
    }

    public static long countSolutions(Function<EBoard, Walker> walkerFactory) {
        return countSolutions(walkerFactory, 4);
    }

    public static long countSolutions(Function<EBoard, Walker> walkerFactory, int max) {
        return countSolutions(walkerFactory, max, b -> {});
    }

    public static long countSolutions(Function<EBoard, Walker> walkerFactory, int max, Consumer<EBoard> solutionConsumer) {
        EBoard board = getCluedBoard();
        Walker walker = walkerFactory.apply(board);

//        new BoardVisualiser(board);
//        new BoardVisualiser(board, true);

        StatsSolver solver = new StatsSolver(board, walker, max, solutionConsumer);
        solver.run();
        return solver.getFoundSolutions();
    }

    /**
     * @return standard eternii board with clues.
     */
    private static EBoard getCluedBoard() {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        pieces.processEterniiClues((x, y, piece, rotation) -> board.placePiece(x, y, piece, rotation, ""));
        return board;
    }

}
