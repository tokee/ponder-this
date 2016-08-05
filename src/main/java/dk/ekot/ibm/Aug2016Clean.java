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
package dk.ekot.ibm;


import dk.ekot.misc.RandomAccessStack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * https://www.research.ibm.com/haifa/ponderthis/challenges/August2016.html
 * Ponder This Challenge:
 * A king receives 10 bags of N golden coins each. Each coin should weigh exactly 10 grams, but some bags contain (only)
 * counterfeit coins that weigh exactly 9 grams each.
 * If N>=1024 then one can identify the counterfeit bags using a single measurement with an accurate weighing scale.
 * (How?)
 * Our challenge this month is to find a way to identify the counterfeit bags, when we know that there are at most
 * three such bags out of the ten, and when N=174.
 * Bonus '*' for solving it with even smaller N.
 */
public class Aug2016Clean {
    private static Log log = LogFactory.getLog(Aug2016Clean.class);

    public static void main(String[] args) {
        for (int bags = 1 ; bags < 13 ; bags++) {
            onlyValid(bags, 500, 1);
        }
    }

    public static void onlyValid(int bagCount, int atMostCoinsPerBag, int startCoins) {
        int[] bags = new int[bagCount+1];
        bags[0] = startCoins-1;
        int[] best = new int[bagCount+1];
        RandomAccessStack used = new RandomAccessStack(atMostCoinsPerBag*3);
        AtomicInteger atMost = new AtomicInteger(atMostCoinsPerBag);
        long startTime = System.nanoTime();
        boolean ok = onlyValid(1, bags, best, used, atMost);
        System.out.println(String.format("bags=%d, atMost=%d, time=%.2fms, pass=%b, result=%s",
                                         bagCount, atMostCoinsPerBag, (System.nanoTime()-startTime)/1000000.0, ok,
                                         ok ? toString(best, 1, best.length) : "N/A"));
    }

    private static boolean onlyValid(final int bag, final int[] bags, final int[] best,
                                     final RandomAccessStack used, final AtomicInteger atMost) {
        if (bag == bags.length) {
            System.out.println(toString(bags, 1, bags.length));
            System.arraycopy(bags, 0, best, 0, bags.length);
            atMost.set(Math.min(atMost.get(), bags[bags.length - 1]));
            atMost.decrementAndGet();
            return true;
        }

        boolean someFound = false;
/*        if (bag == 3 && toString(bags, 1, bags.length).startsWith("[3, 6, ")) { // [3, 6, 12, 20, 24, 25]
            System.out.println("Nearly there: " + toString(bags, 1, bag));
        }*/
        int minus = bags.length-bag;
        for (int coins = bags[bag-1]+1 ; coins <= atMost.get()-minus ; coins++) {
            if (used.contains(coins)) {
                continue;
            }
            bags[bag] = coins;
            final int usedMark = used.getStackPos();
            if (!checkAndMarkAllPermutations(bags, bag, used, atMost.get())) {
                continue;
            }
            if (onlyValid(bag+1, bags, best, used, atMost)) {
                someFound = true;
            }
            used.popTo(usedMark);
        }
        return someFound;
    }

    // Is false is returned, used will have been restored to its previous state
    // If true is returned, the used stack will have been updated
    private static boolean checkAndMarkAllPermutations(
            final int[] bags, final int bagIndex, final RandomAccessStack used, final int maxBagSize) {
        final int coins = bags[bagIndex];

        if (!used.checkAndPush(coins)) {
            return false; // no used.popTo as it has not bee updated
        }
        final int usedMark = used.getStackPos();

        for (int i2 = 1 ; i2 < bagIndex ; i2++) {
            final int c2 = bags[i2];
            if (!used.checkAndPush(coins+c2)) { // 2 bad bags: a+b=c
                used.popTo(usedMark);
                return false;
            }

            for (int i3 = 1 ; i3 < i2 ; i3++) { // 3 bad bags: a*b*c=d
                final int c3 = bags[i3];
                if (!used.checkAndPush(coins+c2+c3)) { // 2 bad bags: a+c=d
                    used.popTo(usedMark);
                    return false;
                }
            }
        }
        return true; // No collisions
    }

