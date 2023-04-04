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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

public class QuadCreator {
    private static final Logger log = LoggerFactory.getLogger(QuadCreator.class);

    static final EPieces epieces = EPieces.getEternii();
    static final int[] corner;
    static final int[] edge;
    static final int[] free;
    static {
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x,y,p,r) -> bag.remove(p));
        corner = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.CORNER)
                .mapToInt(i -> i)
                .toArray();
        edge = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.EDGE)
                .mapToInt(i -> i)
                .toArray();
        free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
    }


    public static void main(String[] args) {
        System.out.println("Quads: " + createQuads().size());
    }

    public static QuadBag createQuads() {
        QuadBag qb = new QuadBag(new PieceTracker(), QuadBag.BAG_TYPE.clue_c);
        //createInners(qb);  //   912,330
        //createEdges(qb);   //    69,531
        //createCorners(qb); //     1,291
        //createClueNW(qb); //      5,414
        //createClueNE(qb); //      4,847
        //createClueSW(qb); //      4,945
        //createClueSE(qb); //      5,049
        createClueC(qb); //      4,633
        return qb;
    }

    public static final int[] ROT_ALL = new int[]{0, 1, 2, 3};

    public static QuadBag createCorner(QuadBag quadBag) {
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
//        log.info("corner={}, edge={}, free={}", corner.length, edge.length, free.length);
        findPermutations(corner, ROT_ALL, (piece, rot) ->
                                 epieces.getLeft(piece, rot) == EPieces.EDGE_EDGE &&
                                 epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,

                         edge, ROT_ALL, (piece, rot) -> epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,

                         free, ROT_ALL, (piece, rot) -> true,

                         edge, ROT_ALL, (piece, rot) -> epieces.getLeft(piece, rot) == EPieces.EDGE_EDGE,

                         epieces, quadBag);
        return quadBag;
    }

    public static QuadBag createEdges(QuadBag quadBag) {
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
//        log.info("edge={}, free={}", edge.length, free.length);
        findPermutations(edge, ROT_ALL, (piece, rot) -> epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,
                         edge, ROT_ALL, (piece, rot) -> epieces.getTop(piece, rot) == EPieces.EDGE_EDGE,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,

                         epieces, quadBag);
        return quadBag;
    }

    public static QuadBag createInners(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        final AtomicInteger nw = new AtomicInteger(0);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,

                         epieces, quadBag);
        return quadBag;
    }

    public static QuadBag createInnersNoQRot(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        final AtomicInteger nw = new AtomicInteger(0);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(free, ROT_ALL, (piece, rot) -> { nw.set(piece); return true; },
                         free, ROT_ALL, (piece, rot) -> piece > nw.get(),
                         free, ROT_ALL, (piece, rot) -> piece > nw.get(),
                         free, ROT_ALL, (piece, rot) -> piece > nw.get(),

                         epieces, quadBag);
        return quadBag;
    }

    // Must be positioned at qcoordinates (1, 1)
    public static QuadBag createClueNW(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] clue = new int[1];
        int[] clueR = new int[1];
        epieces.processEterniiClues((x, y, p, r) -> {
            if (x == 2 && y == 2) {
                clue[0] = p;
                clueR[0] = r;
            }
        });
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(clue, clueR, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,

                         epieces, quadBag);
        return quadBag;
    }

    // Must be positioned at qcoordinates (1, 6)
    public static QuadBag createClueNE(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] clue = new int[1];
        int[] clueR = new int[1];
        epieces.processEterniiClues((x, y, p, r) -> {
            if (x == 13 && y == 2) {
                clue[0] = p;
                clueR[0] = r;
            }
        });
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(free, ROT_ALL, (piece, rot) -> true,
                         clue, clueR, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,
                         free, ROT_ALL, (piece, rot) -> true,

                         epieces, quadBag);
        return quadBag;
    }

    // Must be positioned at qcoordinates (6, 6)
    public static QuadBag createClueSE(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] clue = new int[1];
        int[] clueR = new int[1];
        epieces.processEterniiClues((x, y, p, r) -> {
            if (x == 13 && y == 13) {
                clue[0] = p;
                clueR[0] = r;
            }
        });
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(
                free, ROT_ALL, (piece, rot) -> true,
                free, ROT_ALL, (piece, rot) -> true,
                clue, clueR, (piece, rot) -> true,
                free, ROT_ALL, (piece, rot) -> true,

                epieces, quadBag);
        return quadBag;
    }

    // Must be positioned at qcoordinates (1, 6)
    public static QuadBag createClueSW(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] clue = new int[1];
        int[] clueR = new int[1];
        epieces.processEterniiClues((x, y, p, r) -> {
            if (x == 2 && y == 13) {
                clue[0] = p;
                clueR[0] = r;
            }
        });
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(
                free, ROT_ALL, (piece, rot) -> true,
                free, ROT_ALL, (piece, rot) -> true,
                free, ROT_ALL, (piece, rot) -> true,
                clue, clueR, (piece, rot) -> true,

                epieces, quadBag);
        return quadBag;
    }

    // Must be positioned at qcoordinates (3, 4)
    public static QuadBag createClueC(QuadBag quadBag) {
        final EPieces epieces = EPieces.getEternii();
        Set<Integer> bag = epieces.getBag();
        epieces.processEterniiClues((x, y, p, r) -> bag.remove(p));
        int[] clue = new int[1];
        int[] clueR = new int[1];
        epieces.processEterniiClues((x, y, p, r) -> {
            if (x == 7 && y == 8) {
                clue[0] = p;
                clueR[0] = r;
            }
        });
        int[] free = bag.stream()
                .filter(piece -> epieces.getType(piece) == EPieces.INNER)
                .mapToInt(i -> i)
                .toArray();
//        log.info("free={}", free.length);
        // Do the hack with nw to ensure no quad-rotations
        findPermutations(
                free, ROT_ALL, (piece, rot) -> true,
                clue, clueR, (piece, rot) -> true,
                free, ROT_ALL, (piece, rot) -> true,
                free, ROT_ALL, (piece, rot) -> true,

                epieces, quadBag);
        return quadBag;
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
        long startTimeNS = System.nanoTime();
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
//                                                                           ", perm=" + nws.length*nes.length*ses.length*sws.length);
                                        // All match!
                                        int qpiece = QBits.createQPiece(nw, ne, se, sw);
                                        long qedges = QBits.createQEdges(nwr, ner, ser, swr, nw, ne, se, sw);
//                                        System.out.println("Storing " + QBits.toStringFull(qpiece, qedges));
                                        // TODO: Disable validation
//                                        validate(qpiece, qedges, nw, ne, se, sw, nwr, ner, ser, swr);
                                        quadBag.addQuad(qpiece, qedges);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        long spendTimeNS = System.nanoTime() - startTimeNS;
        log.info("Created {} quads for type {} in {}ms: {} quads/s",
                 quadBag.size(), quadBag.getType(), spendTimeNS/1000000, quadBag.size()*1000000000L/spendTimeNS);
    }

    private static void validate(int qpiece, long qedges, int nw, int ne, int se, int sw, int nwr, int ner, int ser, int swr) {
        if (QBits.getPieceNW(qpiece) != nw) {
            throw new IllegalStateException("NW should match. Expected " + nw + ", got " + QBits.getPieceNW(qpiece));
        }
        if (QBits.getPieceNE(qpiece) != ne) {
            throw new IllegalStateException("NE should match. Expected " + ne + ", got " + QBits.getPieceNE(qpiece));
        }
        if (QBits.getPieceSE(qpiece) != se) {
            throw new IllegalStateException("SE should match. Expected " + se + ", got " + QBits.getPieceSE(qpiece));
        }
        if (QBits.getPieceSW(qpiece) != sw) {
            throw new IllegalStateException("SW should match. Expected " + sw + ", got " + QBits.getPieceSW(qpiece));
        }

        if (QBits.getRotNW(qedges) != nwr) {
            throw new IllegalStateException("NW rot should match. Expected " + nwr + ", got " + QBits.getRotNW(qedges));
        }
        if (QBits.getRotNE(qedges) != ner) {
            throw new IllegalStateException("NE rot should match. Expected " + ner + ", got " + QBits.getRotNE(qedges));
        }
        if (QBits.getRotSE(qedges) != ser) {
            throw new IllegalStateException("SE rot should match. Expected " + ser + ", got " + QBits.getRotSE(qedges));
        }
        if (QBits.getRotSW(qedges) != swr) {
            throw new IllegalStateException("SW rot should match. Expected " + swr + ", got " + QBits.getRotSW(qedges));
        }
    }

}
