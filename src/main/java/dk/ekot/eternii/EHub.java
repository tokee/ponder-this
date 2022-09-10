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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Responsible for starting and stopping solvers.
 */
public class EHub implements EListener, Runnable {
    private static final Logger log = LoggerFactory.getLogger(EHub.class);

    private static int globalBest = 0;
    private static int globalBestPhase2 = 0;
    public static final int DEFAULT_RESET_TIME = 5000;
    public static final int DEFAULT_MAX_TIME = 500000000;
    public static final int DEFAULT_PHASE2_RESET = 1000;
    public static final int DEFAULT_PHASE2_TIMEOUT = 60000;

    private final boolean clues;
    private final int resetTime = 0; // Not used here
    private final int maxTime = 0;   // Not used here
    private static ExecutorService executor;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        if (args.length >= 1 && "-h".equals(args[0])) {
            System.out.println("EHub <threads> <hints (def: true)>");
            return;
        }
        int threadCount = args.length >= 1 ? Integer.parseInt(args[0]) : 5;
        boolean clues = args.length >= 2 ? Boolean.parseBoolean(args[1]) : true;
        //int resetTime = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_RESET_TIME;
        //int maxTime = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_MAX_TIME;

        System.out.println("Starting " + threadCount + " boards with clues=" + clues);
        executor = Executors.newFixedThreadPool(threadCount*2);
        List<Future<?>> jobs = new ArrayList<>();
        for (int i = 0 ; i < threadCount ; i++) {
           jobs.add(executor.submit(new EHub(clues)));
        }
        for (Future<?> job: jobs) {
            job.get();
        }
        executor.shutdown();
        System.out.println("Finished running");
    }

