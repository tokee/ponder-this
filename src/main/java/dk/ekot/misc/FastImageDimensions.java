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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 *
 */
public class FastImageDimensions {
    private static Log log = LogFactory.getLog(FastImageDimensions.class);
    // https://stackoverflow.com/questions/672916/how-to-get-image-height-and-width-using-java
    public static Dimension getImageDimensions(final String path) {
        Dimension result = null;
        String suffix = getFileSuffix(path);
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            try {
                ImageInputStream stream = new FileImageInputStream(new File(path));
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return new Dimension(width, height);
            } catch (IOException e) {
                log.debug("Exception parsing " + path + ", using fallback");
            } finally {
                reader.dispose();
            }
        }
        try {
            BufferedImage bimg = ImageIO.read(new File(path));
            return new Dimension(bimg.getWidth(), bimg.getHeight());
        } catch (Exception e) {
            throw new RuntimeException("Exception fallback parsing image dimensions from " + path, e);
        }
    }
    private static String getFileSuffix(final String path) {
        String result = null;
        if (path != null) {
            result = "";
            if (path.lastIndexOf('.') != -1) {
                result = path.substring(path.lastIndexOf('.'));
                if (result.startsWith(".")) {
                    result = result.substring(1);
                }
            }
        }
        return result;
    }

    public static Dimension getSlowImageDimensions(String path) {
        try {
            BufferedImage bimg = ImageIO.read(new File(path));
            return new Dimension(bimg.getWidth(), bimg.getHeight());
        } catch (Exception e) {
            throw new RuntimeException("Exception fallback parsing image dimensions from " + path, e);
        }
    }
    //   https://stackoverflow.com/questions/35925981/is-there-any-way-in-java-to-take-image-width-and-height-without-transfer-or-down
    public static Dimension getThirdImageDimensions(String path) throws IOException {
        URL url = new File(path).toURL();
        try (InputStream stream = url.openStream()) {
            // The "useCache" setting will decide whether "input" below
            // will be disk or memory cached
            try (ImageInputStream input = ImageIO.createImageInputStream(stream)) {
                ImageReader reader = ImageIO.getImageReaders(input).next(); // TODO: Handle no reader
                try {
                    reader.setInput(input);

                    // Get dimensions of first image in the stream, without decoding pixel values
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    return new Dimension(width, height);
                } finally {
                    reader.dispose();
                }
            }
        }
    }
}
