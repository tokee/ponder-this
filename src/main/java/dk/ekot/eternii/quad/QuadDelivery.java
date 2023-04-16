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
package dk.ekot.eternii.quad;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 *
 */
@FunctionalInterface
public interface QuadDelivery extends Function<QWalker.Move, IntStream> {
    QuadDelivery IDENTITY = QWalker.Move::getAvailableQuadIDs;

    QuadDelivery RANDOM_BORDER = new QuadDelivery() {
        final Random random = new Random(); // TODO: Introduce seed

        @Override
        public IntStream apply(QWalker.Move move) {
            if (!move.isBorderOrCorner()) {
                return move.getAvailableQuadIDs();
            }
            return Arrays.stream(shuffle(move.getAvailableQuadIDs().toArray(), random));
        }
    };

    QuadDelivery BORDER_BY_NEIGHBOURS = move ->
            move.isBorderOrCorner() ?
                    move.getAvailableQuadIDsByNeighbours() :
                    move.getAvailableQuadIDs();

    // Destructive to the original array
    static int[] shuffle(int[] array, Random random) {
        for (int i = 0; i < array.length; i++) {
            int randomIndexToSwap = random.nextInt(array.length);
            int temp = array[randomIndexToSwap];
            array[randomIndexToSwap] = array[i];
            array[i] = temp;
        }
        return array;
    }
}
