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
package dk.ekot.ibm;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * https://www.research.ibm.com/haifa/ponderthis/challenges/January2021.html
 */
public class VaccineRobot {
    private static Log log = LogFactory.getLog(VaccineRobot.class);

    // Overall principle is that each tile on the grid holds bits for its state

    // 0, 1 & 2 are simply received vaccine doses
    public static void main(String[] args) {
//        threaded(1, 4, 4, 3);
 //       threaded(8, 4, 200, 3, true);
        //threaded(8, 93, 200, 3);
        //threaded(8, 4, 200, 3);
        timeFlat(); // ~10s
        //empties();

//        test4();

//        Grid grid = new Grid(4);
//        grid.mark(new Pos(1, 0));
//        System.out.println(grid.fullRun());
//        System.out.println(grid);
    }

    private static void timeFlat() {
        long startMS = System.currentTimeMillis();
        threaded(1, 41, 45, 3);
        System.out.println("Total time: " + (System.currentTimeMillis()-startMS));
    }

    private static void empties() {
        for (int side = 4 ; side <= 50 ; side++) {
            FlatGrid grid = new FlatGrid(side);
            grid.fullRun();
            System.out.println(grid);
        }
    }

    private static void threaded(int threads, int minSide, int maxSide, int maxAntis) {
        threaded(threads, minSide, maxSide, maxAntis, false);
    }
    private static void threaded(int threads, int minSide, int maxSide, int maxAntis, boolean compare) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<String>> jobs = new ArrayList<>(maxSide-minSide+1);
        for (int side = minSide ; side <= maxSide ; side++) {
            final int finalSide = side;
            jobs.add(executor.submit(() -> {
                for (int antis = 1 ; antis <= maxAntis ; antis++) {
                    String base = null;
                    if (compare) {
                        base = systematic(finalSide, finalSide, antis);
                    }
                    String result = systematicFlat(finalSide, finalSide, antis);
                    if (result != null) {
                        return compare ? base + result : result;
                    }
                }
                return null;
            }));
        }
        jobs.forEach(job -> {
            try {
                System.out.print(job.get());
            } catch (Exception e) {
                log.error("Failed job", e);
            }
        });
        executor.shutdown();
    }

    private static void test4() {
        Grid grid = new Grid(4);
        grid.setMarks(new Pos(3, 0));
//        System.out.println(grid.fullRun());
//        System.out.println(grid);

        FlatGrid flat = new FlatGrid(4);
        flat.setMarks(new Pos(3, 0));
//        System.out.println(flat.fullRun());
//        System.out.println(flat);

        while (grid.hasNext()) {
            System.out.println("****************");
            grid.next();
            System.out.println(grid);
            System.out.println(grid.hasNext() + " -- " + grid.allImmune());
            flat.next();
            System.out.println(flat);
            System.out.println(flat.hasNext() + " " + grid.allImmune());
        }
    }

    private static String systematic(int width, int height, int antiCount) {
        Grid grid = new Grid(width, height);
        Pos[] antis = new Pos[antiCount];
        for (int i = 0 ; i < antis.length ; i++) {
            antis[i] = new Pos();
        }
        return systematic(grid, antis, 0, 0);
    }
    private static String systematic(Grid grid, Pos[] antis, int antiIndex, int minPos) {
        int all = grid.width*grid.height;
//        if (antiIndex == 1) {
//            System.out.println(minPos + "/" + (all-1));
//        }
        for (int p = minPos ; p < all-(antis.length-antiIndex-1) ; p++) {
            antis[antiIndex].x =p % grid.height;
            antis[antiIndex].y =p / grid.height;
            if (antiIndex < antis.length-1) {
                String result = systematic(grid, antis, antiIndex + 1, p + 1);
                if (result != null) {
                    return result;
                }
                continue;
            }

            grid.clear();
            grid.setMarks(antis);
            if (grid.fullRun()) {
                return String.format(
                        Locale.ENGLISH, "Grid(%3d, %3d) moves=%6d, ms=%6d, antis=%d: %s%n",
                        grid.width, grid.height, grid.move, grid.lastRunMS, antis.length, Arrays.asList(antis));
            }
        }
        return null;
    }

    private static String systematicFlat(int width, int height, int antiCount) {
        FlatGrid empty = new FlatGrid(width, height);
        empty.fullRun(); // We know there must be at least 1 antiCount in one of the non-zero positions

        FlatGrid grid = new FlatGrid(width, height);
        return systematicFlat(grid, empty, new int[antiCount], 0, 0);
    }
    private static String systematicFlat(FlatGrid grid, FlatGrid baseEmpty, final int[] antis, int antiIndex, int minPos) {
        final int all = grid.width*grid.height;
//        if (antiIndex == 1) {
//            System.out.println(minPos + "/" + (all-1));
//        }

        for (int antiPos = minPos ; antiPos < all-(antis.length-antiIndex-1) ; antiPos++) {
            antis[antiIndex] = antiPos;
            if (antiIndex < antis.length-1) { // More antis to go
                String result = systematicFlat(grid, baseEmpty, antis, antiIndex + 1, antiPos + 1);
                if (result != null) {
                    return result;
                }
                continue;
            }

            // Reached the bottom

            if (!baseEmpty.atLeastOneAntiOnMarked(antis)) {
                continue; // No need to try as all the anties ar only on non-visited places
            }

            grid.clear();
            grid.setMarks(antis);
            if (grid.fullRun()) {
                return String.format(
                        Locale.ENGLISH, "Flat(%3d, %3d) moves=%6d, ms=%6d, antis=%d: %s%n",
                        grid.width, grid.height, grid.move, grid.lastRunMS, antis.length, toPosList(antis, grid.width, grid.height));
            }
        }
        // TODO: Also handle antis.length == 0
        return null;
    }

    private static List<Pos> toPosList(int[] antis, int width, int height) {
        List<Pos> poss = new ArrayList<>(antis.length);
        for (int anti : antis) {
            poss.add(new Pos(anti % width, anti / width));
        }
        return poss;
    }

    private static class Grid {
        public static final int IMMUNE = 2;
        public static final int COUNT = 3;

        public static final int UP = 4;
        public static final int RIGHT = 8;
        public static final int DOWN = 16;
        public static final int LEFT = 32;

        public static final int ANTI = 64;

        private final int width;
        private final int height;
        private final int[][] grid;

        private int direction = UP;
        private final Pos pos = new Pos();
        private int move = 0;
        private int lastUpdate = 0;
        private int marks = 0;
        private int immune = 0; // Fields vaccinated twice
        private final int maxNonChangingMoves;
        private long lastRunMS = 0;

        public Grid(int edge) {
            this(edge, edge);
        }

        public Grid(int width, int height) {
            this.width = width;
            this.height = height;
            grid = new int[width][height];
            maxNonChangingMoves = width*height;
        }

        public void setMarks(Pos... antis) {
            clear();
            marks = 0;
            addMarks(antis);
        }

        public void addMarks(Pos... antis) {
            for (Pos anti : antis) {
                grid[anti.x][anti.y] |= ANTI;
            }
            marks += antis.length;
        }

        public void clear() {
            for (int[] rows : grid) {
                Arrays.fill(rows, 0);
            }
            immune = 0;
            move = 0;
            lastUpdate = 0;
            direction = UP;
            pos.set(0, 0);
            grid[0][0] |= direction;
        }

        public boolean fullRun() {
            lastRunMS -= System.currentTimeMillis();
            while (hasNext()) {
                next();
            }
            lastRunMS += System.currentTimeMillis();
            return allImmune();
        }

        public boolean hasNext() {
            return !allImmune() && move - maxNonChangingMoves < lastUpdate ;
        }

        public void next() {
            grid[pos.x][pos.y] |= direction; // Mark current direction on the field
            int field = grid[pos.x][pos.y];
            //System.out.println(pos + ": " + field);
            //System.out.println(this);
            if ((field & ANTI) == ANTI ||
                (field & COUNT) == IMMUNE) {
                // No marking
            } else {
                grid[pos.x][pos.y]++;
            }

            if ((field & ANTI) == ANTI) {
                counterClockwise();
                forward();
            } else if ((field & COUNT) == 0) {
                lastUpdate = move;
                clockwise();
                forward();
            } else if ((field & COUNT) == 1) {
                lastUpdate = move;
                immune++;
                counterClockwise();
                forward();
            } else if ((field & COUNT) == IMMUNE) {
                forward();
            }
            move++;
        }

        private void forward() {
            switch (direction) {
                case UP: {
                    pos.y--;
                    if (pos.y < 0) {
                        pos.y = height - 1;
                    }
                    break;
                }
                case RIGHT: {
                    pos.x++;
                    if (pos.x == width) {
                        pos.x = 0;
                    }
                    break;
                }
                case DOWN: {
                    pos.y++;
                    if (pos.y == height) {
                        pos.y = 0;
                    }
                    break;
                }
                case LEFT: {
                    pos.x--;
                    if (pos.x < 0) {
                        pos.x = width - 1;
                    }
                    break;
                }
            }
        }

        private void counterClockwise() {
            switch (direction) {
                case UP: {
                    direction = LEFT;
                    break;
                }
                case LEFT: {
                    direction = DOWN;
                    break;
                }
                case DOWN: {
                    direction = RIGHT;
                    break;
                }
                case RIGHT: {
                    direction = UP;
                    break;
                }
            }
        }

        private void clockwise() {
            switch (direction) {
                case UP: {
                    direction = RIGHT;
                    break;
                }
                case RIGHT: {
                    direction = DOWN;
                    break;
                }
                case DOWN: {
                    direction = LEFT;
                    break;
                }
                case LEFT: {
                    direction = UP;
                    break;
                }
            }
        }

        public boolean canMove() {
            int field = grid[pos.x][pos.y];
            return (field & direction) != direction || // New direction
                   (
                           (field & ANTI) != ANTI && // Not anti
                           (field & COUNT) != IMMUNE // Not yet immune
                   );
        }

        public int countVaccinated() {
            int vaccinated = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    vaccinated += (grid[x][y] & COUNT) == IMMUNE ? 1 : 0;
                }
            }
            return vaccinated;
        }

        public boolean allImmune() {
            return immune + marks == width * height;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < width; y++) {
                for (int x = 0; x < width; x++) {
                    int field = grid[x][y];
                    sb.append(((field & ANTI) == ANTI) ? "B" : field & COUNT);
                    if (pos.equals(x, y)) {
                        switch (direction) {
                            case UP: {
                                sb.append("↑");
                                break;
                            }
                            case RIGHT: {
                                sb.append("→");
                                break;
                            }
                            case DOWN: {
                                sb.append("↓");
                                break;
                            }
                            case LEFT: {
                                sb.append("←");
                                break;
                            }
                        }
                    } else {
                        sb.append(" ");
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }
    private static class FlatGrid {

        public static final int UP = 0;
        public static final int RIGHT = 1;
        public static final int DOWN = 2;
        public static final int LEFT = 3;

        public static final int ANTI = 15;

        private final int width;
        private final int height;
        private final int total;
        private final int[] grid;

        private int direction = UP;
        private int pos = 0;
        private int posX = 0;
        private int posY = 0;
        private int move = 0;
        private int lastUpdate = 0;
        private int marks = 0;
        private int immune = 0; // Fields vaccinated twice
        private final int maxNonChangingMoves;
        private long lastRunMS = 0;

        public FlatGrid(int edge) {
            this(edge, edge);
        }

        public FlatGrid(int width, int height) {
            this.width = width;
            this.height = height;
            total = width*height;
            grid = new int[total];
            maxNonChangingMoves = total;
        }

        public void setMarks(Pos... antis) {
            clear();
            marks = 0;
            addMarks(antis);
        }

        public void addMarks(Pos... antis) {
            for (Pos anti : antis) {
                grid[anti.x + anti.y * width] = ANTI;
            }
            marks += antis.length;
        }

        public void setMarks(int... antis) {
            clear();
            marks = 0;
            addMarks(antis);
        }

        public void addMarks(int... antis) {
            for (int anti : antis) {
                grid[anti] = ANTI;
            }
            marks += antis.length;
        }

        public void clear() {
            Arrays.fill(grid, 0);
            immune = 0;
            move = 0;
            lastUpdate = 0;
            direction = UP;
            pos = 0;
            posX = 0;
            posY = 0;
        }

        public boolean fullRun() {
            lastRunMS -= System.currentTimeMillis();
            while (hasNext()) {
                next();
            }
            lastRunMS += System.currentTimeMillis();
            return allImmune();
        }

        public boolean hasNext() {
            return !allImmune() && move - maxNonChangingMoves < lastUpdate;
        }

        public void next() {
            switch (grid[pos]) {
                case 0: {
                    lastUpdate = move;
                    grid[pos]++;
                    clockwise();
                    forward();
                    break;
                }
                case 1: {
                    lastUpdate = move;
                    grid[pos]++;
                    immune++;
                    counterClockwise();
                    forward();
                    break;
                }
                case 2: {
                    forward();
                    break;
                }
                case ANTI: {
                    counterClockwise();
                    forward();
                    break;
                }
                default:
                    throw new IllegalStateException("grid[" + pos + "] was " + grid[pos]);
            }
            move++;
        }

        private void forward() {
            switch (direction) {
                case UP: {
                    if ((posY -= 1) < 0) {
                        posY = height-1;
                    }
                    break;
                }
                case RIGHT: {
                    posX++;
                    if (posX == width) {
                        posX = 0;
                    }
                    break;
                }
                case DOWN: {
                    if ((posY += 1) == height) {
                        posY = 0;
                    }
                    break;
                }
                case LEFT: {
                    posX--;
                    if (posX < 0) {
                        posX = width-1;
                    }
                    break;
                }
            }
            pos = posX + posY*width;
  //          System.out.println(this);
        }

        private void counterClockwise() {
            direction--;
            if (direction == UP - 1) {
                direction = LEFT;
            }
        }

        private void clockwise() {
            direction++;
            if (direction == LEFT + 1) {
                direction = UP;
            }
        }

        public final boolean allImmune() {
            return immune + marks == total;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < width; y++) {
                for (int x = 0; x < width; x++) {
                    int field = grid[x+y*width];
                    sb.append(field == ANTI ? "B" : field);
                    if (pos == x+y*width) {
                        switch (direction) {
                            case UP: {
                                sb.append("↑");
                                break;
                            }
                            case RIGHT: {
                                sb.append("→");
                                break;
                            }
                            case DOWN: {
                                sb.append("↓");
                                break;
                            }
                            case LEFT: {
                                sb.append("←");
                                break;
                            }
                        }
                    } else {
                        sb.append(" ");
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        public boolean atLeastOneAntiOnMarked(FlatGrid empty) {
            for (int pos = 0 ; pos < total ; pos++) {
                if (grid[pos] == ANTI && empty.grid[pos] != 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean atLeastOneAntiOnMarked(Pos[] antis) {
            for (Pos anti: antis) {
                if (grid[anti.x + anti.y*width] != 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean atLeastOneAntiOnMarked(int[] antis) {
            for (int anti: antis) {
                if (grid[anti] != 0) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private static class Pos {
        public int x = 0;
        public int y = 0;

        public Pos() { }
        public Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void set(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public boolean equals(int x, int y) {
            return this.x == x && this.y == y;
        }
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /*
    Flat(  4,   4) moves=    51, ms=     0, antis=1: [(3, 0)]
    Flat(  5,   5) moves=    89, ms=     0, antis=1: [(3, 0)]
    Flat(  6,   6) moves=   139, ms=     0, antis=1: [(1, 0)]
    Flat(  7,   7) moves=   200, ms=     0, antis=1: [(1, 0)]
    Flat(  8,   8) moves=   263, ms=     4, antis=1: [(5, 6)]
    Flat(  9,   9) moves=   319, ms=     4, antis=1: [(6, 7)]
    Flat( 10,  10) moves=   451, ms=     0, antis=1: [(9, 1)]
    Flat( 11,  11) moves=   485, ms=     2, antis=1: [(0, 4)]
    Flat( 12,  12) moves=   894, ms=     0, antis=2: [(0, 0), (9, 0)]
    Flat( 13,  13) moves=   887, ms=    33, antis=2: [(1, 0), (3, 0)]
    Flat( 14,  14) moves=   967, ms=     1, antis=2: [(1, 0), (11, 2)]
    Flat( 15,  15) moves=  1181, ms=    39, antis=1: [(10, 12)]
    Flat( 16,  16) moves=  1555, ms=     2, antis=2: [(0, 0), (0, 14)]
    Flat( 17,  17) moves=  1838, ms=    12, antis=2: [(0, 0), (16, 13)]
    Flat( 18,  18) moves=  1976, ms=   114, antis=2: [(13, 0), (0, 5)]
    Flat( 19,  19) moves=  1795, ms=    18, antis=1: [(0, 18)]
    Flat( 20,  20) moves=  2354, ms=     1, antis=2: [(0, 0), (19, 1)]
    Flat( 21,  21) moves=  2411, ms=     1, antis=2: [(0, 0), (2, 1)]
    Flat( 22,  22) moves=  3798, ms=    12, antis=2: [(1, 0), (0, 6)]
    Flat( 23,  23) moves=  3679, ms=    34, antis=1: [(22, 22)]
    Flat( 24,  24) moves=  4502, ms=    99, antis=2: [(3, 0), (23, 0)]
    Flat( 25,  25) moves=  4468, ms=    92, antis=2: [(2, 0), (21, 22)]
    Flat( 26,  26) moves=  4841, ms=   336, antis=2: [(11, 0), (0, 23)]
    Flat( 27,  27) moves=  5433, ms=    56, antis=2: [(1, 0), (0, 10)]
    Flat( 28,  28) moves=  6274, ms=    90, antis=2: [(3, 0), (27, 27)]
    Flat( 29,  29) moves=  6450, ms=    61, antis=2: [(1, 0), (2, 2)]
    Flat( 30,  30) moves=  7207, ms=   432, antis=2: [(3, 0), (4, 19)]
    Flat( 31,  31) moves=  9441, ms=   160, antis=2: [(2, 0), (24, 28)]
    Flat( 32,  32) moves=  7145, ms=    67, antis=2: [(1, 0), (2, 1)]
    Flat( 33,  33) moves=  8256, ms=     3, antis=2: [(0, 0), (28, 0)]
    Flat( 34,  34) moves=  8871, ms=  1010, antis=2: [(9, 0), (30, 33)]
    Flat( 35,  35) moves=  8643, ms=    88, antis=2: [(0, 0), (0, 32)]
    Flat( 36,  36) moves=  9924, ms=   755, antis=2: [(6, 0), (33, 0)]
    Flat( 37,  37) moves=  9216, ms=    75, antis=2: [(0, 0), (0, 32)]
    Flat( 38,  38) moves= 11893, ms=   737, antis=2: [(4, 0), (3, 37)]
    Flat( 39,  39) moves= 14882, ms=    78, antis=2: [(1, 0), (4, 0)]
    Flat( 40,  40) moves= 12784, ms=   736, antis=2: [(3, 0), (0, 8)]
    Flat( 41,  41) moves= 16824, ms=  8510, antis=2: [(32, 0), (32, 11)]
    Flat( 42,  42) moves= 17468, ms=  2769, antis=2: [(11, 0), (0, 33)]
    Flat( 43,  43) moves= 18038, ms=   721, antis=2: [(4, 0), (0, 10)]
    Flat( 44,  44) moves= 19382, ms=  2838, antis=2: [(8, 0), (1, 43)]
    Flat( 45,  45) moves= 16325, ms=  9756, antis=2: [(37, 0), (0, 11)]
    Flat( 46,  46) moves= 18941, ms=  3308, antis=2: [(9, 0), (0, 11)]
    Flat( 47,  47) moves= 25372, ms=  2674, antis=2: [(6, 0), (0, 10)]
    Flat( 48,  48) moves= 26220, ms=  1401, antis=2: [(6, 0), (0, 14)]
    Flat( 49,  49) moves= 24875, ms=    20, antis=2: [(0, 0), (0, 3)]
    Flat( 50,  50) moves= 24322, ms= 12552, antis=2: [(34, 0), (1, 48)]
    Flat( 51,  51) moves= 24497, ms= 13356, antis=2: [(33, 0), (0, 22)]
    Flat( 52,  52) moves= 27189, ms= 16494, antis=2: [(46, 0), (41, 3)]
    Flat( 53,  53) moves= 20581, ms=  1452, antis=2: [(2, 0), (0, 16)]
    Flat( 54,  54) moves= 30162, ms= 16998, antis=2: [(42, 0), (2, 29)]
    Flat( 55,  55) moves= 27533, ms= 14855, antis=2: [(27, 0), (0, 37)]
    Flat( 56,  56) moves= 33683, ms= 25472, antis=2: [(45, 0), (5, 49)]
    Flat( 57,  57) moves= 25755, ms=  5160, antis=2: [(11, 0), (5, 49)]
    Flat( 58,  58) moves= 41383, ms=  5915, antis=2: [(6, 0), (50, 57)]
    Flat( 59,  59) moves= 24317, ms=  4731, antis=2: [(7, 0), (0, 33)]
    Flat( 60,  60) moves= 32201, ms= 48439, antis=2: [(1, 1), (17, 37)]
    Flat( 61,  61) moves= 44500, ms=  7226, antis=2: [(7, 0), (0, 30)]
    Flat( 62,  62) moves= 48588, ms= 61918, antis=2: [(1, 1), (4, 52)]
    Flat( 63,  63) moves= 26682, ms= 30220, antis=2: [(23, 0), (0, 32)]
    Flat( 64,  64) moves= 46171, ms= 32245, antis=2: [(36, 0), (1, 7)]
    Flat( 65,  65) moves= 62404, ms= 56115, antis=2: [(50, 0), (43, 60)]
    Flat( 66,  66) moves= 57160, ms=161264, antis=2: [(65, 1), (0, 34)]
    Flat( 67,  67) moves= 49408, ms= 26218, antis=2: [(25, 0), (64, 59)]
    Flat( 68,  68) moves= 55571, ms= 77547, antis=2: [(62, 0), (43, 18)]
    Flat( 69,  69) moves= 56026, ms= 38006, antis=2: [(27, 0), (0, 55)]
    Flat( 70,  70) moves= 36513, ms=  4763, antis=2: [(2, 0), (0, 39)]
    Flat( 71,  71) moves= 48872, ms= 48111, antis=2: [(28, 0), (0, 20)]
    Flat( 72,  72) moves= 63994, ms=104741, antis=2: [(60, 0), (0, 30)]
    Flat( 73,  73) moves= 58381, ms= 22695, antis=2: [(11, 0), (64, 71)]
    Flat( 74,  74) moves= 42215, ms= 30194, antis=2: [(16, 0), (0, 31)]
    Flat( 75,  75) moves= 75372, ms= 33477, antis=2: [(15, 0), (66, 71)]
    Flat( 76,  76) moves= 56351, ms=214531, antis=2: [(3, 1), (75, 4)]
    Flat( 77,  77) moves= 46786, ms= 24363, antis=2: [(9, 0), (0, 38)]
    Flat( 78,  78) moves= 62893, ms= 70888, antis=2: [(27, 0), (0, 37)]
    Flat( 79,  79) moves= 67541, ms=363294, antis=2: [(23, 1), (0, 34)]
    Flat( 80,  80) moves= 80879, ms=824241, antis=2: [(60, 3), (0, 44)]
    Flat( 81,  81) moves= 88374, ms=291205, antis=2: [(78, 0), (21, 39)]
    Flat( 82,  82) moves= 86374, ms=164138, antis=2: [(61, 0), (0, 56)]
    Flat( 83,  83) moves= 94889, ms=604173, antis=2: [(11, 2), (0, 64)]
    Flat( 84,  84) moves= 94331, ms=417419, antis=2: [(11, 1), (69, 79)]
    Flat( 85,  85) moves= 45979, ms=278060, antis=2: [(72, 0), (0, 22)]
    Flat( 86,  86) moves=108540, ms=371177, antis=2: [(83, 0), (9, 20)]
    Flat( 87,  87) moves= 86835, ms=202368, antis=2: [(39, 0), (0, 41)]
    Flat( 88,  88) moves=119793, ms=300375, antis=2: [(70, 0), (72, 59)]
    Flat( 89,  89) moves=124968, ms=876927, antis=2: [(65, 1), (85, 80)]
    Flat( 90,  90) moves=100611, ms=150160, antis=2: [(38, 0), (0, 66)]
    Flat( 91,  91) moves=102011, ms=352257, antis=2: [(73, 0), (58, 64)]
    Flat( 92,  92) moves=126202, ms= 96744, antis=2: [(17, 0), (0, 68)]
     */

}
