package dk.ekot.misc;

import org.junit.Test;

import java.awt.*;
import java.io.IOException;

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
public class FastImageDimensionsTest {

    @Test
    public void testCamera() throws IOException {
        final String CAMERA = "/home/te/projects/ponder-this/src/test/resources/20161101-2205_2_SorteretDuplo.jpg";
        final int MAJOR = 5;
        final int RUNS = 1000;
        for (int m = 0 ; m < MAJOR ; m++) {
            speedTest(CAMERA, RUNS);
        }
    }

    @Test
    public void testIco() throws IOException {
        final String CAMERA = "/home/te/projects/ponder-this/src/test/resources/jpegcrops.ico";
        final int MAJOR = 5;
        final int RUNS = 1000;
        for (int m = 0 ; m < MAJOR ; m++) {
            speedTest(CAMERA, RUNS);
        }
    }

    @Test
    public void testSvg() throws IOException {
        final String CAMERA = "/home/te/projects/ponder-this/src/test/resources/mail.svg";
        final int MAJOR = 5;
        final int RUNS = 1000;
        for (int m = 0 ; m < MAJOR ; m++) {
            speedTest(CAMERA, RUNS);
        }
    }

    private void speedTest(String image, int RUNS) throws IOException {
        Dimension dimension = new Dimension(-1, -1);

        {
            long ns = -System.nanoTime();
            for (int i = 0; i < RUNS; i++) {
                dimension = FastImageDimensions.getImageDimensions(image);
            }
            ns += System.nanoTime();
            System.out.println(String.format(
                    "Fast:  %d iterations with %s completed in %dms (%.2fms/image) with dimensions %dx%d",
                    RUNS, image, ns / 1000000, ns / 1000000.0 / RUNS, dimension.width, dimension.height));
        }

        {
            long ns = -System.nanoTime();
            for (int i = 0; i < RUNS; i++) {
                dimension = FastImageDimensions.getThirdImageDimensions(image);
            }
            ns += System.nanoTime();
            System.out.println(String.format(
                    "Third: %d iterations with %s completed in %dms (%.2fms/image) with dimensions %dx%d",
                    RUNS, image, ns / 1000000, ns / 1000000.0 / RUNS, dimension.width, dimension.height));
        }
        /*
        {
            long ns = -System.nanoTime();
            for (int i = 0; i < RUNS; i++) {
                dimension = FastImageDimensions.getSlowImageDimensions(image);
            }
            ns += System.nanoTime();
            System.out.println(String.format(
                    "Slow: %d iterations with %s completed in %dms (%.1fms/image) with dimensions %dx%d",
                    RUNS, image, ns / 1000000, ns / 1000000.0 / RUNS, dimension.width, dimension.height));
        } */
    }
}