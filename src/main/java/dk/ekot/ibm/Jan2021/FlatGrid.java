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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
class FlatGrid {

    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;

    public static final int ANTI = 15;

    private static final int[] NO_ANTIS = new int[0];

    public final String name;
    public final int width;
    public final int height;
    private final List<Integer> startYs;
    private final int total;
    private final int[] grid;

    private int[] antis = new int[0];
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
        this (width, height, "Flat");
    }
    public FlatGrid(int width, int height, String name) {
        this(width, height, name, Collections.singletonList(0));
    }
    public FlatGrid(int width, int height, String name, List<Integer> startYs) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.startYs = startYs;
        total = width * height;
        grid = new int[total];
        maxNonChangingMoves = total;
    }

    public Match getMatch() {
        return new Match(name, width, height, move, lastRunMS, toString(), antis, startYs);
    }

    public void setMarks(Pos... antis) {
        setMarks(toIntAntis(antis));
    }

    public void addMarks(Pos... antis) {
        addMarks(toIntAntis(antis));
    }

    public void setMarks(int... antis) {
        marks = antis.length;
        for (int anti : antis) {
            grid[anti] = ANTI;
        }
        this.antis = this.antis.length == antis.length ? this.antis : new int[antis.length];
        System.arraycopy(antis, 0, this.antis, 0, antis.length);
    }

    public void addMarks(int... antis) {
        for (int anti : antis) {
            grid[anti] = ANTI;
        }
        marks += antis.length;
        int[] newAntis = new int[this.antis.length + antis.length];
        System.arraycopy(this.antis, 0, newAntis, 0, this.antis.length);
        System.arraycopy(antis, 0, newAntis, this.antis.length, antis.length);
        this.antis = newAntis;
    }

    private int[] toIntAntis(Pos[] antis) {
        int[] intAntis = new int[antis.length];
        for (int i = 0; i < antis.length; i++) {
            Pos anti = antis[i];
            intAntis[i] = anti.x + anti.y * width;
        }
        return intAntis;
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
                    posY = height - 1;
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
                    posX = width - 1;
                }
                break;
            }
        }
        pos = posX + posY * width;
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
                int field = grid[x + y * width];
                sb.append(field == ANTI ? "B" : field);
                if (pos == x + y * width) {
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
        for (int pos = 0; pos < total; pos++) {
            if (grid[pos] == ANTI && empty.grid[pos] != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean atLeastOneAntiOnMarked(Pos[] antis) {
        for (Pos anti : antis) {
            if (grid[anti.x + anti.y * width] != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean atLeastOneAntiOnMarked(int[] antis) {
        for (int anti : antis) {
            if (grid[anti] != 0) {
                return true;
            }
        }
        return false;
    }

    public List<Integer> getRowsForLeftmostNontouched() {
        List<Integer> startYs = new ArrayList<>();
        int leftmostX = Integer.MAX_VALUE;
        for (int posY = 0; posY < height ; posY++) {
          for (int posX = 0 ; posX < width && posX <= leftmostX ; posX++) {
              if (grid[posX + posY*width] == 0) {
                  if (posX < leftmostX) {
                      startYs.clear();
                  }
                  leftmostX = posX;
                  startYs.add(posY);
                  break;
              }
          }
        }
        return startYs;
    }

    public boolean isPositionMarked(int anti) {
        return grid[anti] != 0;
    }
}
