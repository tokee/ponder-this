package dk.ekot.eternii;

import junit.framework.TestCase;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

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
public class BaseGraphicsTest extends TestCase {

    public void testRotate() {
        try (InputStream imgIn = Thread.currentThread().getContextClassLoader().
                getResourceAsStream("eternii/i0.png")) {
            BufferedImage edgeImage = ImageIO.read(imgIn);
            BufferedImage img = new BufferedImage(edgeImage.getWidth()*4, edgeImage.getHeight()*2, BufferedImage.TYPE_INT_RGB);
            img.getGraphics().drawImage(edgeImage, 0, 0, null);
            img.getGraphics().drawImage(BaseGraphics.rotate90(edgeImage), edgeImage.getWidth(), 0, null);
            img.getGraphics().drawImage(BaseGraphics.rotate180(edgeImage), edgeImage.getWidth()*2, 0, null);
            img.getGraphics().drawImage(BaseGraphics.rotate270(edgeImage), edgeImage.getWidth()*3, 0, null);
            BaseGraphics.displayImage(img);
            Thread.sleep(10000000L);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load piece eternii/i0.png");
        }

    }
}