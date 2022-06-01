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
package dk.ekot.eternii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Prioritises top-left.
 *
 * Observation: Expands in chunks, reaches 70 free in 10 minutes, 61 after an hour or so,
 * stable for the next hour (40M attempts), then 60 free (60M attempts?).
 * Speed: ~5000 @ office machine
 * Attempts: 888175K, free=136, minFree=57, attempts/sec=5237, best=https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfadtcaftdafgfacnfacocafhcadufabmdafjbaelfadteadhdaacddsdahvjskvwvtkuvtnlkgwqnnspwouishhruukjhmnrkjronlslrtgwshhlgcabhdpcajnmpwlmnulpllkilqjskprsjiqorrpnqjttprqrtooiqlugowswulgosbabgcpcammupmsompgmsiiwgsjgisiujomminiomtvvirkhvigmkgjigwgsjouvgbafucqeauiwqowuimqhwwsoqgrusujtrmlljouplvpsuhhrpmrmhilirsuhlvusufafueibawtgiusrthwusokuwuhwktvphlwvvpmiwsutmrhoumorhiqwohmkqsowmfadobpcagtnprsmtujosumhjwksmpllkvmqlinjmtkknoggkrtrgwhptkrphwpkrdaepcidanqwimqgqovjqhnlvsvpnlgvvqiigjvvikokvgllorwvlppvwpjppkqtjeafqdgcawvjggtqvjistlpripropvmkrijjmvomjklwolwolvgwwvgsgpnigtmqnfaemcsbajuhsqnousnnnrqunosnqkppsjijpmhgiwjhhovmjwkrvsiukisoiqugseaeubveahunvoujunhhuuqlhnqoqphjqjnthgrinhkprmttkrvmtulkvorglgtmreabteseanhlsjlthhwqllwvwovuwjnnvtqvniklqpgnktrkgmorrkqmogmpqmrwmbacrepbalsnpthusqkwhvrnkaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaujvmaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaqvpraaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
 */
public class WalkerE implements Walker {
    private static final Logger log = LoggerFactory.getLogger(WalkerE.class);

    private final EBoard board;
    private final Comparator<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> comparator;

    public WalkerE(EBoard board) {
        this.board = board;
        comparator = getFieldComparator();
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
        return getFreePiecesStrategyA()
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the free fields with lists of corresponding Pieces. Empty if no free fields.
     */
    public Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getFreePiecesStrategyA() {
        return board.streamAllFields()
                .filter(EBoard.Field::isFree)
                .map(field -> new EBoard.Pair<>(field, field.getBestPieces()))
                .sorted(comparator);
    }

    private Comparator<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getFieldComparator() {
        return Comparator.<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>>comparingInt(pair -> pair.left.getY()*board.getWidth() + pair.left.getX()
                ) // Board edges first
                .thenComparingInt(pair -> pair.right.size())                         // Least valid pieces
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount())           // Least free edges
                .thenComparingInt(pair -> pair.left.getX() == 0 || pair.left.getY() == 0 ||
                                        pair.left.getX() == board.getWidth() - 1 ||
                                        pair.left.getY() == board.getHeight() - 1 ? 0 : 1);
    }


}
