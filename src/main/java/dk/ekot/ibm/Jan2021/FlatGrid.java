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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
final class FlatGrid {

    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int DIRECTION_MASK = 3;

    public static final int ANTI = 15;

    private static final int[] NO_ANTIS = new int[0];

    public final String name;
    public final int width;
    public final int height;
    private final List<Integer> startYs;
    private final int total;
    private final int maxNonChangingMoves;


    //private final int[] grid;
    final byte[] grid;

    private int direction = UP;
    private int posX = 0;
    private int posY = 0;
    private int move = 0;
    private int maxMove = 0;
    private int immune = 0; // Fields vaccinated twice

    private int marks = 0;
    private long lastRunMS = 0;
    private int[] antis = new int[0];

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
        //grid = new int[total];
        grid = new byte[total];
        maxNonChangingMoves = total;
    }

    public Match getMatch(int threads) {
        return new Match(name, width, height, move, lastRunMS*threads, toString(), antis, startYs);
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
        //Arrays.fill(grid, 0);
        Arrays.fill(grid, (byte)0);
        immune = 0;
        move = 0;
        maxMove = maxNonChangingMoves;
        direction = UP;
        posX = 0;
        posY = 0;
    }

    public boolean fullRunOld() {
        lastRunMS -= System.currentTimeMillis();
        while (hasNext()) {
            next();
        }
        lastRunMS += System.currentTimeMillis();
        return allImmune();
    }

    public boolean fullRunOld2() {
        lastRunMS -= System.currentTimeMillis();
        while (move != maxMove) {
            next();
        }
        lastRunMS += System.currentTimeMillis();
        return allImmune();
    }

    public boolean fullRun() {
        lastRunMS -= System.currentTimeMillis();
        while (move != maxMove) {
            final int pos = posX + posY * width;
            final int tile = grid[pos];
            if (tile == 0) {
                ++grid[pos];
                ++direction;
                maxMove = move + maxNonChangingMoves;
            } else if (tile == 1) {
                ++grid[pos];
                --direction;
                if (++immune == total-marks) {
                    break;
                }
                maxMove = move+maxNonChangingMoves;
            } else if (tile == ANTI) {
                --direction;
            }
            // Not shown above: 2, where the bot does not turn
            // Seems like the JIT is clever enough to see that 2 is the most common case

            switch (direction & DIRECTION_MASK) {
                case UP: {
                    if (--posY == -1) {
                        posY = height-1;
                    }
                    break;
                }
                case RIGHT: {
                    if (++posX == width) {
                        posX = 0;
                    }
                    break;
                }
                case DOWN: {
                    if (++posY == height) {
                        posY = 0;
                    }
                    break;
                }
                case LEFT: {
                    if (--posX == -1) {
                        posX = width-1;
                    }
                    break;
                }
            }
            ++move;
        }
        lastRunMS += System.currentTimeMillis();
        return allImmune();
    }

    final void next() {
        final int pos = posX + posY * width;
        final int tile = grid[pos];
        if (tile == 0) {
            ++grid[pos];
            ++direction;
            maxMove = move + maxNonChangingMoves;
        } else if (tile == 1) {
            ++grid[pos];
            --direction;
            maxMove = ++immune == total-marks ? move+1 : move+maxNonChangingMoves;
        } else if (tile == ANTI) {
            --direction;
        }
        // Not shown above: 2, where the bot does not turn
        // Seems like the JIT is clever enough to see that 2 is the most common case

        switch (direction & DIRECTION_MASK) {
            case UP: {
                if (--posY == -1) {
                    posY = height-1;
                }
                break;
            }
            case RIGHT: {
                if (++posX == width) {
                    posX = 0;
                }
                break;
            }
            case DOWN: {
                if (++posY == height) {
                    posY = 0;
                }
                break;
            }
            case LEFT: {
                if (--posX == -1) {
                    posX = width-1;
                }
                break;
            }
        }
        ++move;
    }

    public boolean hasNext() {
        return !allImmune() && move < maxMove;
    }

    public final boolean allImmune() {
        return immune + marks == total;
    }

    public String toString() {
        final int pos = posX + posY * width;
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                int field = grid[x + y * width];
                sb.append(field == ANTI ? "B" : field);
                if (pos == x + y * width) {
                    switch (direction & DIRECTION_MASK) {
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

    /**
     * Clears, assigns antis and performs a fullRun.
     */
    public boolean fullRun(int[] antis) {
        clear();
        setMarks(antis);
        return fullRun();
    }

    /**
     * @return a boolean version of the grid with true for all entries that have been visited.
     */
    public boolean[] getVisitedMap() {
        boolean[] visited = new boolean[grid.length];
        for (int i = 0 ; i < grid.length ; i++) {
            visited[i] = grid[i] != 0;
        }
        return visited;
    }
}

/*
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,674
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,897
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     8,112
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    10,490
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    11,694
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    13,464
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,973
      41       8     109       0       6       2       5       7       2     134       4     198      81      75    10,174
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     8,282
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    10,569
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    12,643
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    14,098
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     7,388
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,724
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     7,770
      43      11     105       0      12       1       5       3       1      44       3     155      39      27     9,968
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    10,880
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    12,744

Selective *
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     8,377
      41       8     109       0       6       2       5       7       2     134       4     198      81      75    11,933
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     9,746
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    12,583
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    13,874
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    16,212

++x not x++
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,930
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,910
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     8,143
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    10,379
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    11,481
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    13,312
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,569
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,889
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     7,913
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    10,196
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    11,270
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    13,079

local pos
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,806
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,362
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     7,516
      43      11     105       0      12       1       5       3       1      44       3     155      39      27     9,746
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    10,696
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    12,413
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,918
      41       8     109       0       6       2       5       7       2     134       4     198      81      75    10,249
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     8,354
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    10,733
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    11,979
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    13,941
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,573
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,343
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     7,622
      43      11     105       0      12       1       5       3       1      44       3     155      39      27     9,886
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    10,785
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    12,419

localise immunecheck
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     5,974
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,442
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     7,610
      43      11     105       0      12       1       5       3       1      44       3     155      39      27     9,774
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    10,807
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    13,419

inline
    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,036
      41       8     109       0       6       2       5       7       2     134       4     198      81      75     9,162
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     7,457
      43      11     105       0      12       1       5       3       1      44       3     155      39      27     9,468
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    10,580
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    12,310

 */