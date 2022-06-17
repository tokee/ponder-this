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
package dk.ekot.eternii;

import java.util.function.BiConsumer;

/**
 * Simple rectangle with the coordinate system having (0, 0) in the top left corner.
 */
public class Rect {
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public final int width;
    public final int height;
    public final int area;

    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;

        this.width = right-left+1;
        this.height = bottom-top+1;
        this.area = width*height;
    }

    /**
     * Walk the rectangle, top left to bottom right in western reading order.
     * @param consumer
     */
    public void walk(BiConsumer<Integer, Integer> consumer) {
        for (int y = top ; y <= bottom ; y++) {
            for (int x = left ; x <= right ; x++) {
                consumer.accept(x, y);
            }
        }
    }

    /**
     * @return true if {@code (x, y)} is inside of the rectangle, all rectangle edges inclusive.
     */
    public boolean isInside(int x, int y) {
        return x >= left && x <= right && y >= top && y<= bottom;
    }
}
