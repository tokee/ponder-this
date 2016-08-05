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
 * Holds a list of unique integers from 0-n as a stack, with O(1) check for existence of a value in the stack.
 * </p><p>
 * This class is final only to ensure all possible speed-hints til the JVM. It has not been tested if this has any
 * real-world effect and if the code is to be used more generally, final could easily be a problem.
 */
public final class RandomAccessStack {
    private static Log log = LogFactory.getLog(RandomAccessStack.class);

    private final int[] stack;
    int stackPos = 0; //
    private final boolean[] bitmap;

    public RandomAccessStack(int maxValue) {
        bitmap = new boolean[maxValue+1];
        stack = new int[maxValue+1];
    }

    public int getStackPos() {
        return stackPos;
    }

    public int pop() {
        if (stackPos == 0) {
            throw new IllegalStateException("Pop called on empty stack");
        }
        bitmap[stack[stackPos]] = false;
        return stack[stackPos--];
    }

    public void pop(int amount) {
        popTo(stackPos-amount);
    }

    public void popTo(int stackPos) {
        for (int pos = stackPos+1 ; pos <= this.stackPos ; pos++) {
            bitmap[stack[pos]] = false;
        }
        this.stackPos = stackPos;
    }

    public void push(int value) {
        if (bitmap[value]) {
            throw new IllegalArgumentException("The value " + value + " is already on the stack");
        }
        bitmap[value] = true;
        stack[++stackPos] = value;
    }

    public boolean checkAndPush(int value) {
        if (bitmap[value]) {
            return false;
        }
        bitmap[value] = true;
        stack[++stackPos] = value;
        return true;
    }

    public boolean contains(int value) {
        return bitmap[value];
    }

}
