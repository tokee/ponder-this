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
public class SignificantBytesIntrinsic {
    private static Log log = LogFactory.getLog(SignificantBytesIntrinsic.class);

    /**
     * Write the x value as vInt at pos in arr, returning the new endPos. This requires arr to be capable of holding the
     * bytes needed to represent x. Array length checking should be performed beforehand.
     * @param x   the value to write as vInt.
     * @param arr the array holding vInt-values.
     * @param pos the position in arr where the vInt representation of x should be written.
     * @return the new end position after writing x at pos.
     */
    private static int writeInt(int x, byte[] arr, int pos) {
      int a;
      a = (x >>> (7*4));
      if (a != 0) {
        arr[pos++] = (byte)(a | 0x80);
      }
      a = (x >>> (7*3));
      if (a != 0) {
        arr[pos++] = (byte)(a | 0x80);
      }
      a = (x >>> (7*2));
      if (a != 0) {
        arr[pos++] = (byte)(a | 0x80);
      }
      a = (x >>> (7*1));
      if (a != 0) {
        arr[pos++] = (byte)(a | 0x80);
      }
      arr[pos++] = (byte)(x & 0x7f);
      return pos;
    }


    /** Taken from Solr 7:
     * Number of bytes to represent an unsigned int as a vint. */
    public static int vIntSize(int x) {
      if ((x & (0xffffffff << (7*1))) == 0 ) {
        return 1;
      }
      if ((x & (0xffffffff << (7*2))) == 0 ) {
        return 2;
      }
      if ((x & (0xffffffff << (7*3))) == 0 ) {
        return 3;
      }
      if ((x & (0xffffffff << (7*4))) == 0 ) {
        return 4;
      }
      return 5;
    }

    /* Integer.numberOfLeadingZeros is intrinsic and will be executed in a single CPU cycle on modern CPUs.
     * At least it should be, so why is it slower than the dummy implementation below?
      * */
    public static int vIntSizeIntrinsic(int x) {
        //return bits > 28 ? 5 : bits > 21 ? 4 : bits > 14 ? 3 : bits > 7 ? 2 : 1;
        return BLOCK7[Integer.numberOfLeadingZeros(x)];
        //return bits;
//        return (32-Integer.numberOfLeadingZeros(x)) / 7 + 1;

    }
    final static byte[] BLOCK7 = new byte[]{
            5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1};

    public static int dummy(int x) {
//        return 32-x;
        return BLOCK7[(32 - Integer.numberOfLeadingZeros(x))];
/*        switch (Integer.numberOfLeadingZeros(x)) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7: return 1;
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14: return 2;
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21: return 3;
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28: return 4;
            case 29:
            case 30:
            case 31:
            case 32: return 5;
        }
        return 5;
    }            */
    }
}
