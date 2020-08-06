package dk.ekot.misc;

import org.junit.Test;

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
public class GrowableLongArrayTest {

    @Test
    public void testTrivial() {
        GrowableLongArray la = new GrowableLongArray();
        la.set(0, 87);
        la.set(12, 88);
        assertEquals("At index 0, the value should be correct", 87, la.get(0));
        assertEquals("At index 12, the value should be correct", 88, la.get(12));
    }

    @Test
    public void testHighIndex() {
        GrowableLongArray la = new GrowableLongArray();
        final long index = Integer.MAX_VALUE*12L;
        la.set(index, 89);
        assertEquals("At index " + index + " (> Integer.MAX_VALUE), the value should be correct", 89, la.get(index));
    }
}