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

import java.math.BigInteger;

/**
 * https://www.research.ibm.com/haifa/ponderthis/challenges/March2016.html
 * Take the last x digits, square them, take the last x digits of that, reverse them,
 * try X=1...9 of revXorg
 */
public class Mar2016 {

    public void simple(int digits) {
        BigInteger bi = new BigInteger("10");
        bi = bi.pow(digits-1);
        BigInteger end = bi.multiply(BigInteger.TEN);
        System.out.println("End: " + end);
        while (!bi.equals(end)) {
            String firstDigits= new StringBuilder(bi.pow(2).mod(end).toString()).reverse().toString();
            String lastDigits = bi.toString();
            for (int i = 0 ; i < 10 ; i++) {
                String candidate = firstDigits + i + lastDigits;
                BigInteger powEnd = new BigInteger(candidate).pow(2);
                String revPow = new StringBuilder(powEnd.toString()).reverse().toString();
            }
            bi = bi.add(BigInteger.ONE);
        }
    }

    public static void main(String[] args) {
        new Mar2016().simple(2);
    }
}
