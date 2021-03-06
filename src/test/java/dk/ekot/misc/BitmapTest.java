package dk.ekot.misc;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class BitmapTest {

    @Test
    public void testSpecificSetGets() {
        Bitmap b = new Bitmap(128);
        Arrays.stream(new int[]{0, 1, 63, 64, 127}).peek(b::set).forEach(
                bit -> assertTrue("The bit " + bit + " should be set", b.get(bit)));
    }

    @Test
    public void testShiftCache() {
        Random r = new Random(87);
        Bitmap con = new Bitmap(256, true);
        for (int i = 0 ; i < 100 ; i++) {
            con.set(r.nextInt(con.size()));
        }
        Bitmap sans = con.makeCopy(false);

        Bitmap conDest = new Bitmap(con.size(), false);
        Bitmap sansDest = new Bitmap(sans.size(), false);

        con.shift(-62, conDest);
        sans.shift(-62, sansDest);
        assertTrue("Specific shifting by -62 should give equal results", conDest.equalBits(sansDest));

        for (int i = -200 ; i <= 200 ; i++) {
            con.shift(i, conDest);
            sans.shift(i, sansDest);
            assertTrue("Shifting by " + i + " should give equal results", conDest.equalBits(sansDest));
        }
    }

    @Test
    public void testNext() {
        Bitmap b = new Bitmap(128);
        Arrays.stream(new int[]{0, 1, 63, 64, 127}).forEach(b::set);
        final int[][] TESTS = new int[][] {
                {0, 0},
                {1, 1},
                {2, 63},
                {63, 63},
                {64, 64},
                {65, 127},
                {128, Integer.MAX_VALUE}
        };
        for (int[] test: TESTS) {
            assertEquals("Next for " + test[0] + " should be as expected", test[1], b.thisOrNext(test[0]));
        }
    }

    @Test
    public void testRandomSetGet() {
        Bitmap b = new Bitmap(1000);

        Random r = new Random(87);
        for (int i = 0; i < 100 ; i++) {
            if (r.nextDouble() < 0.1) {
                b.set(i);
            }
        }

        r = new Random(87);
        for (int i = 0; i < 100 ; i++) {
            if (r.nextDouble() < 0.1) {
                assertTrue("The bit at index " + i + " should be set but was not", b.get(i));
                b.set(i);
            } else {
                assertFalse("The bit at index " + i + " should not be set but was", b.get(i));
            }
        }
    }

    @Test
    public void testSpecificShiftLect() {
        final int[] bits = new int[]{0, 1, 63, 64, 127};
        Bitmap b = new Bitmap(128);

        Arrays.stream(bits).forEach(b::set);

        b.shift(-1);

        final int[] expected = new int[]{0, 62, 63, 126};
        Arrays.stream(expected).forEach(
                bit -> assertTrue("The bit " + bit + " should be set", b.get(bit)));
    }

    @Test
    public void testSpecificShiftRight() {
        final int[] bits = new int[]{0, 1, 63, 64, 127};
        Bitmap b = new Bitmap(128);

        Arrays.stream(bits).forEach(b::set);

        b.shift(1);

        final int[] expected = new int[]{1, 2, 64, 65};
        Arrays.stream(expected).forEach(
                bit -> assertTrue("The bit " + bit + " should be set", b.get(bit)));
    }


    @Test
    public void testRandomShift() {
        final Random r = new Random(87);
        final int RUNS = 1000;

        for (int run = 0 ; run < RUNS ; run++) {
            final Bitmap b = new Bitmap(r.nextInt(4*Long.BYTES*8-1)+1);
            final int[] bits = new int[r.nextInt(b.size()+1)];
            for (int i = 0 ; i < bits.length ; i++) {
                bits[i] = r.nextInt(b.size());
            }

            Arrays.stream(bits).forEach(b::set);

            final int shift = (r.nextBoolean() ? -1 : 1) * r.nextInt(b.size());
            b.shift(shift);

            Arrays.stream(bits).map(bit -> bit+shift).filter(bit -> bit >= 0 && bit < b.size()).forEach(
                    bit -> assertTrue("The bit " + bit + " should be set", b.get(bit)));
        }
    }

    public void testDump() {
        Bitmap b = new Bitmap(128);
        b.set(0);
        b.set(3);
        b.set(63);
        b.set(62);
        b.set(64);
        b.set(127);
        b.set(92);
        dump(b);
    }

    private void dump(Bitmap b) {
        for (long l: b.getBacking()) {
            System.out.println("0b" + String.format("%64s", Long.toBinaryString(l)).replace(' ', '0'));
        }
    }

}