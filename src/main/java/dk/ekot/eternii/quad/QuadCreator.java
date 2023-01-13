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

import dk.ekot.eternii.EPieces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

/**
 *
 */
public class QuadCreator {
    private static final Logger log = LoggerFactory.getLogger(QuadCreator.class);

    public static void main(String[] args) {
        System.out.println("Quads: " + createQuads().size());
    }

    public static QuadBag createQuads() {
        QuadBag qb = new QuadBag(new PieceMap());
        createInners(qb);  //   912,330
        //createEdges(qb);   //    69,531
        //createCorners(qb); //     1,291
        return qb;
    }

    public static final int[] ROT_ALL = new int[]{0, 1, 2, 3};

    public static void createCorners(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] corner = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.CORNER)
                .mapToInt(i -> i)
                .toArray();
        int[] edge = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.EDGE)
                .mapToInt(i -> i)
                .toArray();
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
        log.info("corner={}, edge={}, free={}", corner.length, edge.length, free.length);
        findPermutations(corner, ROT_ALL, (piece, rot) ->
                                 epieces.getLeft(piece, rot) == EPieces.EDGE_EDGE &&
                                 epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,

                         edge, ROT_ALL, (piece, rot) -> epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,

                         free, ROT_ALL, (piece, rot) -> true,

                         edge, ROT_ALL, (piece, rot) -> epieces.getLeft(piece, rot) == EPieces.EDGE_EDGE,

                         epieces, quadBag);
    }

    public static void createEdges(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] edge = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.EDGE)
                .mapToInt(i -> i)
                .toArray();
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
        log.info("edge={}, free={}", edge.length, free.length);
        findPermutations(edge, ROT_ALL, (piece, rot) -> epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,
                         edge, ROT_ALL, (piece, rot) -> epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,

                         epieces, quadBag);
    }

    public static void createInners(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
        log.info("free={}", free.length);
        final AtomicInteger nw = new AtomicInteger(0);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(free, ROT_ALL, (piece, rot) -> { nw.set(piece); return true; },
                         free, ROT_ALL, (piece, rot) -> piece > nw.get(),
                         free, ROT_ALL, (piece, rot) -> piece > nw.get(),
                         free, ROT_ALL, (piece, rot) -> piece > nw.get(),

                         epieces, quadBag);
    }

    /*
     * Generic permutator for quads.
     * XXs = pieces for the position, XXrs = rotations, XXp = predicate taking [piece, rotation]
     */
    public static void findPermutations(int[] nws, int[] nwrs, BiPredicate<Integer, Integer> nwp,
                                        int[] nes, int[] ners, BiPredicate<Integer, Integer> nep,
                                        int[] ses, int[] sers, BiPredicate<Integer, Integer> sep,
                                        int[] sws, int[] swrs, BiPredicate<Integer, Integer> swp,
                                        EPieces epieces, QuadBag quadBag) {
        for (final int nw : nws) {
            for (int nwr: nwrs) {
                if (!nwp.test(nw, nwr)) {
                    continue;
                }
                final int nwColRight = epieces.getRight(nw, nwr);

                for (int ne : nes) {
                    if (ne == nw) {
                        continue;
                    }
                    for (int ner: ners) {
                        if ((epieces.getLeft(ne, ner) != nwColRight) ||
                            (!nep.test(ne, ner))) {
                            continue;
                        }
                        // nw + ne ok
                        final int neColBottom = epieces.getBottom(ne, ner);

                        for (int se : ses) {
                            if (se == nw || se == ne) {
                                continue;
                            }
                            for (int ser: sers) {
                                if ((epieces.getTop(se, ser) != neColBottom) ||
                                    (!sep.test(se, ser))) {
                                    continue;
                                }

                                // nw + ne + se ok
                                final int seColLeft = epieces.getLeft(se, ser);
                                final int nwColBottom = epieces.getBottom(nw, nwr);

                                for (int sw : sws) {
                                    if (sw == nw || sw == ne || sw == se) {
                                        continue;
                                    }
                                    for (int swr: swrs) {
                                        if ((epieces.getRight(sw, swr) != seColLeft) ||
                                            (epieces.getTop(sw, swr) != nwColBottom) ||
                                            (!swp.test(sw, swr))) {
                                            continue;
                                        }
//                                                        System.out.println("nw " + epieces.toDisplayString(nw, nwr) +
//                                                                           ", ne " + epieces.toDisplayString(ne, ner) +
//                                                                           ", se " + epieces.toDisplayString(se, ser) +
//                                                                           ", sw " + epieces.toDisplayString(sw, swr) +
//                                                                           ", perm=" + (nwi+1)*(nei+1)*(sei+1)*(swi+1));
                                        // All match!
                                        quadBag.addQuad(
                                                QBits.createPiece(nw, ne, se, sw),
                                                // TODO: Provide colors
                                                QBits.createInner(0, ner, ser, swr,
                                                                  0, 0, 0, 0, 0, 0, 0, 0));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
