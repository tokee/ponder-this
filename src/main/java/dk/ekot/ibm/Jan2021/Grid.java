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
package dk.ekot.ibm.Jan2021;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Arrays;

/**
 *
 */
class Grid {
    public static final int IMMUNE = 2;
    public static final int COUNT = 3;

    public static final int UP = 4;
    public static final int RIGHT = 8;
    public static final int DOWN = 16;
    public static final int LEFT = 32;

    public static final int ANTI = 64;

    public final int width;
    public final int height;
    private final int[][] grid;

    private int direction = UP;
    private final Pos pos = new Pos();
    public int move = 0;
    private int lastUpdate = 0;
    private int marks = 0;
    private int immune = 0; // Fields vaccinated twice
    private final int maxNonChangingMoves;
    public long lastRunMS = 0;

    public Grid(int edge) {
        this(edge, edge);
    }

    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
        grid = new int[width][height];
        maxNonChangingMoves = width * height;
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
        return !allImmune() && move - maxNonChangingMoves < lastUpdate;
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
