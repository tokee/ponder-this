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
package dk.ekot.similar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class MultiDimPoints {
    public final int points;
    public final int dimensions;
    public final double[] inner;

    private Length[] lengths;

    public MultiDimPoints(Path inputFile, int maxPoints) throws IOException {
        BufferedReader input = getReader(inputFile);
        int totalLines = 0;
        int dimensions = -1;
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            totalLines++;
            if (dimensions == -1) {
                dimensions = line.split(" ").length;
            }
            if (totalLines > maxPoints) {
                break;
            }
        }
        input.close();
        this.points = Math.min(maxPoints, totalLines);
        if (points < maxPoints) {
            System.err.println("Warning: Requested a maximum of " + maxPoints + " points, but '" +
                               inputFile.getFileName() + " only holds " + points);
        }
        this.dimensions = dimensions;
        inner = new double[points * dimensions];

        input = getReader(inputFile);
        int point = 0;
        while ((line = input.readLine()) != null && point < points) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] tsneDims = line.split(" ");
            if (tsneDims.length != dimensions) {
                throw new IllegalArgumentException(
                        "The file '" + inputFile.getFileName() + "' was expected to holds points with dimensions " +
                        dimensions + ", but a line with dimension " + tsneDims.length + " was encountered:\n" +
                        line);
            }
            for (int dim = 0; dim < dimensions; dim++) {
                set(dim, point, Double.parseDouble(tsneDims[dim]));
            }
            point++;
        }
        input.close();
    }

    public BufferedReader getReader(Path inputFile) throws IOException {
        return new BufferedReader(
                inputFile.getFileName().toString().endsWith(".gz") ?
                        new InputStreamReader(new GZIPInputStream(inputFile.toUri().toURL().openStream()), StandardCharsets.UTF_8) :
                        new InputStreamReader(inputFile.toUri().toURL().openStream(), StandardCharsets.UTF_8));
    }

    public MultiDimPoints(int dimensions, int points) {
        this.points = points;
        this.dimensions = dimensions;
        inner = new double[points * dimensions];
    }

    public final double get(final int dimension, final int point) {
        return inner[point * dimensions + dimension];
    }

    public final void set(final int dimension, final int point, final double value) {
        inner[point * dimensions + dimension] = value;
    }

    public int size() {
        return inner.length;
    }

    public Length[] getLengths() {
        double minLength = Double.MAX_VALUE;
        double maxLength = Double.MIN_VALUE;
        if (lengths == null) {
            lengths = new Length[points];
            for (int point = 0; point < points; point++) {
                double length = 0;
                for (int dim = 0; dim < dimensions; dim++) {
                    try {
                        final double d = get(dim, point);
                        length += d * d;
                    } catch (Exception e) {
                        throw new RuntimeException("point=" + point + ", dim=" + dim + " with #points=" + points + ", #dims=" + dimensions, e);
                    }
                }
                lengths[point] = new Length(point, length);
                if (length < minLength) {
                    minLength = length;
                }
                if (length > maxLength) {
                    maxLength = length;
                }
            }
            Arrays.sort(lengths);
        }
        System.out.println(String.format("Length calculation for %d points: min=%.2f, max=%.2f",
                                         points, minLength, maxLength));
        return lengths;
    }

    public void fill(NearestNeighbour.DISTRIBUTION distribution, boolean randomize) {
        switch (distribution) {
            case random: {
                Random random = new Random(87);
                for (int i = 0; i < inner.length; i++) {
                    inner[i] = random.nextDouble();
                }
                return;
            }
            case linear: {
                double value = 0;
                for (int i = 0; i < inner.length; i++) {
                    inner[i] = (value += 1);
                }
                break;
            }
            case exponential: {
                double value = Float.MAX_VALUE;
                for (int i = 0; i < inner.length; i++) {
                    inner[i] = (value /= 2);
                }
                break;
            }
            case logarithmic: {
                double value = 1;
                for (int i = 0; i < inner.length; i++) {
                    inner[i] = Math.log(value);
                    value += 1;
                }
                break;
            }
            case onlyTen: {
                Random random = new Random(87);
                for (int point = 0; point < points; point++) {
                    for (int dimension = 0; dimension < 10; dimension++) {
                        set(random.nextInt(dimensions), point, random.nextInt());
                    }
                }
                return;
            }
            case thack2: {
                Random random = new Random(87);
                int index = 0;
                for (; index < 20 && index < inner.length; index++) {
                    inner[index] = random.nextDouble() * 2 + 1;
                }
                for (; index < 50 && index < inner.length; index++) {
                    inner[index] = random.nextDouble() + 1;
                }
                for (; index < 200 && index < inner.length; index++) {
                    inner[index] = random.nextDouble() * 0.5 + 0.5;
                }
                for (; index < inner.length; index++) {
                    inner[index] = random.nextDouble() * 0.2;
                }
                break;
            }
            default:
                throw new UnsupportedOperationException(
                        "The distribution '" + distribution + "' is not supported");
        }
        if (randomize) {
            randomizeOrder();
        }
    }

    public void randomizeOrder() {
        Random random = new Random(87);
        for (int i = inner.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            double swap = inner[index];
            inner[index] = inner[i];
            inner[i] = swap;
        }
    }

    public int getPoints() {
        return points;
    }

    public int getDimensions() {
        return dimensions;
    }
}
