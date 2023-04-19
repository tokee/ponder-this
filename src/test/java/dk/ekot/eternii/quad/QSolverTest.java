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

import dk.ekot.eternii.BoardVisualiser;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class QSolverTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(QSolverTest.class);

    public void testExperimental() {
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Experimental");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);

        QSetup setup = new QSetup().
                walker(
                        //QWalker.corners(),
                        QWalker.quadCornersClockwise(),
                        //QWalker.freeNeighbourFields(),
                        //QWalker.quadCorners(),
                        //QWalker.borders(),
                        //QWalker.isBorderOrCorner(QWalker.fewestNeighboursQuads()),
                        QWalker.isBorderOrCorner(QWalker.available()),
                        QWalker.minMaxAvailable(),
                        QWalker.borderBorders(),
                        QWalker.topLeft()).
                quadDelivery(QuadDelivery.RANDOM_BORDER);

        runSolver(board, setup);
    }

    public void testExplosionDamper() {
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Explosion Damper");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);
        // 188 after a day
        QSetup setup = new QSetup().
                walker(QWalker.quadCornersClockwise(),
                       //thenComparingInt(QWalker.borders()).
                       QWalker.isBorderOrCorner(QWalker.fewestNeighbourQuads()),
                       QWalker.isBorderOrCorner(QWalker.available()),
                       QWalker.minMaxAvailable(),
                       QWalker.borderBorders(),
                       QWalker.topLeft()).
                quadDelivery(QuadDelivery.BORDER_BY_NEIGHBOURS);

        runSolver(board, setup);
    }

    public static void runSolver(QBoard board, QSetup setup) {

        QSolverBacktrack solver = new QSolverBacktrack(board, setup);

        try {
            solver.run();
        } catch (Exception e) {
            log.warn("Got exception while running", e);
        }

        try {
            Thread.sleep(1000000000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /*

    20230414:
    Laptop:  Attempts: 106104584, placed=144|188, att/sec=2924, possible=2e+66 best=188 free: https://e2.bucas.name/#puzzle=TokeEskildsen&board_w=16&board_h=16&board_edges=abdaabgbacsbabhcafubafqfafgfackfacpcaepcaeteafoeaemfaeueaeseaabedidaggjisjwghhwjunvhqoqngllokpllppjppskptjisovmjmqlvuiwqsjgibabjdpcajnmpwlmnwoklaaaaaaaaaaaaaaaaaaaaaaaaiqwomokqlugowswugvgsbacvcrbamhmrmkqhkgikaaaaaaaaaaaaaaaaaaaaaaaawqlhkwhqgiiwwiligpnicabpbteamrgtaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaalpluhsspiujslkvunhskbafheibagtgiaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaalomnsujojuouvususrtufacrbveagtqvaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaamtlgjkqtolnksnplthjncafhendaqgtnaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaluqrqnounvjnpsuvjuhsfafudhdatrwhkmnrttkmplmtqiklomniijjmaaaaaaaaaaaaaaaajmllupmmhtvpfabtdgdawstgnnnskvrnmtrvkuvtnhhujqphaaaaaaaaaaaaaaaalsnhmwksvjgwbafjdweatqqwngwqrjkgronjvuwohwkupmiwaaaaaaaaaaaaaaaantqvkvrtgouvfadoepbaqrqpwmmrkigmnqwiwsoqksssiuksaaaaaaaaaaaaaaaaqooirhmousthdadsboeaqtjomsutgruswvlromjvsommkuwoaaaaaaaaaaaaaaaaoggkmpqgtwhpdafwencajminujvmurijlslrjprsmonpwuioaaaaaaaaaaaaaaaagrinqvprhnlvfacncidaigqivvlgijvvlthjrujtnrquilprwmulqntmpgtnqmqgiommpstolgoscadgdcaaqeaclfaevcafhbacjfabqeafpdaeufadtdafteadqbaemdabtcadocacdaac
    Desktop: Attempts: 377501663, placed=156|196, att/sec=4156, possible=5e+11 best=196 free: https://e2.bucas.name/#puzzle=TokeEskildsen&board_w=16&board_h=16&board_edges=abdaabgbacsbabhcafjbacrfaepcadweadsdaepdaeteafqeaemfaeueaeseaabedidaggjisjwghhwjjqphrqpqphiqwushsuvupmmuttkmqvgtmqlvuiwqsjgibabjdpcajnmpwlmnwoklppropvwpijnvskqjvkokmnrkkvrngouvlugowswugvgsbacvcrbamhmrmkqhkgikrtrgwvwtntovqvntoknvrvmkrkhvuwokgiiwwiligpnicabpbtfamqntqwtqinqwrqunwlrqogllnnpgnvjnmtrvhjltosujiukslkvunhskbafhfufanvhutkuvqmokulwmrlsllkplppskjttprqrtlhwqunhhkolnvmjosowmfadofgfahhlgaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaalsuhjprswnspdaenfubalpluaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaauisorilisoiieafobjfalijjaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaasvpnlrwviqorfafqfhcajumhaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaphtvwkuhounkfaducpcamiwpaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaatlqtuqrlngwqdadgchbawmqhaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaqooirrmowhtrdadhboeaqtjomsutgrusilprwvvlpsuvpgmsrjkgmlljsnhllktnoggkmpqgtwhpdafwencajminujvmurijphhrvjshuoujmniokpgnlmtphgimtnqggrinqvprhnlvfacncidaigqivvlgijvvhukjsrtuujtrijpjgwvjtgiwigtgqmqgiommpstolgoscadgdcaaqeaclfaevcafkfactdafteadpbaeveabibaeteabqbaemdabtcadocacdaac

     */
}
