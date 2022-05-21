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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 *
 */
public class BaseGraphics {
    private static final Logger log = LoggerFactory.getLogger(BaseGraphics.class);

    public static BufferedImage rotate90(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dest = new BufferedImage(h, w, src.getType());
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                dest.setRGB(y, w - x - 1, src.getRGB(x, y));
        return dest;
    }

    // TODO: Make this a real 180 degree rotation
    public static BufferedImage rotate180(BufferedImage src) {
        return rotate90(rotate90(src));
    }

    // TODO: Make this a real 270 degree rotation
    public static BufferedImage rotate270(BufferedImage src) {
        return rotate90(rotate90(rotate90(src)));
    }

    public static void displayImage(BufferedImage img) {
        ImageIcon imageIcon = new ImageIcon(img);
        JFrame jFrame;
        jFrame = new JFrame();

        jFrame.setLayout(new FlowLayout());

        jFrame.setSize(img.getWidth(), img.getHeight());
        JLabel jLabel = new JLabel();

        jLabel.setIcon(imageIcon);
        jFrame.add(jLabel);
        jFrame.setVisible(true);

        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
