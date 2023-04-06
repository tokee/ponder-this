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

import java.util.stream.IntStream;

/**
 * Delivers quads.
 */
public interface QuadHolder {

    /**
     * @return all IDs for available quads.
     */
    IntStream getAvailableQuadIDs();

    /**
     * @return true if the quad with the given ID is available, else false.
     */
    boolean isAvailable(int quadID);

    /**
     * @return the number of available Quads.
     */
    int available();

}
