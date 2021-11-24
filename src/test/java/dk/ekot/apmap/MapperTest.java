package dk.ekot.apmap;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Locale;

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
public class MapperTest extends TestCase {

    @Test
    public void testToString() {
        for (int edge = 3 ; edge <= 5 ; edge++) {
            Mapper map = new Mapper(edge);
            System.out.println(map + " Elements: " + map.validCount());
            System.out.println();
        }
    }

    @Test
    public void testFlattening() {
        for (int edge = 3 ; edge <= 5 ; edge++) {
            Mapper map = new Mapper(edge);
            int flat[] = map.getFlat();
            flat[5] = Mapper.MARKER;
            flat[10] = Mapper.ILLEGAL;
            map.setFlat(flat);
            System.out.println(map + " Elements: " + map.validCount());
            System.out.println();
        }
    }

    @Test
    public void testTriples() {
        for (int edge = 3 ; edge <= 5 ; edge++) {
            Mapper map = new Mapper(edge);

            int[] flat = map.getFlat();
            flat[5] = Mapper.MARKER;
            map.setFlat(flat);
            System.out.println(map + " Elements: " + map.validCount());
            int[][] triples = map.getFlatTriples();
            System.out.println("Triples from 5: " + triplesToString(triples[5]));
            System.out.println();
        }
    }

    private String triplesToString(int[] triples) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0 ; i < triples.length ; i+=3) {
            sb.append(String.format("[%d, %d, %d]", triples[i], triples[i+1], triples[i+2]));
            if (i < triples.length-1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Test
    public void testVisitTriples2() {
        dumpVisitTriples(2, 1, 0);
        dumpVisitTriples(2, 2, 1);
    }
    @Test
    public void testVisitTriples3() {
        dumpVisitTriples(3, 2, 1);
    }
    @Test
    public void testVisitTriples5() {
        dumpVisitTriples(5, 4, 0);
    }
    public void dumpVisitTriples(int edge, int x, int y) {
        Mapper board = new Mapper(edge);
        board.setQuadratic(x, y, Mapper.MARKER);
//        System.out.println(board);
        board.visitTriples(x, y, (posA, posB) -> {
            board.quadratic[posA] = Mapper.ILLEGAL;
            board.quadratic[posB] = Mapper.ILLEGAL;
            //board.quadratic[posB] = Mapper.ILLEGAL;
            board.priority[posA] = 1;
            board.priority[posB] = 1;
        });
        System.out.println(board);
    }
}