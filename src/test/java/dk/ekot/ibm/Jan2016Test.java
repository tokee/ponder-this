package dk.ekot.ibm;

import junit.framework.TestCase;

public class Jan2016Test extends TestCase {

    /*
    From the assignment:
    So, for example, using sets of size 3, we can get to N=19 by S_1={1,3,6} and S_2={2,3,5}.
     */
    public void testGetMaxNaive() throws Exception {
        assertEquals("Assignment gave the answer", 19, Jan2016.getMaxNaive(new int[]{1,3,6}, new int[]{2,3,5}));
    }

    public void testFindMaxNaive() {
        assertEquals(21, Jan2016.findMaxNaive(3, 3, 19));
    }

    public void testFindMaxNaive3_50() {
        assertEquals(21, Jan2016.findMaxNaive(3, 3, 30));
    }

    public void testFindMaxNaive6_56() {
        assertEquals(56, Jan2016.findMaxNaive(6, 6, 56));
    }
}