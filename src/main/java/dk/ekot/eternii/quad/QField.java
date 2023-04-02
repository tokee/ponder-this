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

/**
 * A field on a QBoard.
 */
class QField {
    private boolean free = true;
    private QuadSet set = null;
    // If free == false below
    private int qpiece = 0;
    private long qedges = 0;

    /**
     * Setting Quad automatically
     * <p>
     * sets free to false
     * decrements needs in set (if available)
     */
    public void setQuad(int qpiece, long qedges) {
        if (free && set != null) {
            set.decNeed();
        }
        set = null;

        this.qpiece = qpiece;
        this.qedges = qedges;

        free = false;
    }

    /**
     * Setting the QField to free automatically
     * <p>
     * sets free to true
     * increments needs in set
     * decrements needs in previous set (if available)
     */
    public void setFree(QuadSet set) {
        if (free && this.set != null) {
            this.set.decNeed();
        }
        this.set = set;
        set.incNeed();

        free = true;
    }

    /**
     * Checks if the state for the field is ok by counting possible quads and comparing to need.
     *
     * @return true if not free or set.needsSatisfied().
     */
    public boolean isOK() {
        // TODO: Ensure that needsSatisfied is auto-checking for changes
        return !free || (set != null && set.needsSatisfied());
    }
}
