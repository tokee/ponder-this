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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Is isReadable reliable?
 */
public class IsReadableTest {

    @Test
    public void testBasic() throws URISyntaxException {
        final String RESOURCE = "log4j.xml";
        URL resource = Thread.currentThread().getContextClassLoader().getResource(RESOURCE);
        Assert.assertNotNull("Resource '" + RESOURCE + "' should exist", resource);

        Path resourcePath = Paths.get(resource.toURI());
        Assert.assertTrue("Resource '" + RESOURCE + "' should be readable", Files.isReadable(resourcePath));
    }

    @Test
    public void testPeter() {
        // "~/testprojekts/pdfbox_shrinkpdf/src/main/resources/TS_DRAlaes-A552.pdf"
        //final String filePathString = "~/testprojekts/pdfbox_shrinkpdf/src/main/resources/TS_DRAlaes-A552.pdf";
        //final String filePathString = "~/te/tmp/fnaf.pdf";
        final String filePathString = "/home/te/tmp/fnaf.pdf";
        File f = new File(filePathString);
        String extension = "pdf";
        //String extension = getFileExtension(f);
        if (!Objects.equals("pdf", new String(extension))) {
            System.out.println("file is not a pdf file");
        }
        if (Files.notExists(Paths.get(filePathString)) && (f.isFile())) {
            System.out.println("file does not exist");
        }
        boolean readable = Files.isReadable(f.toPath()) && !Files.isDirectory(f.toPath());  // Returns incorrect result for correct file and path
        System.out.println("readable: " + readable);

        if (!readable) {
            System.out.println("file is not readable ");
        }
    }

    boolean isNotPdf(String pdflink2) {
        boolean result = false;
        final String filePathString = "ServiceConfig.getResourcesDir()" + pdflink2;
        File f = new File(filePathString);
        String extension = getFileExtension(f);
        if (!Objects.equals("pdf",new String(extension))) {
            System.out.println("file is not a pdf file");
            result = false;
            // System.exit(0);
        }
        if (Files.notExists(Paths.get(filePathString)) && (f.isFile())) {
            System.out.println("file does not exist");
            // System.exit(0);
            result = false;
        }
        return result;
    }

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }

    boolean isPdf(String pdflink2) {
        final Path resource = Paths.get("ServiceConfig.getResourcesDir()" + pdflink2);

        if (!resource.toString().endsWith(".pdf")) { // toString needed as Path.endsWith matches on the full filename
            System.out.println("File is not a pdf file");
            return false;
        }

        if (!Files.exists(resource)) {
            System.out.println("File does not exist");
            return false;
        }

        if (Files.isDirectory(resource)) {
            System.out.println("Expected a file but got a folder");
            return false;
        }

        if (!Files.isReadable(resource)) {
            System.out.println("File is not readable ");
            return false;
        }

        return true;
    }

}
