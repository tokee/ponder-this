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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

/**
 *
 */
public class QSolverTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(QSolverTest.class);

    public void testCounter() {
        QuadBag.disabledMaps = new HashSet<>(
                Arrays.asList(0b1111, 0b1110, 0b1101, 0b1011, 0b0111));
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Experimental");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);

        QWalker walker = new QWalkerImpl(board,
                Comparator.comparingInt(QWalker.identity()). // For easier experiments below
                        thenComparingInt(QWalker.quadCornersClockwise()));

        QuadDelivery quadDelivery = QuadDelivery.IDENTITY;



        QSolverBacktrack solver = new QSolverBacktrack(
                board, walker, quadDelivery, QSolverBacktrack.getSolutionPrinter(1000), 4, Long.MAX_VALUE);
        runSolver(solver);
    }

    public void testExperimental() {
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Experimental");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);

        // 120 after a day
        QWalker walker = new QWalkerImpl(board,
                Comparator.comparingInt(QWalker.identity()). // For easier experiments below
                        thenComparingInt(QWalker.corners()).
                        thenComparingInt(QWalker.freeNeighbourFields()).
                        thenComparingInt(QWalker.quadCorners()).
                        //thenComparingInt(QWalker.quadCornersClockwise()).
                        //thenComparingInt(QWalker.borders()).
                        //thenComparingInt(QWalker.isBorderOrCorner(QWalker.fewestNeighboursAvailable())).
                        thenComparingInt(QWalker.isBorderOrCorner(QWalker.available())).
                        thenComparingInt(QWalker.minMaxAvailable()).
                        thenComparingInt(QWalker.borderBorders()).
                        thenComparingInt(QWalker.topLeft()));

        QuadDelivery quadDelivery = QuadDelivery.IDENTITY;
        
        runSolver(board, walker, quadDelivery);
    }

    public void testExplosionDamper() {
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Explosion Damper");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);
        // 188 after a day
        QWalker walker = new QWalkerImpl(board,
                Comparator.comparingInt(QWalker.quadCornersClockwise()).
                        //thenComparingInt(QWalker.borders()).
                        thenComparingInt(QWalker.isBorderOrCorner(QWalker.fewestNeighbourQuads())).
                        thenComparingInt(QWalker.isBorderOrCorner(QWalker.available())).
                        thenComparingInt(QWalker.minMaxAvailable()).
                        thenComparingInt(QWalker.borderBorders()).
                        thenComparingInt(QWalker.topLeft()));

        QuadDelivery quadDelivery = QuadDelivery.BORDER_BY_NEIGHBOURS;

        runSolver(board, walker, quadDelivery);
    }

    private void runSolver(QBoard board, QWalker walker, QuadDelivery quadDelivery) {
        runSolver(board, walker, quadDelivery, 65);
    }
    private void runSolver(QBoard board, QWalker walker, QuadDelivery quadDelivery, int maxDepth) {
        QSolverBacktrack solver = new QSolverBacktrack(
                board, walker, quadDelivery, QSolverBacktrack.PRINT_ALL, maxDepth, Long.MAX_VALUE);

        runSolver(solver);
    }

    private void runSolver(QSolverBacktrack solver) {
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
