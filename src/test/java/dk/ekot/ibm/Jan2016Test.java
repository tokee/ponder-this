package dk.ekot.ibm;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Jan2016Test extends TestCase {

    /*
    From the assignment:
    So, for example, using sets of size 3, we can get to N=19 by S_1={1,3,6} and S_2={2,3,5}.
     */
    public void testGetMaxNaive() throws Exception {
        assertEquals("Assignment gave the answer", 19, Jan2016.getMaxNaive(new int[]{1,3,6}, new int[]{2,3,5}));
    }

    public void testGetMaxBitmap() throws Exception {
        assertEquals("Assignment gave the answer", 19, Jan2016.getMaxBitmap(new int[]{1, 3, 6}, new int[]{2, 3, 5}));
    }

    public void testFindMaxNaive() {
        assertEquals(21, Jan2016.findMaxNaive(3, 3, 19));
    }
    public void testHasSolutionWithTwins() {
         assertTrue(Jan2016.hasSolutionWithTwins(3, 3, 19));
     }


    public void testFindMaxNaive3_50() {
        assertEquals(21, Jan2016.findMaxNaive(3, 3, 30));
    }

    public void testFindMaxNaive6_56() {
        assertEquals(57, Jan2016.findMaxNaive(6, 6, 56));
    }

    public void testFindMaxFixed6_56_f6_7() {
        assertEquals(56, Jan2016.findMaxFixed(6, 6, 80, 6, 7));
    }

    public void testHasSolutionWithTwins6_56() {
         assertTrue(Jan2016.hasSolutionWithTwins(6, 6, 72));
     }

    public void testFindMaxTwins() {
        Jan2016.findMaxTwins(6, 6, 20);
    }

    public void testDumpPairs() {
        for (int i = 1 ; i <= 12 ; i++) {
            System.out.println(i + ": " + Jan2016.toString(Jan2016.getAllPairs(i)));
        }
    }

    public void testDumpNeededPairs() {
         for (int i = 1 ; i <= 12 ; i++) {
             System.out.println(i + ": " + Jan2016.toString(Jan2016.getNeededPairsWithGap(i)));
         }
     }

    public void testLargestSolutionWithRulesSetSize1() {
         assertEquals(3, Jan2016.largestSolutionWithRules(1));
     }
    public void testLargestSolutionWithRulesSetSize2() {
         assertEquals(12, Jan2016.largestSolutionWithRules(2));
     }
    public void testLargestSolutionWithRulesSetSize3() {
         assertEquals(23, Jan2016.largestSolutionWithRules(3));
     }
    public void testLargestSolutionWithRulesSetSize4() {
         assertEquals(33, Jan2016.largestSolutionWithRules(4));
     }
    // Fast up until 43, slower until 50, horribly slow after that
    public void testLargestSolutionWithRulesSetSize6() {
         assertEquals(23, Jan2016.largestSolutionWithRules(6));
     }

    public void testDumpSortedByLength() {
        final int maxGear = 102;
        List<Set<Jan2016.Pair>> sets = Jan2016.calculateRules(maxGear);
        Collections.sort(sets, new Comparator<Set<Jan2016.Pair>>() {
            @Override
            public int compare(Set<Jan2016.Pair> o1, Set<Jan2016.Pair> o2) {
                return o1.size() - o2.size();
            }
        });
        for (Set<Jan2016.Pair> rule: sets) {
            int max = 0 ;
            for (Jan2016.Pair pair: rule) {
                max = Math.max(max, Math.max(pair.s1, pair.s2));
            }
            System.out.println(max + ": " + Jan2016.toString(rule));
        }
    }


}