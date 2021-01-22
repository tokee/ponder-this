package dk.ekot.ibm.Jan2021;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
public class WalkPlannerTest extends TestCase {

    public void testWalkTrivial() {
        int width = 3;
        int height = 3;
        int[] walk = new int[width*height];
        for (int i = 0 ; i < walk.length ; i++) {
            walk[i] = i;
        }
        assertWalks(width, height, Collections.singletonList(walk));
    }

    public void testWalkSingleStep() {
        int width = 3;
        int height = 3;
        int[] walk = new int[width*height];
        int index = 0;

        for (int x = 0 ; x < width ; x++) {
            walk[index++] = x;
        }

        for (int y = 1 ; y < height ; y++) {
            walk[index++] = y*width;
        }

        for (int y = 1 ; y < height ; y++) {
            for (int x = 1 ; x < width ; x++) {
                walk[index++] = x + y*width;
            }
        }

        assertWalks(width, height, Collections.singletonList(walk));

    }

    public void testWalkComplex() {
        int width = 3;
        int height = 3;
        List<int[]> walks = getComplexWalk(width, height);

        assertWalks(width, height, walks);

    }

    private List<int[]> getComplexWalk(int width, int height) {
        List<int[]> walks = new ArrayList<>();

        int[] walk0 = new int[width];
        int index = 0;
        for (int x = 0; x < width; x++) {
            walk0[index++] = x;
        }
        walks.add(walk0);

        index = 0;
        int[] walk1 = new int[height - 1];
        for (int y = 1; y < height; y++) {
            walk1[index++] = y * width;
        }
        walks.add(walk1);

        index = 0;
        int[] walk2 = new int[(width - 1) * (height - 1)];
        for (int y = 1; y < height; y++) {
            for (int x = 1; x < width; x++) {
                walk2[index++] = x + y * width;
            }
        }
        walks.add(walk2);
        return walks;
    }

    public void testWalkStridesSimple() {
        int width = 3;
        int height = 3;
        int[] blocks = new int[]{3, 6};

        List<int[]> walks = new ArrayList<>();
        int index = 0;
        for (int block : blocks) {
            int[] walk = new int[block];
            for (int i = 0; i < block; i++) {
                walk[i] = index++;
            }
            walks.add(walk);
        }

        assertWalks(width, height, walks);
    }

    private void assertWalks(int width, int height, List<int[]> walks) {
        for (int antiCount = 1 ; antiCount <=3 ; antiCount++) {
            assertWalks(width, height, walks, antiCount);
        }
    }
    private void assertWalks(int width, int height, List<int[]> walks, int antiCount) {
        int fields = width*height;
        int expected = antiCount == 1 ? fields :
                // n! / ((n-k)! * k!)
                fac(fields) / ((fac(fields-antiCount) * fac(antiCount)));

        WalkPlanner.Walk walk = new WalkPlanner.Walk(width, height, walks, antiCount);
        int[] antiPoss = null;
        int posCount = 0;
        List<String> positions = new ArrayList<>();
        while ((antiPoss = walk.next(antiPoss)) != null) {
            ++posCount;
            //positions.add(antiPoss[0] < antiPoss[1] ? "[" + antiPoss[0]+ ", " + antiPoss[1] + "]" : "[" + antiPoss[1]+ ", " + antiPoss[0] + "]");
            Arrays.sort(antiPoss);
            positions.add(Arrays.toString(antiPoss));
            //positions.add(Arrays.toString(antiPoss));
        }
        Collections.sort(positions);

        List<String> duplicates = findDuplicates(positions);

        if (antiCount == 3) {
            assertEquals("The returned list should contain the expected elements",
                         listToString(getExpected3(width * height)), listToString(positions));
        }

        assertEquals("The number of delivered tuples for antiCount=" + antiCount + " should be correct\n" +
                     listToString(positions) + "\n\n" +
                     duplicates,
                     expected, posCount);
    }

    private String listToString(List<String> positions) {
        return positions.toString().replace("[[", "[").replace("]]", "]").replace("], [", "]\n[");
    }

    private List<String> findDuplicates(List<String> positions) {
        Set<String> set = new HashSet<>();
        return positions.stream().filter(element -> !set.add(element)).collect(Collectors.toList());
    }

    private int fac(int fields) {
        int factorial = 1;
        for (int i = 2 ; i <= fields ; i++) {
            factorial *= i;
        }
        return factorial;
    }

    public List<String> getExpected3(int fields) {
        List<String> result = new ArrayList<>();
        for (int one = 0 ; one < fields-2; one++) {
            for (int two = one+1 ; two < fields-1; two++) {
                for (int three = two+1 ; three < fields; three++) {
                    result.add("[" + one + ", " + two + ", " + three + "]");
                }
            }
        }
        return result;
    }
}