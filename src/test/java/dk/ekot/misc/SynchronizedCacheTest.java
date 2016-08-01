package dk.ekot.misc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

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
public class SynchronizedCacheTest {
    private static Log log = LogFactory.getLog(SynchronizedCacheTest.class);

    public void testBigHammer() {

    }

    @Test
    public void testHammering() throws InterruptedException {
        final int THREADS = 20;
        final List<String> copied = new ArrayList<>();
        final SynchronizedCache sc = new SynchronizedCache() {
            @Override
            protected void copy(Path fullSourcePath, Path fullCachePath) throws IOException {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                copied.add(fullCachePath.toString());
            }
        };

        for (int t = 0 ; t < THREADS ; t++) {
            // Test-copies take ~25ms. Every fifth "copy" should this be processed without delay
            final String dest = t%5 == 0 ? "fifth" : "same";
            new Thread(() -> {
                try {
                    sc.add(Paths.get("foo"), Paths.get(dest));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        //    Thread.sleep(5);
        }
        Thread.sleep(1000); // It should take 19*20ms
        log.debug("Mock file copy order: " + copied);
        // There are 4 'fifth's and 16 'same'. As the two groups should be processed practically independently,
        // The 4 'fifth's should be in the first half of the copied list
        int fifths = 0;
        for (int i = 0 ; i < THREADS/2 ; i++) {
            if ("fifth".equals(copied.get(i))) {
                fifths++;
            }
        }
        assertEquals("There should be " + THREADS/5 + " \"copies\" of the file 'fifth' among the first " + THREADS/2 +
                     " files \"copied\"\n" + copied, THREADS/5, fifths);
    }
}