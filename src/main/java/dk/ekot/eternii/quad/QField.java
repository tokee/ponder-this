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
 * A field on a QBoard. It is static and always uses the same Quadbag.
 */
class QField {
    private final QuadBag quadBag;
    private final int x;
    private final int y;

    private boolean free = true;
    private QuadEdgeMap edgeMap = null;
    private long edgeHash;
    // If free == false below
    private int qpiece = 0;
    private long qedges = 0;

    public QField(QuadBag quadBag, int x, int y) {
        this.quadBag = quadBag;
        this.x = x;
        this.y = y;
    }

    /**
     * Setting Quad automatically
     * <p>
     * sets free to false
     * decrements needs in set (if available)
     */
    public void setQuad(int quadID) {
        if (free && edgeMap != null) {
            edgeMap.decNeed();
        }
        edgeMap = null;

        this.qpiece = quadBag.getQPiece(quadID);
        this.qedges = quadBag.getQEdges(quadID);

        free = false;
    }

    /**
     *
     * <p>
     * sets free to true
     * increments needs in set
     * decrements needs in previous set (if available)
     */
    protected void setEdgeMap(QuadEdgeMap edgeMap) {
        if (free && this.edgeMap != null) {
            this.edgeMap.decNeed();
        }
        this.edgeMap = edgeMap;
        edgeMap.incNeed();

        free = true;
    }

    /**
     * Checks if the state for the field is ok by counting possible quads and comparing to need.
     *
     * @return true if not free or set.needsSatisfied().
     */
    public boolean isOK() {
        // TODO: Ensure that needsSatisfied is auto-checking for changes
        return !free || (edgeMap != null && edgeMap.needsSatisfied());
    }

    public IntStream getAvailableQuadIDs() {
        return edgeMap.getQuadIDs(edgeHash);
    }

    public QuadBag getBag() {
        return quadBag;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getQPiece() {
        return qpiece;
    }

    public long getQEdges() {
        return qedges;
    }

    /**
     * @return -1 if the field is free, else the N edge.
     */
    public int getEdgeIfDefinedN() {
        return free ? -1 : QBits.getColN(qedges);
    }
    /**
     * @return -1 if the field is free, else the N edge.
     */
    public int getEdgeIfDefinedE() {
        return free ? -1 : QBits.getColE(qedges);
    }
    /**
     * @return -1 if the field is free, else the N edge.
     */
    public int getEdgeIfDefinedS() {
        return free ? -1 : QBits.getColS(qedges);
    }
    /**
     * @return -1 if the field is free, else the N edge.
     */
    public int getEdgeIfDefinedW() {
        return free ? -1 : QBits.getColW(qedges);
    }

    /**
     * Based on the colors and availability of the surrounding edges, choose and set the fitting bag.
     * @param edgeN the N edge or -1 if not available.
     * @param edgeE the E edge or -1 if not available.
     * @param edgeS the S edge or -1 if not available.
     * @param edgeW the W edge or -1 if not available.
     */
    public void autoSelectEdgeMap(int edgeN, int edgeE, int edgeS, int edgeW) {
        setEdgeMap(quadBag.getQuadEdgeMap(edgeN != -1, edgeE != -1, edgeS != -1, edgeW != -1));
        edgeHash = QBits.getHash(edgeN, edgeE, edgeS, edgeW, false);
    }
}
