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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspired by https://twitter.com/authorblues/status/1628131423105257474 this program takes an input file and produces
 * a larger output file, with the added feature of being harder to compress than the original file.
 *
 * De-bloating is supported.
 *
 * Supported bloats are 1 to 2^63-1, signalling the factor of bloat.
 */
public class Bloater {
    public static final Pattern BL_PATTERN = Pattern.compile("(.+)[.]bl([1-9][0-9]*)");

    public static void usage() {
        System.out.println(
                "Bloater: Ensuring more bytes for bucks since 2023\n" +
                "\n" +
                "Bloat: java Bloat.java [file] <factor>\n" +
                "  Bloats the file with the given factor (1 to 2^63-1) and stores the output as\n" +
                "  file.bl<factor>\n" +
                "\n" +
                "Unbloat: java Bloat.java [file.blX]\n" +
                "  Un-bloats the file.blX, where X signifies the previously used bloat factor\n" +
                "\n" +
                "  Created 2023-02-22 by Toke Eskildsen\n" +
                "  No copyright claimed, no responsibility taken");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            return;
        }

        File input = new File(args[0]);
        if (!Files.isReadable(input.toPath())) {
            System.out.println("Error: The file '" + input + "' cannot be read.");
            usage();
            return;
        }

        Matcher m = BL_PATTERN.matcher(input.toString());
        if (m.matches()) {
            unbloat(input, Long.parseLong(m.group(2)), new File(m.group(1)));
            return;
        }

        long factor = args.length > 1 ? Long.parseLong(args[1]) : 2;
        bloat(input, factor, new File(input + ".bl" + factor));
    }

    private static void bloat(File input, long factor, File output) throws IOException {
        if (Files.exists(output.toPath())) {
            System.err.println("Error: Output file '" + output + "' already exists");
            return;
        }

        System.out.println("Bloating '" + input + "' with factor " + factor);
        Random r = new Random(factor); // Must be deterministic!
        try (FileInputStream fis = new FileInputStream(input) ;
             BufferedInputStream bis = new BufferedInputStream(fis) ;
             
             FileOutputStream fos = new FileOutputStream(output) ;
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            int b;
            while ((b = fis.read()) != -1) {
                fos.write(r.nextInt() ^ b); // Data byte
                for (int i = 1 ; i < factor ; i++) {
                    fos.write(r.nextInt()); // Filler bytes
                }
            }
        }
        System.out.println("Finished writing '" + output + "'");
    }

    private static void unbloat(File input, long factor, File output) throws IOException {
        if (Files.exists(output.toPath())) {
            System.err.println("Error: Output file '" + output + "' already exists");
            return;
        }
        
        System.out.println("Unbloating '" + input + "' with factor " + factor);
        Random r = new Random(factor); // Must be deterministic!
        try (FileInputStream fis = new FileInputStream(input) ;
             BufferedInputStream bis = new BufferedInputStream(fis) ;

             FileOutputStream fos = new FileOutputStream(output) ;
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            int b;
            while ((b = fis.read()) != -1) {
                fos.write(r.nextInt() ^ b); // Data byte
                long skipThis = factor-1;
                while (skipThis > 0) {
                    skipThis -= fis.skip(skipThis); // Skip filler bytes
                }
                // Need to keep the deterministic pseudo randoms aligned
                for (int i = 1 ; i < factor ; i++) {
                    r.nextInt();
                }
            }
        }
        System.out.println("Finished writing '" + output + "'");
    }
}
