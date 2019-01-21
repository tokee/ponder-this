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

/**
 *
 */
public class MoreMisc {
    public static void main(String[] args) {
        down(0, 5);
    }

    private static void down(int low, int high) {
        if (low == high) {
            return;
        }
        int middle = (high + low) >>> 1;
        System.out.println(middle + ": [" + low + " " + high + "]");
        down(low, middle);
        if (middle < high-1) {
            down(middle+1, high);
        }
    }
}
