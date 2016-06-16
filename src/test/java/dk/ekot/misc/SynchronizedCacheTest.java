package dk.ekot.misc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
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

    @Test
    public void testHammering() throws InterruptedException {
        log.debug("Activating hammer");
        final int THREADS = 20;
        final SynchronizedCache sc = new SynchronizedCache();

        for (int t = 0 ; t < THREADS ; t++) {
            final String dest = "bar" + (t == 5 ? "5" : "0"); // To show that t==5 bypasses the queue
            new Thread(() -> {
                try {
                    sc.add(Paths.get("foo"), Paths.get(dest));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            Thread.sleep(5);
        }
        Thread.sleep(1000);
        System.out.println("El finito");
    }
}