//    public EHub(int resetTime, int maxTime) {
  //      this.resetTime = resetTime;
    //    this.maxTime = maxTime;
   // }
    public EHub(boolean clues) {
        this.clues = clues;
    }

    @Override
    public void run() {
        System.out.println("Beginning solve... " + Thread.currentThread().getName());
        testSolver(board -> new WalkerGeneric(board, Comparator.
                           comparingInt(Walker.Move::topLeftFirst)
        ),clues);
        //testSolver(WalkerG2R::new, true);
    }

    private void testSolver(Function<EBoard, Walker> walkerFactory) {
        testSolver(walkerFactory, clues);
    }
    private void testSolver(Function<EBoard, Walker> walkerFactory, boolean clues) {
        EBoard board = getBoard(clues);

        Walker walker = new WalkerGeneric(board, Comparator.
                comparingInt(Walker.Move::topLeftFirst));
        Strategy strategy = new StrategyLeveledReset(walker, this, 0.00001, 5, 500, 30);
        StrategySolverMove solver = new StrategySolverMove(board, strategy, new Random());

//        Walker walker = walkerFactory.apply(board);
//        Strategy strategy = new StrategyReset(walker, this, resetTime, maxTime);
        //StrategySolver solver = new StrategySolver(board, strategy);
//        StrategySolverMove solver = new StrategySolverMove(board, strategy);

        solver.run();
        System.out.println(board.getTopTerminators(15));
    }


    private EBoard getBoard() {
        return getBoard(true);
    }
    private EBoard getBoard(boolean clues) {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        if (clues) {
            pieces.processEterniiClues((x, y, piece, rotation) -> board.placePiece(x, y, piece, rotation, ""));
        }
//        new BoardVisualiser(board);
//        new BoardVisualiser(board, true);
        return board;
    }


    @Override
    public synchronized void localBest(String id, Strategy strategy, EBoard board, StrategySolverState state) {
        if (strategy.acceptsUnresolvable()) {
            if (board.getFilledCount() <= globalBestPhase2) {
                return;
            }
            globalBestPhase2 = board.getFilledCount();
            System.out.printf("Phase 2: %s (%dK attempts/sec): %d %s\n",
                              id, state.getTotalAttemptsPerMS(), board.getFilledCount(), board.getDisplayURL());
            return;
        }

        if (board.getFilledCount() <= globalBest) {
            return;
        }
        globalBest = board.getFilledCount();
        System.out.printf("Phase 1: %s (%dK attempts/sec): %d %s\n",
                          id, state.getTotalAttemptsPerMS(), board.getFilledCount(), board.getDisplayURL());
        if (globalBest >= 200) { // Magic number, sorry!
            activatePhase2(board.getDisplayURL());
        }
    }

    private void activatePhase2(String bucasURL) {
        EBoard board = EBoard.load(bucasURL);
        //Walker walker = new WalkerG2R(board);
        Walker walker = new WalkerGeneric(board, Comparator.comparingInt(Walker.Move::bottomRightFirst));
//        System.out.println("Activating phase 2 for marked " + board.getFilledCount());
        StrategyBase strategy = new StrategyReset(walker, this, DEFAULT_PHASE2_RESET, DEFAULT_PHASE2_TIMEOUT);
        //strategy.setAcceptsUnresolvable(true);
        strategy.setOnlySingleField(false);
        StrategySolver solver = new StrategySolver(board, strategy);
        executor.submit(solver);
    }
}
/*

 Phase 2: pool-1-thread-16 (0K attempts/sec): 213 https://e2.bucas.name/#puzzle=TokeEskildsen&board_w=16&board_h=16&board_edges=abdaafhbabtfacsbabpcafjbaaaaaaaaafqfaelfadteabmdaeibafoeacrfaadcdpcahssptgwssqugphiqjnthaaaaaaaaqjkllthjtpjtmiwpisoioqwsrpnqdaepcnfasnnnwlmnugolimhgtrvmaaaaaaaaknhshlsnjmllwuhmoujuwswunqoseacqaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaahsjusskslrlshmorjvomwlwvogllcadgaaaaaaaaaaaaaaaaaaaaaaaaaaaankoujhukkprhlsnpowmsoiqwwililiridacifubalwmuvvlwgtqvaaaaaaaarluqomnlupmmrilpngrimtlgqqwtlvmqrnkvcaenbhcamkqhlwokquiwaaaaaaaaujsinnvjmpjnllkprwvllrqwwpkrmonpkvkoeabvcocaqnqoouqniowuaaaaaaaastopvvitjqovkwhqvkvwqjskkgrjnnpgkpgnbaepckfaqmokqhwmwqlhaaaaaaaaommiijjmosujhvjsvijvsjgironjpprogtnpeaetftdaovntwppvloupaaaaaaaamttkjoqtuvgojgwvjigggqiintmqrwhtnspweaesdidanqwipqrqunrqaaaaaaaatrkgqrtrglorwollggkoigtgmpqghhrpptwheabtdweawvgwrkhvrphkaaaaaaaakrvwtkvrolnklkilkgiktmrgqgqmrtrgwvwtbacvepcagmsphjumhhwjaaaaaaaavoknvmjonrkmijurijpjrijjqooiriqowtgicadtcrbasmtrujvmwgsjaaaaaaaakuvtjtruksntuvuspnsvjminorrmqvprgsgvdadsbgbatnqgvijnstjiaaaaaaaavuworhounlvhulplsuhlisourtusplmtghhldadhbeaaqbaejbabjfabaaaaaaaawdafofadvcafpcachbacoeabueaemfaehcafdaac
 Phase 1: pool-1-thread-3 (215K attempts/sec): 211 https://e2.bucas.name/#puzzle=TokeEskildsen&board_w=16&board_h=16&board_edges=adcaadhdabmdafubadufafwdafgfackfacocacpcafhcaelfadteadidadsdaabdcqeahmkqmrwmusgruvuswppvgmspkigmaaaaaaaahhunlsnhtgwsiiwgsjgibabjeseaknhswlmngosluplopjppshvjgimhaaaaaaaaujosnmpjwuhmwswugvgsbacvepcahjqpmlljslrllkplpgnkvvlgmtrvaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaacsbaqugslkvurvmkpsuvntkslqttrqpqaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabveagouvvjqominjuksikntkthjnptwhaaaaaaaaaaaaaaaaaaaaaaaaaaaabacheqbauiwqqwoinspwssksthusjlthwvvlgwwvvlrwijjlaaaaaaaalvmqrnkvcaenbtfawvwtoknvprhkkhvruqlhtjkqvvijwvkvrtkvjistaaaaaaaamorhkqmoeafqfqfawtqqnlkthwqlvwlwlrqwkmnrijjmkgrjkoggsnqoaaaaaaaartrgmrgtfacrfoeaqwsokrvwqrtrluqrqnounsnnjprsroppglloqiklaaaaaaaariqogpnicadpepbastopvvittkuvqjskovmjntqvrwhtpkrwlwokkuhwaaaaaaaaqooinpmodaepbgbaolugiwilushwsphsmiwpqphihtvprsmtouishhruaaaaaaaaolnkmtpleabtboeaurhoijurhhwjhlghwmulhjumvomjmnloiomnrgloaaaaaaaanrquphkrbafhemfahmrmujvmwgsjgjiguhsjukjhmttklgmtmpqglsnpaaaaaaaaqvprkokvfadofjbaronjvuwosrtuilirsuhljtrutpjtmmupqntmnpgnaaaaaaaaprilkgtrdadgbeaandaeweadteaeibaehcabrbacjfabufaftdafgcadnfacvcafidactcaddaac
 Phase 1: pool-1-thread-10 (211K attempts/sec): 212 https://e2.bucas.name/#puzzle=TokeEskildsen&board_w=16&board_h=16&board_edges=abeaacrbafvcaemfabveacvbaepcabteafhbaaaaaaaaadofaftdadufadgdaabdelfarwvlvgwwmpqgvwppvvlwpsuvtgwshhlgaaaaaaaaorgltkvruhwkgimhbaeifoeavntowlmnqjklpppjloupuvgowvkvlgvvaaaaaaaagkogvulkwswumwkseadweqbattlqmqntkwhqpmiwupmmgnkpkvrnvijvaaaaaaaaoiqwliwiwtgikmttdabmbjfalijjnqwihmkqiommmnlokounrhmojnthaaaaaaaaqvntwlwvgmtltplmbaepfhcajumhwokukqmomqgqlvmquwovmsowthusaaaaaaaanhhuwmqhtrsmlslreaescrfamorrknvompjngtnpmsutoqwsovjqusuvaaaaaaaahlsnqiklsjgilthjeadtfqfartrqvvitjnnvnsnnuhlswjhhjrijuhhraaaaaaaasksskntkgwqnhptwdaepfubarijuiwginspwnplslulphmwuijjmhukjaaaaaaaasjwgtrujqwlrtqqweacqbtfajistgqiiaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaawolluisoliriqoricacofufasgruikkgaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaluqrshwurphhrhkpcabhfjbaronjklwowmulkrvmtrgrmhmruqlhnouqaaaaaaaaqrpnwpkrhjqpkgrjbabgbpcanigpwquiugsqvgsggjigmlljlkiluvtkaaaaaaaaphtvkrphqpqrrsjpbacscidagmkiujvmskqjsiukisoilgosigtgtrkgaaaaaaaatpjtpropqvprjgwvcadgdsdaknhsvijnqooiujuooqtjosnqtopskvkoaaaaaaaajkqtolnkprilwhtrdadhdcaahbacjbaboeabueaeteaencaepcackfacaaaaaaaaqeafndaeidadtcaddaac

 */