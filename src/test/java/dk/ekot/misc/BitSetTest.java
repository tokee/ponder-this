package dk.ekot.misc;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class BitSetTest {

    @Test
    public void testMix() {
        BitSet.Single set1 = new BitSet.Single();
        set1.set(0).set(87).set(3001);
        set1.shift(-1);
        assertFalse("There should be no bit 0", set1.get(0));
        assertFalse("There should be no bit -1", set1.get(-1));
        assertTrue("There should be a bit 86", set1.get(86));

        BitSet.Single set2 = new BitSet.Single();
        set2.set(4).set(86).set(1000);

        { // and
            BitSet merged = BitSet.and(set1, set2);
            assertFalse("There should be no bit 1000 for and",merged.get(1000));
            assertFalse("There should be no bit 3000 for and", merged.get(3000));
            assertTrue("There should be a bit 86 for and", merged.get(86));
        }

        { // or
            BitSet merged = BitSet.or(set1, set2);
            assertTrue("There should be a bit 1000 for or", merged.get(1000));
            assertTrue("There should be a bit 3000 for or", merged.get(3000));
            assertTrue("There should be a bit 86 for or", merged.get(86));
        }

        { // multi shift
            BitSet merged = BitSet.or(set1, set2);
            merged.shift(3);
            assertTrue("There should be a bit 1003 for or", merged.get(1003));
            assertTrue("There should be a bit 3003 for or", merged.get(3003));
            assertTrue("There should be a bit 89 for or", merged.get(89));
        }
    }

    @Test
    public void testSingleIterator() {
        BitSet.Single set1 = new BitSet.Single();
        {
            Iterator<Integer> it = set1.iterator();
            assertFalse("There should not be values for the empty set", it.hasNext());
        }

        set1.set(0).set(87).set(3001);
        set1.shift(-1);

        {
            Iterator<Integer> it = set1.iterator();
            assertTrue("There should be values", it.hasNext());
            assertEquals("First integer should be correct", Integer.valueOf(86), it.next());
            assertEquals("Second integer should be correct", Integer.valueOf(3000), it.next());
            assertFalse("There should be no more values after 2 values", it.hasNext());
        }
    }

    @Test
    public void testAndIterator() {
        BitSet set1 = new BitSet.Single().set(0, 87, 3001).shift(-1);
        BitSet set2 = new BitSet.Single().set(3, 85).shift(4);
        BitSet multi = BitSet.and(set1, set2);


        {
            Iterator<Integer> it = multi.iterator();
            assertTrue("There should be values", it.hasNext());
            int index = 0;
            for (int expected: new int[]{7, 86, 89, 3000}) {
                assertEquals("Value #" + ++index + " should be correct", Integer.valueOf(expected), it.next());
            }
            assertFalse("There should be no more values", it.hasNext());
        }

    }
}