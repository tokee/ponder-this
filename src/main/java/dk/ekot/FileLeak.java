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
package dk.ekot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Checking code for file leaks
 */
public class FileLeak {
    private static final Logger log = LoggerFactory.getLogger(FileLeak.class);
    private static int count = 0;
    private static String attribute_language_da = "da";
    private static String attribute_language_en_US = "en_US";

    public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        final int NUM = 5; // 5*1.9GB
        final String large = "flora_danica.zip";

        LinkedList<String> records = new LinkedList<>();
        for (int i = 0 ; i < NUM ; i++) {
            records.add(large);
        }

        writeItemAsSAF("/mnt/bulk/space/", new String[0], records, "/home/te/tmp/fileleak");
        System.out.println("Ready!");
        Thread.sleep(1000000000L);
    }

    public static boolean writeItemAsSAF(String datadirectory, String[] line, LinkedList<String> records, String outputdirectory) throws IOException,
                ParserConfigurationException, TransformerException {
        log.debug("entering writeItemAsSAF method with parameters: (" + datadirectory + ", " + Arrays.toString(line) + "," + outputdirectory + ")");

        // Only do something if you have the files
        LinkedList<File> wavFiles = new LinkedList<>();
        for (String filename: records) {
            File wavFile = new File(datadirectory, filename);
            wavFiles.add(wavFile);
            log.debug("wav file = " + wavFile);
            if (!wavFile.exists()) {
                return false;
            }
        }

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item" + count);
        item_directory.mkdir();
        count++;
        log.debug("count = " + count);

        //The contents file simply enumerates, one file per line, the bitstream file names
        //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
        File contents = new File(item_directory, "contents");
        System.out.println("Creating " + contents);
        contents.createNewFile();
        FileWriter contentsFileWriter = new FileWriter(contents);

        //We also need to move or link or copy the wav files to this new directory
        //Files.copy()
        int c = 0;
        for (File file: wavFiles) {
            c++;
            Path wavfile_path = file.toPath();
            //Files.copy(wavfile_path, item_directory.toPath().resolve(wavfile_path.getFileName().toString() + c));
            contentsFileWriter.write(file.getName());
        }

        //TODO And we would like a zip file with all the wav files
        File zipFile = new File(item_directory, "all.zip");
        System.out.println("ZIPping " + wavFiles);
        zip(wavFiles, zipFile);
        contentsFileWriter.write(zipFile.getName());

        //remember to write the contents file
        contentsFileWriter.flush();
        contentsFileWriter.close();

        return true;
    }

    /**
     * zip files into destFile.
     * @param files
     * @param destFile
     * @throws IOException
     */
    public static void zip(List<File> files, File destFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(destFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        int count = 0;
        for (File f : files) {
            count++;
            System.out.println("ZIPping " + (f.getAbsolutePath() + count));
            FileInputStream fis = new FileInputStream(f.getAbsolutePath());
            System.out.println("Writing entry " + count);
            ZipEntry zipEntry = new ZipEntry(f.getName() + count);
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();
    }

}
