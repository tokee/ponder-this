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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *
 */
public class PerformanceCollector {
    private static final Logger log = LoggerFactory.getLogger(PerformanceCollector.class);

    private final Writer performance;
    private final Writer progress;
    private final long startTime = System.currentTimeMillis();

    public PerformanceCollector(String prefix) {
        try {
            LocalDateTime date = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm");
            String timestamp = date.format(formatter);

            FileOutputStream fos = new FileOutputStream(prefix + "_perf_" + timestamp + ".dat");
            performance = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            performance.write("# time\tplaced\ttried\n");

            FileOutputStream fos2 = new FileOutputStream(prefix + "_prog_" + timestamp + ".dat");
            progress = new OutputStreamWriter(fos2, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create progress and performance files", e);
        }
    }

    public void collect(int placed, long tried, String progress) {
        long spend = System.currentTimeMillis()-startTime;
        try {
            performance.write(String.format(Locale.ROOT, "%d\t%d\t%s\n", spend, placed, tried));
            performance.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write performance", e);
        }
        try {
            this.progress.write(String.format(Locale.ROOT, "time=%dm, placed=%d, tried=%d, att/sec=%dK %s\n",
                                              spend, placed, tried, tried/spend, progress));
            this.progress.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write progress", e);
        }
        System.out.printf(Locale.ROOT, "time=%dms, placed=%d, tried=%d, %s\n", spend, placed, tried, progress);
    }

    public void close() {
        try {
            performance.flush();
            performance.close();
            progress.flush();
            progress.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to close properly", e);
        }
    }
}
