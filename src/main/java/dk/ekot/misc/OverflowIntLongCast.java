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

/**
 *
 */
public class OverflowIntLongCast {
    private static Log log = LogFactory.getLog(OverflowIntLongCast.class);

    public static void main(String[] args) {
        printAsLong(Integer.MAX_VALUE);
        printAsLong(-1);
        printAsLong(Integer.MIN_VALUE);
        printAsLong(-88901);
        System.out.println(Long.toBinaryString(-4294878395L));
        System.out.println(Integer.toBinaryString((int)-4294878395L));
        System.out.println((int)-4294878395L);

        int a = Integer.MAX_VALUE;
        int b = Integer.MAX_VALUE;
        long c = a + b;
        System.out.println("c: " + c);
        long d = 45 * (long)getInt();
    }

    private static int getInt() {
        return 87;
    }

    private static void printAsLong(long l) {
        System.out.println(l);
    }
}
