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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * A field on a QBoard. It is static and always uses the same Quadbag.
 */
class QField {
    private static final Logger log = LoggerFactory.getLogger(QField.class);

    private final QBoard board;
    private final QuadBag quadBag;
    private final int x;
    private final int y;

    private boolean free = true;
    private QuadEdgeMap edgeMap = null;
    private long edgeHash;
    // If free == false below
    private int quadID = -1;
    private int qpiece = 0;
    private long qedges = 0;
    private String[] text = new String[0];

    public QField(QBoard board, QuadBag quadBag, int x, int y) {
        this.board = board;
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

        this.quadID = quadID;
        this.qpiece = quadBag.getQPiece(quadID);
        this.qedges = quadBag.getQEdges(quadID);

        free = false;
    }

    public QuadEdgeMap getEdgeMap() {
        return edgeMap;
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
    public boolean needsSatisfied() {
        //log.debug("needsSatisfied called for {}", this);
        //log.info("Checking needs satisfied for " + this);
        // TODO: Ensure that needsSatisfied is auto-checking for changes
        boolean satisfied = !free || (edgeMap != null && edgeMap.needsSatisfied(edgeHash));
        //if (!satisfied) {
        //    log.debug("needsNotSatisfied for {} with {}, needs {} and hash {}",
        //            this, edgeMap == null ? "no edgemap" : edgeMap, edgeMap == null ? "N/A" : edgeMap.getNeed(), edgeHash);
        //}
        return satisfied;
    }

    public IntStream getAvailableQuadIDs() {
        return edgeMap.getAvailableQuadIDs(edgeHash);
    }
    public IntStream getAvailableQuadIDsNoCache() {
        return edgeMap.getAvailableQuadIDsNoCache(edgeHash);
    }

    /**
     * Maximum number of quads that can be retrieved from {@link #getAvailableQuadIDs()} and
     * {@link #getAvailableQuadIDsNoCache()}, ignoring {@link PieceTracker} state.
     */
    public int getMaxAvailable() {
        if (edgeMap == null) {
            return 0;
        }
        return edgeMap.getMaxAvailable(edgeHash);
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
        if (!free) {
            // No need for warning as this is "auto"
            //log.warn("Attempting to set edgeMap for non-free " + this);
            return;
        }
        try {
            QuadEdgeMap edgeMap = quadBag.getQuadEdgeMap(edgeN != -1, edgeE != -1, edgeS != -1, edgeW != -1);
            if (edgeMap == null) {
                // TODO: Elevate this to error or Exception
                log.warn("Got edgeMap == null for quadBag type {} with position ({}, {}) " +
                         "with edges {}, {}, {}, {}. Probably due to a temporary disabling of edgeMap generation",
                         quadBag.getType(), getX(), getY(), edgeN, edgeE, edgeS, edgeW);
            } else {
                //log.debug("Setting edgeMap for ({}, {}) with edges N={}, E={}, S={}, W={}",
                //          x, y, edgeN, edgeE, edgeS, edgeW);
                setEdgeMap(edgeMap);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Exception auto requesting and setting edgeMap for quadBag type " + quadBag.getType() +
                    " with position (" + getX() + ", " + getY() + 
                    ") with edges " + edgeN + ", " + edgeE + ", " + edgeS + ", " + edgeW, e);
        }
        edgeHash = QBits.getHash(edgeN, edgeE, edgeS, edgeW, false);
    }

    @Override
    public String toString() {
        return "QField(" + x + ", " + y + ", quad=" + !free + ")";
    }

    /**
     * @return true if there are no quad on the field, else false.
     */
    public boolean isFree() {
        return free;
    }

    public void setFree() {
        if (free && this.edgeMap != null) {
            this.edgeMap.decNeed();
        }
        free = true;
    }

    public int getQuadID() {
        if (free) {
            throw new IllegalStateException("Attempting to get quad ID for a free field " + this);
        }
        return quadID;
    }

    public String approximateQuadCount() {
        return (!free || quadBag == null) ? "N/A" : edgeMap.approximateQuadCount(edgeHash);
    }

    public String[] getText() {
        return text;
    }

    public void setText(String... text) {
        this.text = text;
    }

    /**
     * @return precise counting of available quads (might be slow).
     */
    public int available() {
        return (!free || quadBag == null) ? 0 : edgeMap.available(edgeHash);
    }

    public QBoard getBoard() {
        return board;
    }
}
