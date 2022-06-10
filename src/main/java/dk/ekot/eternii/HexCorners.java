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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Finds all possible valid hex (4x4 pieces) corners.
 */
public class HexCorners {
    private static final Logger log = LoggerFactory.getLogger(HexCorners.class);

    public static void main(String[] args) {
        dumpValids(new Rect(0, 0, 1, 1), new File("all_2x2_corner.dat").toPath());
    }

    /**
     * Dump all valid solutions inside of the rect to the outputFile.
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
     * @return number of fileds in the rect without a clue piece.
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
