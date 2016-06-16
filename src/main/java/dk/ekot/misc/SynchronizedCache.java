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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SynchronizedCache {
    private static Log log = LogFactory.getLog(SynchronizedCache.class);

    private static final Set<String> fileCopyLockSet = Collections.synchronizedSet(new HashSet<>(1024));
    private static final Map<String, Path> cache = Collections.synchronizedMap(new HashMap<>(1024));

    public void add(Path fullSourcePath, Path fullCachePath) throws IOException {
        long start = System.nanoTime();
        synchronized (fileCopyLockSet) {
            while (fileCopyLockSet.contains(fullCachePath.toString())) {
                log.trace("Waiting for " + fullCachePath + " to be removed from lock set");
                try {
                    fileCopyLockSet.wait(1000);
                } catch (InterruptedException e) {
                    log.debug("Interrupted or timed out while waiting for changes in fileCopyLockSet, looking for " +
                              fullCachePath);
                }
                log.trace("Interrupted waiting for " + fullCachePath + " after " +
                          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms");
            }
            log.trace("Adding to lock set: " + fullCachePath);
            fileCopyLockSet.add(fullCachePath.toString());
        }

        try {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (elapsed > 250) {
                log.warn(String.format("File copy of '%s' delayed %d ms.  Possible DDOS.",
                                       fullCachePath.toString(), elapsed));
            }
            copy(fullSourcePath, fullCachePath);
        } finally {
            log.trace("Removing " + fullCachePath + " from lock set");
            // https://docs.oracle.com/javase/tutorial/essential/concurrency/guardmeth.html
            synchronized (fileCopyLockSet) {
                fileCopyLockSet.remove(fullCachePath.toString());
                fileCopyLockSet.notifyAll(); // Notify waiters
            }
        }
        Path extractedPath = fullSourcePath;
        cache.put(extractedPath.toString(), fullCachePath);
    }

    // Hack to make it easier to mock unit testing
    protected void copy(Path fullSourcePath, Path fullCachePath) throws IOException {
        Files.createDirectories(fullCachePath.getParent());
        Files.copy(fullSourcePath, fullCachePath, StandardCopyOption.REPLACE_EXISTING);
    }
}
