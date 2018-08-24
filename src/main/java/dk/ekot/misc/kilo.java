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
package dk.ekot.misc;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 */
public class kilo {
    private static Log log = LogFactory.getLog(kilo.class);

    public static void main(String args[]) {
        long[] a = new long[1000];
        Arrays.fill(a, ((long)'t') << 24 + ((long)'o') << 16 + ((long)'k') << 8 + ((long)'t'));
        //AtomicInteger a = new AtomicInteger(0);
        //Stream.generate(() -> a.incrementAndGet() < 1000 ? "t" : null).
    }
}
