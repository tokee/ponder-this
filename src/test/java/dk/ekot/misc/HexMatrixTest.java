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
package dk.ekot.misc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class HexMatrixTest {
    private static final Logger log = LoggerFactory.getLogger(HexMatrixTest.class);

    public void testThomasWay() {
        String values3_3 = "381,79c,26c";
        char[][] matrix3_3 = createFromString(values3_3);

        String values12_12 = "0a8301b11b01,1bda41b24d78,37c09e8d5998,60473283d3b8,13279043d9bc,371bf4c021c1,1d122e800ee1,5bc967265d88,5f1998f5915d,628dff094034,39effbe6ecc8,2c440c20e0a0";
        char[][] matrix12_12 = createFromString(values12_12);

        // split by ,
    }

    public char[][] createFromString (String values){
        String[] lines = values.split(",");
        int size = lines[0].length();

        char[][] matrix = new char[size][size];
        for (int i = 0; i < lines.length; i++) {
            for (int j = 0; j < lines.length; j++) {
                matrix[i][j] = lines[i].charAt(j);
            }
        }
        return matrix;
    }

    @Test
    public void testTokeThomasModified() {
        String values = "0a8301b11b01,1bda41b24d78,37c09e8d5998,60473283d3b8,13279043d9bc,371bf4c021c1,1d122e800ee1,5bc967265d88,5f1998f5915d,628dff094034,39effbe6ecc8,2c440c20e0a0";

        String[] lines = values.split(",");

        char[][] matrix = new char[lines.length][];
        for (int i = 0; i < lines.length; i++) {
            matrix[i] = lines[i].toCharArray();
        }
        printMatrix(matrix);
    }

    @Test
    public void testTokeWay() {
        String values = "0a8301b11b01,1bda41b24d78,37c09e8d5998,60473283d3b8,13279043d9bc,371bf4c021c1,1d122e800ee1,5bc967265d88,5f1998f5915d,628dff094034,39effbe6ecc8,2c440c20e0a0";

        char[][] matrix = Arrays.stream(values.split(","))
                .map(String::toCharArray)
                .toArray(size -> new char[size][]);

        printMatrix(matrix);
    }

    @Test
    public void testTokeString() {
        String values = "0a8301b11b01,1bda41b24d78,37c09e8d5998,60473283d3b8,13279043d9bc,371bf4c021c1,1d122e800ee1,5bc967265d88,5f1998f5915d,628dff094034,39effbe6ecc8,2c440c20e0a0";

        String[] matrix = Arrays.stream(values.split(","))
                .toArray(size -> new String[size]);

        System.out.println(Arrays.toString(matrix));
    }

    @Test
    public void testTokeWayPadded3() {
        String values = "0a8301b11b01," +
                        "1bda41b24d78," +
                        "37c09e8d5998," +
                        "60473283d3b8," +
                        "13279043d9bc," +
                        "371bf4c021c1," +
                        "1d122e800ee1," +
                        "5bc967265d88," +
                        "5f1998f5915d," +
                        "628dff094034," +
                        "39effbe6ecc8," +
                        "2c440c20e0a0";

        char[][] matrix = Arrays.stream(("," + values + ",").split(",", Integer.MAX_VALUE))
                .map(row -> "0" + (row.isEmpty() ? "0".repeat(values.split(",").length) : row) + "0")
                .map(String::toCharArray)
                .toArray(size -> new char[size][0]);

        printMatrix(matrix);
    }

    @Test
    public void testTokeWayPadded() {
        String values12_12 = "0a8301b11b01,1bda41b24d78,37c09e8d5998,60473283d3b8,13279043d9bc,371bf4c021c1,1d122e800ee1,5bc967265d88,5f1998f5915d,628dff094034,39effbe6ecc8,2c440c20e0a0";
        char[][] matrix = Arrays.stream(values12_12.split(","))
                .map(row -> "0" + row + "0")
                .collect(() -> new ArrayList<String>(), (list, element) -> list.add(element), (list1, list2) -> list1.addAll(list2))
                .stream()
                .collect(new Collector<String, ArrayList<String>, ArrayList<String>>() {
                    @Override
                    public Supplier<ArrayList<String>> supplier() {
                        return ArrayList::new;
                    }

                    @Override
                    public BiConsumer<ArrayList<String>, String> accumulator() {
                        return ArrayList::add;
                    }

                    @Override
                    public BinaryOperator<ArrayList<String>> combiner() {
                        return (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        };
                    }

                    @Override
                    public Function<ArrayList<String>, ArrayList<String>> finisher() {
                        return (list) -> {
                            String empty = "0".repeat(list.get(0).length());
                            list.add(0, empty);
                            list.add(empty);
                            return list;
                        };
                    }

                    @Override
                    public Set<Characteristics> characteristics() {
                        return Collections.emptySet();
                    }
                })
                .stream()
                .map(String::toCharArray)
                .toArray(size -> new char[size][size]);
        printMatrix(matrix);
    }

    @Test
    public void testTokeWayPadded2() {
        String values12_12 = "0a8301b11b01,1bda41b24d78,37c09e8d5998,60473283d3b8,13279043d9bc,371bf4c021c1,1d122e800ee1,5bc967265d88,5f1998f5915d,628dff094034,39effbe6ecc8,2c440c20e0a0";
        char[][] matrix = Arrays.stream(values12_12.split(","))
                .map(row -> "0" + row + "0")
                .collect(Collectors.collectingAndThen(Collectors.toList(), (list) -> {
                    String empty = "0".repeat(list.get(0).length());
                    list.add(0, empty);
                    list.add(empty);
                    return list;
                }))
                .stream()
                .map(String::toCharArray)
                .toArray(size -> new char[size][size]);
        printMatrix(matrix);
    }

    private void printMatrix(char[][] matrix) {
        for (int i = 0; i < matrix.length ; i++) {
            for (int j = 0; j < matrix[i].length ; j++) {
                System.out.print(matrix[i][j]);
            }
            System.out.println("");
        }
    }
}
