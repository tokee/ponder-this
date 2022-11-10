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

import dk.ekot.misc.Bitmap;
import dk.ekot.misc.IntersectionBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds a shadowing bitmap representing quads.
 */
public class QuadSetBitmap {
    private static final Logger log = LoggerFactory.getLogger(QuadSetBitmap.class);

    /**
     * Number of free Fields that needs 1 element from this QuadSet.
     */
    private int need = 0;

    /**
     * Number of Quads in this set, live as well as dead.
     */
    private int allSize = 0;

    private Bitmap subset;

    public QuadSetBitmap(Bitmap base, int startBlock, int endBlock) {
        subset = new IntersectionBitmap(base, startBlock, endBlock);
        System.arraycopy(base.getBacking(), startBlock, subset.getBacking(), 0, subset.getBacking().length);
    }

    public boolean isNeedSatisfied() {
        return need <= subset.cardinality();
    }

    public int thisOrNext(int index) {
        return subset.thisOrNext(index);
    }

    /**
     * @return number of free Fields that needs 1 element from this QuadSet.
     */
    public int getNeed() {
        return need;
    }

    /**
     * @param need number of free Fields that needs 1 element from this QuadSet
     */
    public void setNeed(int need) {
        this.need = need;
    }

    /**
     * @return number of Quads in this set.
     */
    public int getAvailable() {
        return subset.cardinality();
    }
}