    private static String toString(int[] values, int start, int end) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("[");
        for (int i = start ; i < end ; i++) {
            if (i != start) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

}

/*

/usr/lib/jvm/java-8-oracle/bin/java -Didea.launcher.port=7535 -Didea.launcher.bin.path=/home/te/bin/idea-IU-143.381.42/bin -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/java-8-oracle/jre/lib/charsets.jar:/usr/lib/jvm/java-8-oracle/jre/lib/deploy.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/dnsns.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/jaccess.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/jfxrt.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/localedata.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/nashorn.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/sunec.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/zipfs.jar:/usr/lib/jvm/java-8-oracle/jre/lib/javaws.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jce.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jfr.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jfxswt.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jsse.jar:/usr/lib/jvm/java-8-oracle/jre/lib/management-agent.jar:/usr/lib/jvm/java-8-oracle/jre/lib/plugin.jar:/usr/lib/jvm/java-8-oracle/jre/lib/resources.jar:/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar:/home/te/projects/ponder-this/target/classes:/home/te/.m2/repository/com/ibm/icu/icu4j/56.1/icu4j-56.1.jar:/home/te/.m2/repository/commons-logging/commons-logging/1.1/commons-logging-1.1.jar:/home/te/.m2/repository/javax/servlet/servlet-api/2.3/servlet-api-2.3.jar:/home/te/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar:/home/te/.m2/repository/junit/junit/4.10/junit-4.10.jar:/home/te/.m2/repository/org/hamcrest/hamcrest-core/1.1/hamcrest-core-1.1.jar:/home/te/bin/idea-IU-143.381.42/lib/idea_rt.jar com.intellij.rt.execution.application.AppMain dk.ekot.ibm.Aug2016Clean

bags=1, atMost=500, time=0.41ms, pass=true, result=[1]
bags=2, atMost=500, time=0.03ms, pass=true, result=[1, 2]
bags=3, atMost=500, time=0.02ms, pass=true, result=[1, 2, 4]
bags=4, atMost=500, time=0.05ms, pass=true, result=[1, 2, 4, 8]
[1, 2, 4, 8, 15]
bags=5, atMost=500, time=0.52ms, pass=true, result=[3, 6, 11, 12, 13]
[1, 2, 4, 8, 15, 28]
bags=6, atMost=500, time=2.70ms, pass=true, result=[1, 6, 12, 21, 23, 25]
[1, 2, 4, 8, 15, 28, 52]
[1, 2, 4, 19, 26, 38, 50]
[1, 2, 4, 21, 33, 40, 47]
bags=7, atMost=500, time=65.26ms, pass=true, result=[3, 6, 12, 22, 41, 42, 43]
[1, 2, 4, 8, 15, 28, 52, 96]
[1, 2, 4, 8, 15, 44, 68, 92]
[1, 2, 4, 8, 39, 57, 70, 83]
[1, 2, 4, 34, 54, 66, 74, 81]
[1, 2, 15, 29, 34, 57, 68, 79]
[3, 6, 12, 22, 38, 73, 74, 75]
bags=8, atMost=500, time=5505.90ms, pass=true, result=[6, 12, 23, 40, 68, 70, 71, 72]
[1, 2, 4, 8, 15, 28, 52, 96, 165]
[1, 2, 4, 8, 15, 29, 65, 110, 155]
[1, 2, 4, 8, 15, 56, 80, 116, 152]
[1, 2, 4, 8, 15, 68, 97, 121, 145]
[1, 2, 4, 8, 16, 30, 60, 99, 138]
[1, 2, 4, 8, 16, 51, 76, 106, 136]
[1, 2, 4, 8, 58, 81, 94, 108, 126]
[1, 2, 16, 38, 60, 90, 115, 120, 124]
[1, 2, 21, 32, 58, 72, 112, 116, 120]
[1, 2, 24, 48, 78, 98, 108, 113, 118]
bags=9, atMost=500, time=509267.95ms, pass=true, result=[1, 2, 30, 56, 68, 91, 106, 110, 114]
[1, 2, 4, 8, 15, 28, 52, 96, 165, 278]
[1, 2, 4, 8, 15, 28, 52, 102, 171, 240]
[1, 2, 4, 8, 15, 28, 83, 128, 178, 228]
[1, 2, 4, 8, 15, 29, 70, 115, 165, 215]
[1, 2, 4, 8, 15, 29, 70, 121, 167, 213]
[1, 2, 4, 8, 15, 97, 126, 155, 179, 203]
[1, 2, 4, 8, 41, 74, 88, 144, 172, 196]
[1, 2, 4, 8, 42, 61, 80, 156, 170, 184]
[1, 2, 4, 8, 44, 63, 82, 154, 168, 182]
[1, 2, 4, 8, 73, 93, 123, 138, 153, 178]
[1, 2, 4, 8, 84, 103, 122, 146, 160, 174]

 */
