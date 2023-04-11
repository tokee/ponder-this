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

/**
 * Tracks pieces (1x1) available & used.
 */
public class PieceTracker {

    long stateID = 0;
    /**
     * ByteMap representing pieces: 1 = present, 0 = not present
     */
    public byte[] pieceIDByteMap = new byte[256];

    public PieceTracker() {
        Arrays.fill(pieceIDByteMap, (byte) 1);
    }

    /**
     * Add the given qpiece (in reality the 4 pieces making up the qpiece).
     */
    public void addQPiece(int qPiece) {
        addPieces(QBits.getPieceNW(qPiece), QBits.getPieceNE(qPiece),
                  QBits.getPieceSE(qPiece), QBits.getPieceSW(qPiece));
    }
    /**
     * Add the given pieces to the tracker, thereby making them free for use in quads.
     */
    public void addPieces(int... pieceIDs) {
        for (int pieceID: pieceIDs) {
            if (pieceIDByteMap[pieceID] == 1) {
                throw new IllegalStateException("The piece with id " + pieceID + " has already been added");
            }
        }
        stateID++;
        for (int pieceID: pieceIDs) {
            pieceIDByteMap[pieceID] = 1;
        }
    }

    /**
     * Remove the given qpiece (in reality the 4 pieces making up the qpiece).
     */
    public void removeQPiece(int qPiece) {
        removePieces(QBits.getPieceNW(qPiece), QBits.getPieceNE(qPiece),
                     QBits.getPieceSE(qPiece), QBits.getPieceSW(qPiece));
    }

    /**
     * Remove the given pieces from the tracker, thereby making them unavailable for use in quads.
     */
    public void removePieces(int... pieceIDs) {
        for (int pieceID: pieceIDs) {
            if (pieceIDByteMap[pieceID] == 0) {
                throw new IllegalStateException("The piece with id " + pieceID + " has already been removed");
            }
        }
        // TODO: Make the tracker clever so that removal of just added pieces decrements the stateID
        // TODO: Expand on the idea in the TODO above by keeping a full undo stack
        stateID++;
        for (int pieceID: pieceIDs) {
            pieceIDByteMap[pieceID] = 0;
        }
    }

    /**
     * Each time the PieceMap is changed, the stateID is changed.
     * <p>
     * The same stateID will always correspond to the same state of the {@link #pieceIDByteMap}
     * but not vice versa: A given state of {@link #pieceIDByteMap} can correspond to more than
     * one stateID.
     */
    public long getStateID() {
        return stateID;
    }

    public int cardinality() {
        int cardinality = 0;
        for (int i = 0 ; i < pieceIDByteMap.length ; i++) {
            if (pieceIDByteMap[i] == 1) {
                ++cardinality;
            }
        }
        return cardinality;
    }
}
